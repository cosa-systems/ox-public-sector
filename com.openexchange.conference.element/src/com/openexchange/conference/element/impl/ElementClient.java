/*
 * @copyright Copyright (c) Open-Xchange GmbH, Germany <info@open-xchange.com>
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

import static com.openexchange.conference.element.impl.JsonUtils.toJson;
import static com.openexchange.conference.element.impl.N.logger;
import static java.lang.String.format;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.conference.element.exception.ElementExceptionCodes;
import com.openexchange.conference.element.impl.dto.ElementMeeting;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;

/**
 * {@link ElementClient}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class ElementClient {

    private static final Logger LOG = logger(ElementClient.class);

    private static final String UPDATE_PATH = "/v1/meeting/update";
    private static final String DELETE_PATH = "/v1/meeting/close";

    private final ManagedHttpClient httpClient;
    private final String            url;
    private final String            token;

    public ElementClient(ManagedHttpClient httpClient, String url, String token) {
        super();
        this.httpClient = httpClient;
        this.url = url;
        this.token = token;
    }

    public void updateMeeting(ElementMeeting me) throws OXException {
        final HttpPut req = new HttpPut(format("%s/%s", url, UPDATE_PATH));
        req.addHeader(HttpHeaders.AUTHORIZATION, format("Bearer %s", token));
        HttpResponse response = null;
        try {
            req.setEntity(new StringEntity(toJson(me), ContentType.APPLICATION_JSON));
            LOG.debug("PUT {}, Data {}", req.getURI(), me);
            response = httpClient.execute(req);
            handleResponse(response);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw ElementExceptionCodes.IO_ERROR.create(e.getMessage());
        } finally {
            HttpClients.close(req, response);
        }
    }

    public void deleteMeeting(String roomId) throws OXException {
        final HttpPost req = new HttpPost(format("%s/%s", url, DELETE_PATH));
        req.addHeader(HttpHeaders.AUTHORIZATION, format("Bearer %s", token));
        final JSONObject json = new JSONObject(2);
        HttpResponse response = null;
        try {
            json.put("method", "kick_all_participants");
            json.put("target_room_id", roomId);
            req.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
            LOG.debug("POST {}, Data {}", req.getURI(), json);
            response = httpClient.execute(req);
            handleResponse(response);
        } catch (JSONException | IOException e) {
            LOG.error(e.getMessage(), e);
            throw ElementExceptionCodes.IO_ERROR.create(e.getMessage());
        } finally {
            HttpClients.close(req, response);
        }
    }

    private void handleResponse(@Nullable HttpResponse response) throws OXException, UnsupportedOperationException, IOException {
        if (null == response) {
            return;
        }
        final StatusLine sl = response.getStatusLine();
        if (sl.getStatusCode() != 200) {
            final String errorMessage;
            final Header contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            if (null != contentType && contentType.getValue().equalsIgnoreCase(ContentType.APPLICATION_JSON.toString())) {
                errorMessage = httpEntity2ErrorMessage(response.getEntity());
            } else {
                errorMessage = sl.getReasonPhrase();
            }
            throw ElementExceptionCodes.ELEMENT_ERROR.create(errorMessage);
        }
    }

    private @Nullable String httpEntity2ErrorMessage(@Nullable HttpEntity httpEntity) throws UnsupportedOperationException, IOException {
        if (null != httpEntity) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(httpEntity.getContent()))) {
                final String responseString = r.lines().collect(Collectors.joining("\n"));
                if (null != responseString && !Strings.isEmpty(responseString)) {
                    try {
                        JSONObject json = JSONServices.parseObject(responseString);
                        if (json.hasAndNotNull("message")) {
                            return json.getString("message");
                        }
                    } catch (JSONException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
        return null;
    }
}
