/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.mailauth.impersonate.impl;

import static com.openexchange.mailauth.impersonate.helper.N.defaultProp;
import static com.openexchange.mailauth.impersonate.helper.N.f;
import static com.openexchange.mailauth.impersonate.helper.N.notNull;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.nimbusds.oauth2.sdk.AbstractOptionallyIdentifiedRequest;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.openexchange.annotation.NonNull;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailAuthenticator;
import com.openexchange.mail.api.AuthInfo;
import com.openexchange.mail.api.AuthType;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailauth.impersonate.exception.ImpersonationException;
import com.openexchange.mailauth.impersonate.exception.ImpersonationExceptionCodes;
import com.openexchange.mailauth.impersonate.helper.CustomJWTParser;
import com.openexchange.mailauth.impersonate.helper.N;
import com.openexchange.nimbusds.oauth2.sdk.http.send.HTTPSender;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.OAuthTokens;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.tools.strings.TimeSpanParser;

@NonNullByDefault
public class ImpersonationMailAuthenticator implements MailAuthenticator, ForcedReloadable {

    private static final Logger LOG = N.logger(ImpersonationMailAuthenticator.class);

    private final LeanConfigurationService            lean;
    private final HttpClientService                   httpClientService;
    private final SessionOAuthTokenService            sessionOAuthTokenService;
    private final LoadingCache<UserKey, OAuthTokens>  cache;
    private final LoadingCache<AdminKey, OAuthTokens> adminAccessTokenCache;

    private final AtomicLong refreshTtl = new AtomicLong(TimeUnit.HOURS.toMillis(1));

    public ImpersonationMailAuthenticator(
        LeanConfigurationService leanConfigurationService,
            HttpClientService httpClientService,
            SessionOAuthTokenService sessionOAuthTokenService,
            ThreadPoolService threadPool,
            TimerService timerService) throws OXException {
        super();
        this.lean = leanConfigurationService;
        this.httpClientService = httpClientService;
        this.sessionOAuthTokenService = sessionOAuthTokenService;
        this.cache = createUserCache(notNull(threadPool.getExecutor()), notNull(timerService.getExecutor()));
        this.adminAccessTokenCache = createAdminTokenCache(notNull(threadPool.getExecutor()), notNull(timerService.getExecutor()));
        reloadRefreshTtl();
    }

    @Override
    public boolean accept(@Nullable Session session, @Nullable Account account, boolean forMailAccess) throws OXException {
        {
            session = notNull(session);
            account = notNull(account);
        }
        // only accept secondary accounts
        if (!account.isSecondaryAccount()) {
            return false;
        }
        return lean.getBooleanProperty(session.getUserId(), session.getContextId(), defaultProp("com.openexchange.mailauth.impersonate.enabled", false));
    }

    @Override
    public String getLogin(@Nullable Session session, @Nullable Account account, boolean forMailAccess) throws OXException {
        return notNull(getAuthInfo(null, session, account, forMailAccess).getLogin());
    }

    @Override
    public AuthInfo getAuthInfo(@Nullable String ignoredLogin, @Nullable Session session, @Nullable Account account, boolean forMailAccess) throws OXException {
        {
            session = notNull(session);
            account = notNull(account);
        }
        return getAuthInfo0(session, account, forMailAccess);
    }

    protected AuthInfo getAuthInfo0(Session session, Account account, boolean forMailAccess) throws OXException {
        String configuredlogin = notNull(forMailAccess ? account.getLogin() : account.getTransportLogin());

        try {
            UserKey key = new UserKey(session, configuredlogin, notNull(account.getPrimaryAddress()));
            OAuthTokens oAuthTokens = getTokenFromCache(cache, key, (oldValue) -> getUserToken(key, oldValue));
            return geteAuthInfo1(session, account, configuredlogin, oAuthTokens);
        } catch (ImpersonationException e) {
            LOG.error("error while fetching authInfo account {}: \"{}\"", account.getPrimaryAddress(), e.getMessage(), e);
            throw ImpersonationExceptionCodes.UNEXPECTED_ERROR.create(e.getMessage(), e);
        }
    }

