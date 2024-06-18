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

package com.openexchange.conference.element.impl;

import static com.openexchange.conference.element.impl.JsonUtils.fromJson;
import static com.openexchange.conference.element.impl.JsonUtils.toJson;
import static com.openexchange.conference.element.impl.N.logger;
import static com.openexchange.conference.element.impl.N.notNull;
import static java.lang.String.format;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.conference.element.exception.ElementExceptionCodes;
import com.openexchange.conference.element.impl.dto.AdminStyleLoginIdentifier;
import com.openexchange.conference.element.impl.dto.AdminStyleLoginRequest;
import com.openexchange.conference.element.impl.dto.AdminStyleLoginResponse;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;

/**
 * {@link MatrixClient}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class MatrixClient {

    private static final Logger LOG = logger(MatrixClient.class);

    private final ManagedHttpClient httpClient;
    private final String            url;
    private final String            token;

    public MatrixClient(ManagedHttpClient httpClient, String url, String token) {
        super();
        this.httpClient = httpClient;
        this.url = url;
        this.token = token;
    }

    public String getLoginToken(String matrixUserIdentifier) throws OXException {
        final HttpPost req = new HttpPost(url);
        req.addHeader(HttpHeaders.AUTHORIZATION, format("Bearer %s", token));
        final AdminStyleLoginRequest loginRequest = AdminStyleLoginRequest.builder()
            .withType("m.login.application_service")
            .withIdentifier(AdminStyleLoginIdentifier.builder()
                .withType("m.id.user")
                .withUser(matrixUserIdentifier)
                .build())
            .build();
        HttpResponse response = null;
        try {
            req.setEntity(new StringEntity(toJson(loginRequest), ContentType.APPLICATION_JSON));
            LOG.debug("PUT {}, Data {}", req.getURI(), loginRequest);
            response = notNull(httpClient.execute(req));
            final AdminStyleLoginResponse loginResponse = handleResponse(response, AdminStyleLoginResponse.class);
            LOG.debug("mapped response {}", loginResponse);
            return notNull(loginResponse.getAccessToken());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw ElementExceptionCodes.IO_ERROR.create(e.getMessage());
        } finally {
            HttpClients.close(req, response);
        }
    }

    private <T> T handleResponse(HttpResponse response, Class<T> clazz) throws OXException, UnsupportedOperationException, IOException {
        final StatusLine sl = response.getStatusLine();
        if (sl.getStatusCode() != 200) {
            final String errorMessage = httpEntity2ErrorMessage(response.getEntity());
            throw ElementExceptionCodes.ELEMENT_ERROR.create(errorMessage);
        } else {
            final String entity;
            try {
                entity = EntityUtils.toString(response.getEntity(), "UTF-8");
                LOG.debug("retrieved response {}", entity);
                if (null == entity) {
                    throw ElementExceptionCodes.IO_ERROR.create("no response data");
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                throw new OXException(e);
            }
            return fromJson(entity, clazz);
        }
    }

    private String httpEntity2ErrorMessage(@Nullable HttpEntity httpEntity) {
        if (null != httpEntity) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(httpEntity.getContent()))) {
                final String responseString = r.lines().collect(Collectors.joining("\n"));
                if (null != responseString && !Strings.isEmpty(responseString)) {
                    LOG.debug("error response: {}", responseString);
                    try {
                        JSONObject json = JSONServices.parseObject(responseString);
                        if (json.hasAndNotNull("error")) {
                            final String errorMessage = json.getString("error");
                            LOG.error("{} returned \"{}\"", url, errorMessage);
                            return notNull(errorMessage);
                        }
                    } catch (JSONException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            } catch (UnsupportedOperationException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return "unknown error";
    }
}
