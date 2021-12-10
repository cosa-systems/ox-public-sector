
package com.openexchange.mailauth.impersonate.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.powermock.api.mockito.PowerMockito.when;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.openexchange.config.MockConfigurationService;
import com.openexchange.config.MockLeanConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mail.api.AuthInfo;
import com.openexchange.mail.api.AuthType;
import com.openexchange.mailaccount.Account;
import com.openexchange.nimbusds.oauth2.sdk.http.send.HTTPSender;
import com.openexchange.nimbusds.oauth2.sdk.http.send.HttpClientProvider;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.OAuthTokens;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.test.mock.impl.SessionMock;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HTTPSender.class)
public class ImpersonationMailAuthenticatorTest {

    @Mock
    ThreadPoolService threadPool;

    @Mock
    HttpClientService httpClientService;

    @Mock
    ManagedHttpClient managedHttpClient;

    @Mock
    SessionOAuthTokenService sessionOAuthTokenService;

    @Mock
    TimerService timerService;

    @Mock
    ExecutorService executorService;

    @Mock
    ScheduledExecutorService scheduledExecutorService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(httpClientService.getHttpClient(Mockito.anyString())).thenReturn(managedHttpClient);
        when(threadPool.getExecutor()).thenReturn(executorService);
        when(timerService.getExecutor()).thenReturn(scheduledExecutorService);
        PowerMockito.mockStatic(HTTPSender.class);
    }

    private ImpersonationMailAuthenticator getService() throws OXException {
        return getService(new ConfigHelper());
    }

    @SuppressWarnings("null")
    private ImpersonationMailAuthenticator getService(ConfigHelper confHelper) throws OXException {
        LeanConfigurationService leanConfig = new MockLeanConfigurationService(MockConfigurationService.forValues(confHelper.getAllValuesAsStringAray()));
        return new ImpersonationMailAuthenticator(leanConfig, httpClientService, sessionOAuthTokenService, threadPool, timerService);
    }

    @Test
    public void test_startup() throws OXException {
        ImpersonationMailAuthenticator service = getService();
        Assert.assertThat(service, is(notNullValue()));
    }

    @Test
    public void test_acceptIsDisabledPrimary() throws OXException {
        ImpersonationMailAuthenticator service = getService();
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(false, "login");
        boolean accept = service.accept(SessionMock.builder().userId(3).contextId(1).build(), account, true);
        assertThat(accept, is(equalTo(false)));
    }

    @Test
    public void test_acceptIsDisabledSecondary() throws OXException {
        ImpersonationMailAuthenticator service = getService();
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        boolean accept = service.accept(SessionMock.builder().userId(3).contextId(1).build(), account, true);
        assertThat(accept, is(equalTo(false)));
    }

    @Test
    public void test_acceptIsEnabledPrimary() throws OXException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper.builder().setEnabled("true"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(false, "login");
        boolean accept = service.accept(SessionMock.builder().userId(3).contextId(1).build(), account, true);
        assertThat(accept, is(equalTo(false)));
    }

    @Test
    public void test_acceptIsEnabledSecondary() throws OXException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper.builder().setEnabled("true"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        boolean accept = service.accept(SessionMock.builder().userId(3).contextId(1).build(), account, true);
        assertThat(accept, is(equalTo(true)));
    }

    @Test
    public void test_getAuthInfoMissingClientId() throws OXException {
        expectedException.expectMessage("Missing or empty config: com.openexchange.mailauth.impersonate.clientId");
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
    }

    @Test
    public void test_getAuthInfoMissingClientSecret() throws OXException {
        expectedException.expectMessage("Missing or empty config: com.openexchange.mailauth.impersonate.clientSecret");
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
    }

    @Test
    public void test_getAuthInfoMissingTokenEndpoint() throws OXException {
        expectedException.expectMessage("Missing or empty config: com.openexchange.mailauth.impersonate.tokenEndpoint");
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
    }

    @Test
    public void test_getAuthInfoMissingAccessTokenInSession() throws OXException {
        expectedException.expectMessage("Not allowed to do impersonation auth: 3, 1, missing access_token in session");
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
    }

    @Test
    public void test_getAuthInfoMissingAdminUsernameEnabledAdmin() throws OXException {
        expectedException.expectMessage("Missing or empty config: com.openexchange.mailauth.impersonate.admin.username");
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token")
            .setAdminEnabled("true"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
    }

    @Test
    public void test_getAuthInfoMissingAdminPasswordEnabledAdmin() throws OXException {
        expectedException.expectMessage("Missing or empty config: com.openexchange.mailauth.impersonate.admin.password");
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token")
            .setAdminEnabled("true")
            .setAdminUsername("adminUser"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
    }

    @Test
    public void test_getAuthInfoUseProvidedLogin() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" +
                "    \"scope\"        : \"read write\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
        assertThat(authInfo, is(notNullValue()));
        assertThat(authInfo.getLogin(), is(equalTo("login")));
        assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
        assertThat(authInfo.getPassword(), is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E")));
    }

    @Test
    public void test_getAuthInfoUseProvidedTransportLogin() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" +
                "    \"scope\"        : \"read write\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, false);
        assertThat(authInfo, is(notNullValue()));
        assertThat(authInfo.getLogin(), is(equalTo("transportLogin")));
        assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
        assertThat(authInfo.getPassword(), is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E")));
    }

    @Test
    public void test_getAuthInfoAccessTokenLoginClaim() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token")
            .setAccessTokenLoginClaim("loginIdent"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" +
                "    \"scope\"        : \"read write\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
        assertThat(authInfo, is(notNullValue()));
        assertThat(authInfo.getLogin(), is(equalTo("loginIdent")));
        assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
        assertThat(authInfo.getPassword(), is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E")));
    }

    @Test
    public void test_getAuthInfoCallRefresh() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 59,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"),
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("login")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E")));
        }
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("login")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E")));
        }

    }

    @Test
    public void test_getAuthInfoAdminEnabled() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token")
            .setAdminEnabled("true")
            .setAdminUsername("adminUser")
            .setAdminPassword("adminPassword"));
        Assert.assertThat(service, is(notNullValue()));
        Account account = mockMailAccount(true, "login");
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"),
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), account, true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("login")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E")));
        }
    }

    @Test
    public void test_getAuthInfoAdminEnabledMultipleUsers() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token")
            .setAdminEnabled("true")
            .setAdminUsername("adminUser")
            .setAdminPassword("adminPassword"));
        Assert.assertThat(service, is(notNullValue()));
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"),
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjEifQ.YElQwz27G0a0-Kb0A_yeP9yYafPFq6NCAA_i09x0l9A\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"),
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjIifQ.GCTJxmdu54oxYZw9hGmnKF4tXmk0YGnqc8nLd3bO2NE\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), mockMailAccount(true, "login"), true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("login")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjEifQ.YElQwz27G0a0-Kb0A_yeP9yYafPFq6NCAA_i09x0l9A")));
        }
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(4).contextId(1).build(), mockMailAccount(true, "login2"), true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("login2")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjIifQ.GCTJxmdu54oxYZw9hGmnKF4tXmk0YGnqc8nLd3bO2NE")));
        }
    }

    @Test
    public void test_getAuthInfoAdminEnabledMultipleUsersWithLoginFromAccessToken() throws OXException, IOException, ParseException {
        ImpersonationMailAuthenticator service = getService(ConfigHelper
            .builder()
            .setEnabled("true")
            .setClientId("clientId")
            .setClientSecret("clientSecret")
            .setTokenEndpoint("http://example.com/token")
            .setAdminEnabled("true")
            .setAdminUsername("adminUser")
            .setAdminPassword("adminPassword")
            .setAccessTokenLoginClaim("loginIdent"));
        Assert.assertThat(service, is(notNullValue()));
        when(sessionOAuthTokenService.getFromSession(Mockito.any(Session.class))).thenReturn(Optional.of(new OAuthTokens("access_token", new Date(), "refresh_token")));
        HTTPResponse httpResponse = Mockito.mock(HTTPResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContentAsJSONObject()).thenReturn(
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoibG9naW5JZGVudCJ9.aFIrWEPs4lt2PVV-VqHjcdOlHcl8jn6ytKlvQcIz88E\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"),
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjEifQ.YElQwz27G0a0-Kb0A_yeP9yYafPFq6NCAA_i09x0l9A\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"),
            JSONObjectUtils.parse("{\n" +
                "    \"access_token\" : \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjIifQ.GCTJxmdu54oxYZw9hGmnKF4tXmk0YGnqc8nLd3bO2NE\",\n" +
                "    \"token_type\"   : \"bearer\",\n" +
                "    \"expires_in\"   : 3600,\n" + // force direct expiry
                "    \"scope\"        : \"read write\",\n" +
                "    \"refresh_token\": \"refresh_token_value\"\n" +
                "  }"));
        when(HTTPSender.send(Mockito.any(HTTPRequest.class), Mockito.any(HttpClientProvider.class))).thenReturn(httpResponse);
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(3).contextId(1).build(), mockMailAccount(true, "login"), true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("user1")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjEifQ.YElQwz27G0a0-Kb0A_yeP9yYafPFq6NCAA_i09x0l9A")));
        }
        {
            AuthInfo authInfo = service.getAuthInfo("ignoredLogin", SessionMock.builder().userId(4).contextId(1).build(), mockMailAccount(true, "login2"), true);
            assertThat(authInfo, is(notNullValue()));
            assertThat(authInfo.getLogin(), is(equalTo("user2")));
            assertThat(authInfo.getAuthType(), is(equalTo(AuthType.OAUTHBEARER)));
            assertThat(authInfo.getPassword(),
                is(equalTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJsb2dpbklkZW50IjoidXNlcjIifQ.GCTJxmdu54oxYZw9hGmnKF4tXmk0YGnqc8nLd3bO2NE")));
        }
    }

    private Account mockMailAccount(boolean secondary, String login) {
        Account account = Mockito.mock(Account.class);
        when(account.isSecondaryAccount()).thenReturn(secondary);
        when(account.getLogin()).thenReturn(login);
        when(account.getTransportLogin()).thenReturn("transportLogin");
        when(account.getPrimaryAddress()).thenReturn("test@example.com");
        return account;
    }

}