    protected AuthInfo geteAuthInfo1(Session session, Account account, String configuredlogin, OAuthTokens tokens) throws ImpersonationException {
        String accessToken = notNull(tokens.getAccessToken());
        String loginInfo = getLoginInfo(configuredlogin, session, accessToken);
        AuthType authType = getAuthType(session);
        LOG.trace("returning AuthInfo for PrimaryMail: '{}', Login: '{}', authType: '{}'", account.getPrimaryAddress(), loginInfo, authType);
        return new AuthInfo(loginInfo, accessToken, authType, -1);
    }

    protected String getLoginInfo(String configuredlogin, Session session, String accessToken) throws ImpersonationException {
        String loginClaim = prop(session, "com.openexchange.mailauth.impersonate.accessTokenLoginClaim", "");
        if (null == loginClaim || Strings.isEmpty(loginClaim)) {
            return configuredlogin;
        }
        Map<String, Object> body = CustomJWTParser.parseBody(accessToken);
        Object optInfo = body.get(loginClaim);
        if (null != optInfo) {
            return notNull(optInfo.toString());
        }
        LOG.info("login identifier '{}' not found in accessToken, using configured login '{}' instead", loginClaim, configuredlogin);
        return configuredlogin;
    }

    protected AuthType getAuthType(Session session) throws ImpersonationException {
        String typeStr = requiredNonEmpty(session, "com.openexchange.mailauth.impersonate.authType", "oauthbearer");
        AuthType authType = AuthType.parse(typeStr);
        if (null == authType) {
            LOG.warn("com.openexchange.mailauth.impersonate.authType misconfigured to {}, using OAUTHBEARER instead", typeStr);
            authType = AuthType.OAUTHBEARER;
        }
        return authType;
    }

    protected String getAdminAccessToken(Session session) throws ImpersonationException {

        String adminUsername = requiredNonEmpty(session, "com.openexchange.mailauth.impersonate.admin.username", "");
        Secret adminPassword = new Secret(requiredNonEmpty(session, "com.openexchange.mailauth.impersonate.admin.password", ""));
        URI tokenEndpoint = getTokenEndpoint(session);
        URI tokenLogoutEndpoint = getTokenLogoutEndpoint(session);

        AdminKey key = new AdminKey(tokenEndpoint, tokenLogoutEndpoint, adminUsername, adminPassword, getClientId(session), getClientSecret(session));
        OAuthTokens oAuthTokens = getTokenFromCache(adminAccessTokenCache, key, (oldValue) -> getAdminToken(key, oldValue));
        return notNull(oAuthTokens.getAccessToken());
    }

    protected OAuthTokens getTokenWithGrant(ClientAuthentication clientAuthentication, URI tokenEndpoint, AuthorizationGrant AuthorizationGrant) throws ImpersonationException {
        TokenRequest request = new TokenRequest(
            tokenEndpoint,
            clientAuthentication,
            AuthorizationGrant,
            new Scope());

        TokenResponse response = executeRequest(request, TokenResponse::parse);

        AccessTokenResponse accessTokenResponse = validateResponse(request, response);
        return convertNimbusTokens(notNull(accessTokenResponse.getTokens()));
    }

    protected HTTPResponse executeRequest(AbstractOptionallyIdentifiedRequest request) throws ImpersonationException {
        return executeRequest(request, ParserThrowingFunction.identity());
    }

    protected <V> V executeRequest(AbstractOptionallyIdentifiedRequest request, ParserThrowingFunction<HTTPResponse, V> function) throws ImpersonationException {
        V response;
        try {
            response = function.apply(HTTPSender.send(request.toHTTPRequest(), () -> httpClientService.getHttpClient("mail-auth-impersonate")));
        } catch (com.nimbusds.oauth2.sdk.ParseException | IOException e) {
            throw new ImpersonationException(f("An error occurred: %s", e.getMessage(), e));
        }
        return notNull(response);
    }

