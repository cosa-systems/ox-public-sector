
package com.openexchange.matrixproxy.servlet.impl;

import static com.openexchange.matrixproxy.servlet.helper.N.f;
import static com.openexchange.matrixproxy.servlet.helper.N.logger;
import static com.openexchange.matrixproxy.servlet.helper.N.notNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.openexchange.ajax.SessionServlet;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.matrixproxy.servlet.data.MatrixIdentifier;
import com.openexchange.matrixproxy.servlet.data.MatrixLogin;
import com.openexchange.matrixproxy.servlet.data.MatrixLoginResponse;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.session.Session;

/**
 * {@link MatrixProxyServlet}
 */
@NonNullByDefault
public final class MatrixProxyServlet extends SessionServlet implements Reloadable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -5474935857715684804L;

    public static final String PATH = "/servlet/matrixproxy";

    private static final Logger LOG = logger(MatrixProxyServlet.class);

    private final HttpClientService        httpClientService;
    private final LeanConfigurationService lcs;

    private static final String LOGIN_PATH = "/_matrix/client/r0/login";
    private static final String USER_PARAM = "user";

    @SuppressWarnings({
        "null",
        "unused" })
    private static final class Config {

        public final String       matrixUrl;
        public final String       botUrl;
        public final String       asToken;
        public final List<String> pathWhitelist;

        public Config(String matrixUrl, String botUrl, String asToken, List<String> pathWhitelist) {
            super();
            this.matrixUrl = matrixUrl;
            this.botUrl = botUrl;
            this.asToken = asToken;
            this.pathWhitelist = pathWhitelist;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((asToken == null) ? 0 : asToken.hashCode());
            result = prime * result + ((botUrl == null) ? 0 : botUrl.hashCode());
            result = prime * result + ((matrixUrl == null) ? 0 : matrixUrl.hashCode());
            result = prime * result + ((pathWhitelist == null) ? 0 : pathWhitelist.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Config other = (Config) obj;
            if (asToken == null) {
                if (other.asToken != null)
                    return false;
            } else if (!asToken.equals(other.asToken))
                return false;
            if (botUrl == null) {
                if (other.botUrl != null)
                    return false;
            } else if (!botUrl.equals(other.botUrl))
                return false;
            if (matrixUrl == null) {
                if (other.matrixUrl != null)
                    return false;
            } else if (!matrixUrl.equals(other.matrixUrl))
                return false;
            if (pathWhitelist == null) {
                if (other.pathWhitelist != null)
                    return false;
            } else if (!pathWhitelist.equals(other.pathWhitelist))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Config [matrixUrl=" + matrixUrl + ", botUrl=" + botUrl + ", asToken=" + asToken + ", pathWhitelist=" + pathWhitelist + "]";
        }

    }

    private final AtomicReference<Config> configRef;

    public MatrixProxyServlet(final HttpClientService httpClientService, final LeanConfigurationService lcs) throws OXException {
        super();
        this.httpClientService = httpClientService;
        this.lcs = lcs;
        this.configRef = new AtomicReference<Config>(loadConfig());
    }

    private String mandatory(MatrixProxyProperties prop) throws OXException {
        final String p = lcs.getProperty(prop);
        if (null == p || Strings.isEmpty(p)) {
            throw OXException.general(f("missing mandatory property value for \"%s\"", prop.getFQPropertyName()));
        }
        return p;
    }

    private Config loadConfig() throws OXException {
        final String serverUrl = mandatory(MatrixProxyProperties.serverUrl);
        final String botUrl = mandatory(MatrixProxyProperties.botUrl);
        final String asToken = mandatory(MatrixProxyProperties.asToken);
        final String tmpWhiteList = mandatory(MatrixProxyProperties.apiWhitelist);

        final List<String> whiteList = notNull(Splitter.on(",").omitEmptyStrings().trimResults().splitToList(tmpWhiteList));
        final Config conf = new Config(serverUrl, botUrl, asToken, whiteList);
        LOG.info("using configuration: {}", conf);
        return conf;
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    @Override
    protected void doPost(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    @Override
    protected void doPut(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    @Override
    protected void doDelete(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    protected void execute(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws ServletException, IOException {
        req = notNull(req);
        resp = notNull(resp);
        final String apiUser = req.getParameter(USER_PARAM);
        if (null == apiUser) {
            errorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, f("Missing parameter \"%s\"", USER_PARAM));
            return;
        }

        final String apiPath;
        try {
            apiPath = validatePath(req.getPathInfo());
        } catch (IOException e) {
            errorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        final Session session = getSessionObject(req);

        if (null == session) {
            errorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "no valid session found");
            return;
        }

        final Config config = configRef.get();
        final MatrixLoginResponse loginResp;
        try {
            loginResp = matrixLogin(apiUser);
        } catch (ParseException | OXException | IOException e1) {
            errorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, f("failed to login to \"%s\"", config.matrixUrl));
            return;
        }

        final String userId = loginResp.getUser_id();
        if (Strings.isEmpty(userId) || !userId.startsWith(f("@%s", apiUser))) {
            errorResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "missing or invalid user_id in login response");
            return;
        }

        final HttpRequestBase proxyRequest;
        switch (req.getMethod()) {
            case "POST":
                proxyRequest = new HttpPost();
                ((HttpPost) proxyRequest).setEntity(new InputStreamEntity(req.getInputStream()));
                break;

            case "PUT":
                proxyRequest = new HttpPut();
                ((HttpPut) proxyRequest).setEntity(new InputStreamEntity(req.getInputStream()));
                break;

            case "DELETE":
                proxyRequest = new HttpDelete();
                break;

            default:
                proxyRequest = new HttpGet();
                break;
        }

        try {
            proxyRequest.setURI(new URI(f("%s/%s", config.botUrl, apiPath)));
        } catch (URISyntaxException e) {
            errorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        ((HttpUriRequest) proxyRequest).setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        ((HttpUriRequest) proxyRequest).setHeader("X-Matrix-User-Token", mapper().writeValueAsString(loginResp));
        LOG.debug("URL: \"{}\"", proxyRequest.getURI());
        final HttpResponse postResp = httpClient().execute(proxyRequest);
        LOG.debug("Response: \"{}\"", postResp);

        resp.setStatus(postResp.getStatusLine().getStatusCode());

        final InputStream responseStream = postResp.getEntity().getContent();
        try {
            final ServletOutputStream outputStream = resp.getOutputStream();
            final byte[] buf = new byte[8192];
            int read = -1;
            while ((read = responseStream.read(buf)) > 0) {
                outputStream.write(buf, 0, read);
            }
            outputStream.flush();
        } finally {
            Streams.close(responseStream);
        }

    }

    private void errorResponse(final HttpServletResponse resp, int code, @Nullable final String message) throws IOException {
        final String msg = null == message ? "error" : message;
        LOG.error(msg);
        resp.sendError(code, msg);
    }

    private String validatePath(@Nullable final String path) throws IOException {
        if (null == path || Strings.isEmpty(path)) {
            throw new IOException("missing or empty path");
        }
        final Config config = configRef.get();
        for (final String wp : config.pathWhitelist) {
            if (path.startsWith(wp)) {
                return path;
            }
        }
        throw new IOException(f("path \"%s\" not allowed", path));
    }

    private MatrixLoginResponse matrixLogin(final String user) throws JsonProcessingException, UnsupportedEncodingException, ParseException, OXException, IOException {
        final Config config = configRef.get();
        final HttpPost postReq = new HttpPost(f("%s/%s", config.matrixUrl, LOGIN_PATH));
        ((HttpUriRequest) postReq).setHeader(HttpHeaders.AUTHORIZATION, f("Bearer %s", config.asToken));
        ((HttpUriRequest) postReq).setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        final MatrixLogin loginData = MatrixLogin.builder()
            .withType("uk.half-shot.msc2778.login.application_service")
            .withIdentifier(
                MatrixIdentifier.builder()
                    .withType("m.id.user")
                    .withUser(user)
                    .build())
            .build();
        postReq.setEntity(
            new StringEntity(
                mapper().writeValueAsString(
                    loginData)));

        try {
            LOG.debug("URL: \"{}\"", postReq.getURI());
            LOG.debug("Params: {}", loginData);
            final MatrixLoginResponse loginResp = handleResponse(httpClient().execute(postReq), MatrixLoginResponse.class);
            LOG.debug("Login response: \"{}\"", loginResp);
            return loginResp;
        } catch (ParseException | OXException | IOException e) {
          LOG.error(e.getMessage(), e);
          throw e;
        } finally {
            HttpClients.close(postReq, null);
        }
    }

    private static final <T> T fromJson(final String json, final Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        return notNull(mapper().readValue(json, clazz));
    }

    private <T> T handleResponse(@Nullable final HttpResponse resp, final Class<T> clazz) throws ParseException, IOException, OXException {
        if (null == resp) {
            throw OXException.general("Reponse is null");
        }
        int statusCode = resp.getStatusLine().getStatusCode();
        try {
            final String entity = EntityUtils.toString(resp.getEntity(), "UTF-8");
            if (null == entity) {
                throw OXException.general("Empty response");
            }
            LOG.debug("Code: {}, Response: \"{}\"", statusCode, entity);
            if (statusCode < 200 || statusCode > 201) {
                throw OXException.general(f("Error %d: %s", statusCode, resp.getStatusLine().getReasonPhrase()));
            }
            return fromJson(entity, clazz);
        } catch (ParseException | IOException | OXException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            HttpClients.close(resp, true);
        }
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper();
    }

    private ManagedHttpClient httpClient() {
        return httpClientService.getHttpClient(MatrixProxyHttpClientConfigProvider.CLIENTID);
    }

    @Override
    public void reloadConfiguration(@Nullable ConfigurationService configService) {
        final Config oldConfig = configRef.get();
        try {
            final Config newConfig = loadConfig();
            if (!oldConfig.equals(newConfig)) {
                LOG.info("configuration changed, using new values: {}", newConfig);
                configRef.set(newConfig);
            }
        } catch (OXException e) {
            LOG.error("unable to load configutation", e);
        }
    }

    @Override
    public @Nullable Interests getInterests() {
        return Reloadables.interestsForProperties(
            MatrixProxyProperties.apiWhitelist.getFQPropertyName(),
            MatrixProxyProperties.asToken.getFQPropertyName(),
            MatrixProxyProperties.botUrl.getFQPropertyName(),
            MatrixProxyProperties.serverUrl.getFQPropertyName());
    }

}