    private AuthorizationGrant getAuthorizationGrant(String login, Session session) throws ImpersonationException {
        boolean adminAuthEnabled = lean.getBooleanProperty(session.getUserId(), session.getContextId(), defaultProp("com.openexchange.mailauth.impersonate.admin.enabled", false));
        if (adminAuthEnabled) {
            return new TokenExchangeGrant(getAdminAccessToken(session), login, getTokenType(session));
        } else {
            Optional<OAuthTokens> optToken = sessionOAuthTokenService.getFromSession(session);
            if (!optToken.isPresent()) {
                throw new ImpersonationException(f("Not allowed to do impersonation auth: %d, %d, missing access_token in session", session.getUserId(), session.getContextId()));
            }
            return new TokenExchangeGrant(optToken.get().getAccessToken(), login, getTokenType(session));
        }
    }

    protected @Nullable TokenType getTokenType(Session session) {
        return TokenType.parseToken(prop(session, "com.openexchange.mailauth.impersonate.tokenType", "refresh_token"));
    }

    private ClientAuthentication getClientAuthentication(Session session) throws ImpersonationException {
        return new ClientSecretPost(getClientId(session), getClientSecret(session));
    }

    protected Secret getClientSecret(Session session) throws ImpersonationException {
        return new Secret(requiredNonEmpty(session, "com.openexchange.mailauth.impersonate.clientSecret", ""));
    }

    protected ClientID getClientId(Session session) throws ImpersonationException {
        return new ClientID(requiredNonEmpty(session, "com.openexchange.mailauth.impersonate.clientId", ""));
    }

    private ClientAuthentication getClientAuthentication(AdminKey key) {
        return new ClientSecretPost(key.getClientId(), key.getClientSecret());
    }

    protected String requiredNonEmpty(Session session, String key, String defaultValue) throws ImpersonationException {
        return requiredNonEmpty(session, defaultProp(key, defaultValue));
    }

    protected String requiredNonEmpty(Session session, DefaultProperty property) throws ImpersonationException {
        String value = prop(session, property);
        if (null == value || Strings.isEmpty(value)) {
            throw new ImpersonationException(f("Missing or empty config: %s", property.getFQPropertyName()));
        }
        return value;
    }

    protected @Nullable String prop(Session session, String key, String defaultValue) {
        return lean.getProperty(session.getUserId(), session.getContextId(), defaultProp(key, defaultValue));
    }

    protected @Nullable String prop(Session session, DefaultProperty property) {
        return lean.getProperty(session.getUserId(), session.getContextId(), property);
    }

    private URI getTokenEndpoint(Session session) throws ImpersonationException {
        return notNull(URI.create(requiredNonEmpty(session, "com.openexchange.mailauth.impersonate.tokenEndpoint", "")));
    }

    private @Nullable URI getTokenLogoutEndpoint(Session session) throws ImpersonationException {
        String logoutValue = prop(session, "com.openexchange.mailauth.impersonate.tokenLogoutEndpoint", "");
        if (null == logoutValue || Strings.isEmpty(logoutValue)) {
            return null;
        }
        return notNull(URI.create(logoutValue));
    }

    private AccessTokenResponse validateResponse(TokenRequest request, TokenResponse response) throws ImpersonationException {
        AuthorizationGrant grant = request.getAuthorizationGrant();
        if (!response.indicatesSuccess()) {
            TokenErrorResponse errorResponse = (TokenErrorResponse) response;
            LOG.debug("Got token error response to grant request '{}'", grant.getType().getValue());

            ErrorObject error = errorResponse.getErrorObject();
            if (OAuth2Error.INVALID_GRANT.equals(error)) {
                throw new ImpersonationException("Not allowed to do impersonation auth");
            }
            throw new ImpersonationException(f("An error occurred: %s", error.getCode() + " - " + error.getDescription()));
        }

        AccessTokenResponse tokenResponse = (AccessTokenResponse) response;
        LOG.debug("Got success token response to grant request '{}'", grant.getType().getValue());

        return tokenResponse;
    }

    public static OAuthTokens convertNimbusTokens(Tokens tokens) {
        AccessToken accessToken = tokens.getAccessToken();
        long lifetime = accessToken.getLifetime();
        Date expiryDate = null;
        if (lifetime > 0) {
            long expiryDateMillis = System.currentTimeMillis();
            expiryDateMillis += lifetime * 1000;
            expiryDate = new Date(expiryDateMillis);
        }

        RefreshToken refreshToken = tokens.getRefreshToken();
        String refreshTokenValue = null;
        if (refreshToken != null) {
            refreshTokenValue = refreshToken.getValue();
        }

        return new OAuthTokens(tokens.getAccessToken().getValue(), expiryDate, refreshTokenValue);
    }

    // Caches

    private LoadingCache<UserKey, OAuthTokens> createUserCache(ExecutorService exec, Executor scheduler) {
        return notNull(Caffeine
            .newBuilder()
            .expireAfter(new ExpiryImplementation<UserKey>())
            .scheduler(scheduler instanceof ScheduledExecutorService ? Scheduler.forScheduledExecutorService((ScheduledExecutorService) scheduler) : Scheduler.disabledScheduler())
            .removalListener((@Nullable UserKey key, @Nullable OAuthTokens value, @NonNull RemovalCause cause) -> {
                handleLogout(key, value, cause, (k) -> getClientAuthentication(k.getSession()), (k) -> getTokenLogoutEndpoint(k.getSession()));
            })
            .executor(exec)
            .build(new CacheLoader<UserKey, OAuthTokens>() {

                @Override
                public @Nullable OAuthTokens load(@SuppressWarnings("null") @NonNull UserKey key) throws Exception {
                    return getUserToken(key);
                }

                @Override
                public @Nullable OAuthTokens reload(@SuppressWarnings("null") @NonNull UserKey key, @SuppressWarnings("null") @NonNull OAuthTokens oldValue) throws Exception {
                    // only refresh tokens that between 1 and 2 minutes away from timeout
                    if (!oldValue.accessExpiresWithin(2, TimeUnit.MINUTES)) {
                        LOG.trace("token for '{}' is still valid", key);
                        return oldValue;
                    }
                    // tokens that are below 1 minute will be refreshed in main thread
                    if (oldValue.accessExpiresWithin(1, TimeUnit.MINUTES)) {
                        LOG.trace("token for '{}' will be refreshed outside of cache", key);
                        return oldValue;
                    }
                    // refresh the token via refresh_token
                    if (oldValue.hasRefreshToken()) {
                        return getUserToken(key, oldValue);
                    }
                    // return null and remove token if it has no refresh token, this will never be called for if only access_tokens are
                    // present, as the Expiry handler already removed the entry due to expiresWithin - 2 minutes
                    return null;
                }
            }));
    }

    private OAuthTokens getUserToken(UserKey key) throws ImpersonationException {
        LOG.debug("requesting new access_token for user '{}'", key);
        return getTokenWithGrant(getClientAuthentication(key.getSession()), getTokenEndpoint(key.getSession()), getAuthorizationGrant(key.getLoginName(), key.getSession()));
    }

    private OAuthTokens getUserToken(UserKey key, OAuthTokens oldToken) throws ImpersonationException {
        LOG.debug("requesting new access_token for user '{}' with existing refresh_token", key);
        return getTokenWithGrant(getClientAuthentication(key.getSession()), getTokenEndpoint(key.getSession()), toRefreshGrantToken(oldToken));
    }

    private LoadingCache<AdminKey, OAuthTokens> createAdminTokenCache(ExecutorService exec, Executor scheduler) {
        return notNull(Caffeine
            .newBuilder()
            .expireAfter(new ExpiryImplementation<AdminKey>())
            .scheduler(scheduler instanceof ScheduledExecutorService ? Scheduler.forScheduledExecutorService((ScheduledExecutorService) scheduler) : Scheduler.disabledScheduler())
            .executor(exec)
            .removalListener((@Nullable AdminKey key, @Nullable OAuthTokens value, @NonNull RemovalCause cause) -> {
                handleLogout(key, value, cause, (k) -> getClientAuthentication(k), (k) -> k.getTokenLogoutEndpoint());
            })
            .build(new CacheLoader<AdminKey, OAuthTokens>() {

                @Override
                public @Nullable OAuthTokens load(@SuppressWarnings("null") @NonNull AdminKey key) throws Exception {
                    return getAdminToken(key);
                }

                @Override
                public @Nullable OAuthTokens reload(@SuppressWarnings("null") @NonNull AdminKey key, @SuppressWarnings("null") @NonNull OAuthTokens oldValue) throws Exception {
                    // only refresh tokens that between 1 and 2 minutes away from timeout
                    if (!oldValue.accessExpiresWithin(2, TimeUnit.MINUTES)) {
                        LOG.trace("token for '{}' is still valid", key);
                        return oldValue;
                    }
                    // tokens that are below 1 minute will be refreshed in main thread
                    if (oldValue.accessExpiresWithin(1, TimeUnit.MINUTES)) {
                        LOG.trace("token for '{}' will be refreshed outside of cache", key);
                        return oldValue;
                    }
                    // refresh the token via refresh_token
                    if (oldValue.hasRefreshToken()) {
                        return getAdminToken(key, oldValue);
                    }
                    // return null and remove token if it has no refresh token, this will never be called for if only access_tokens are
                    // present, as the Expiry handler already removed the entry due to expiresWithin - 2 minutes
                    return null;
                }

            }));
    }

    private <K> OAuthTokens getTokenFromCache(LoadingCache<K, OAuthTokens> cache, K key, ImpersonationExceptionThrowingFunction<OAuthTokens, OAuthTokens> func) throws ImpersonationException {
        try {
            OAuthTokens oAuthTokens = notNull(cache.get(key));
            if (!oAuthTokens.accessExpiresWithin(1, TimeUnit.MINUTES)) {
                LOG.trace("token for '{}' is still valid", key);
                // let cache refresh
                cache.refresh(key);
                return oAuthTokens;
            }
            if (oAuthTokens.hasRefreshToken()) {
                // refresh here and put into the refresh_token cache
                try {
                    LOG.debug("requesting new access_token for '{}' with refresh_token", key);
                    oAuthTokens = notNull(func.apply(oAuthTokens));
                    LOG.trace("new access_token for '{}' successful", key);
                    cache.put(key, oAuthTokens);
                } catch (Exception e) {
                    LOG.error("could not refresh admin token for '{}', getting a new token", key);
                    oAuthTokens = invalidateKeyAndGetNewValue(cache, key);
                }
            } else {
                LOG.debug("requesting new refresh_token for '{}'", key);
                // invalidate manually and get a new token from the refreshToken cache
                oAuthTokens = invalidateKeyAndGetNewValue(cache, key);
            }
            return oAuthTokens;
        } catch (CompletionException e) {
            throw new ImpersonationException(f("An error occurred: %s", e.getCause().getMessage(), e.getCause()));
        }
    }

    protected <K, V> V invalidateKeyAndGetNewValue(LoadingCache<K, V> cache, K key) {
        cache.invalidate(key);
        return notNull(cache.get(key));
    }

    protected RefreshTokenGrant toRefreshGrantToken(OAuthTokens oAuthTokens) {
        return new RefreshTokenGrant(toRefreshToken(oAuthTokens));
    }

    protected RefreshToken toRefreshToken(OAuthTokens oAuthTokens) {
        return new RefreshToken(oAuthTokens.getRefreshToken());
    }

    private OAuthTokens getAdminToken(AdminKey key) throws ImpersonationException {
        LOG.debug("getting new fresh token for '{}'", key);
        return getTokenWithGrant(getClientAuthentication(key), key.getTokenEndpoint(), new ResourceOwnerPasswordCredentialsGrant(key.getAdminUsername(), key.getAdminPassword()));
    }

    protected OAuthTokens getAdminToken(AdminKey key, OAuthTokens oldValue) throws ImpersonationException {
        LOG.debug("getting token for '{}' with refresh_token", key);
        return getTokenWithGrant(getClientAuthentication(key), key.getTokenEndpoint(), toRefreshGrantToken(oldValue));
    }

    private final class ExpiryImplementation<K> implements Expiry<K, OAuthTokens> {

        @Override
        public long expireAfterCreate(@Nullable K key, @Nullable OAuthTokens resp, long currentTime) {
            if (null != resp) {
                if (resp.hasRefreshToken()) {
                    return TimeUnit.MILLISECONDS.toNanos(refreshTtl.get());
                }
                return TimeUnit.MILLISECONDS.toNanos(resp.getExpiresInMillis()) - TimeUnit.MINUTES.toNanos(2); // in the future minus 1 minute
            }
            // Store unsuccessful responses for 5 seconds
            return TimeUnit.SECONDS.toNanos(5);
        }

        @Override
        public long expireAfterUpdate(@Nullable K key, @Nullable OAuthTokens resp, long currentTime, long currentDuration) {
            if (null != resp) {
                if (resp.hasRefreshToken()) {
                    return TimeUnit.MILLISECONDS.toNanos(refreshTtl.get());
                }
                return TimeUnit.MILLISECONDS.toNanos(resp.getExpiresInMillis()) - TimeUnit.MINUTES.toNanos(2); // in the future minus 1 minute
            }
            return currentDuration;
        }

        @Override
        public long expireAfterRead(@Nullable K key, @Nullable OAuthTokens resp, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }

    protected <K> void handleLogout(@Nullable K key, @Nullable OAuthTokens value, RemovalCause cause, ImpersonationExceptionThrowingFunction<K, ClientAuthentication> clientAuthProvider, ImpersonationExceptionThrowingFunction<K, URI> tokenLogoutEndpointProvider) {
        if (cause == RemovalCause.REPLACED) {
            // do not log out at replace
            return;
        }

        if (null == key || null == value) {
            return;
        }
        if (!value.hasRefreshToken()) {
            return;
        }
        try {
            URI tokenLogoutEndpoint = tokenLogoutEndpointProvider.apply(key);
            if (null == tokenLogoutEndpoint) {
                return;
            }
            TokenLogoutRequest request = new TokenLogoutRequest(tokenLogoutEndpoint, clientAuthProvider.apply(key), toRefreshToken(value));
            HTTPResponse httpResponse = executeRequest(request);
            if (httpResponse.getStatusCode() == 204) {
                LOG.debug("logout success");
            } else {
                LOG.info("error at logout, response code {}", httpResponse.getStatusCode());
            }
        } catch (ImpersonationException e) {
            LOG.info("could not log out token", e);
            return;
        }
    }

    @Override
    public void reloadConfiguration(@Nullable ConfigurationService configService) {
        reloadRefreshTtl();
    }

    protected void reloadRefreshTtl() {
        DefaultProperty property = defaultProp("com.openexchange.mailauth.impersonate.refreshTtl", "1h");
        String value = lean.getProperty(property);
        long ttl;
        try {
            ttl = TimeSpanParser.parseTimespanToPrimitive(value);
        } catch (IllegalArgumentException e) {
            LOG.warn("{} configuration error for user {} in context {}", property.getFQPropertyName(), e);
            ttl = TimeSpanParser.parseTimespanToPrimitive(property.getDefaultValue(String.class));
        }
        refreshTtl.set(ttl);
    }
}
