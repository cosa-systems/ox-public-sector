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

package com.openexchange.conference.element;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.conference.element.impl.ElementClient;
import com.openexchange.conference.element.impl.JsonUtils;
import com.openexchange.conference.element.impl.dto.ElementMeeting;
import com.openexchange.conference.element.impl.dto.ExternalData;
import com.openexchange.conference.element.impl.dto.OXCalReference;
import com.openexchange.exception.OXException;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.rest.client.httpclient.SimHttpClientService;

/**
 * {@link ElementClientTest}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class ElementClientTest extends AbstractElementTest {

    private static final String MEETING_UPDATE_JSON = "MeetingUpdate.json";
    private static final String TOKEN               = "verysecrettoken";
    private static final int    MOCK_HTTP_PORT      = 3333;
    @SuppressWarnings("null")
    private static final String AUTH_HEADER         = format("Bearer %s", TOKEN);
    @SuppressWarnings("null")
    private static final String MOCK_HTTP_URL       = format("http://localhost:%d", MOCK_HTTP_PORT);

    @SuppressWarnings("null")
    private WireMockServer mockServer;

    @BeforeAll
    public void startWiremock() {
        mockServer = new WireMockServer(options().port(MOCK_HTTP_PORT));
        mockServer.start();
    }

    @AfterAll
    public void stopWiremock() {
        mockServer.stop();
    }

    @Test
    public void testUpdateMeeting() throws FileNotFoundException, IOException, OXException {
        final String jsonData = loadFile(MEETING_UPDATE_JSON);
        LOG.info("ORIG= {}", jsonData);
        mockServer.stubFor(
            put("/v1/meeting/update")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .withRequestBody(equalToJson(jsonData))
                .willReturn(ok()));
        final ElementClient mec = new ElementClient(getHttpClient(), MOCK_HTTP_URL, TOKEN);
        final ElementMeeting fromJson = JsonUtils.fromJson(jsonData, ElementMeeting.class);
        LOG.info("MAPPED= {}", fromJson);
        mec.updateMeeting(fromJson);
    }

    @Test
    public void testDeleteMeeting() throws JsonMappingException, JsonProcessingException, FileNotFoundException, IOException, JSONException, OXException  {
        final ElementMeeting mu = JsonUtils.fromJson(loadFile(MEETING_UPDATE_JSON), ElementMeeting.class);
        final String targetRoomId = mu.getTargetRoomId();
        final JSONObject jsonData = new JSONObject()
            .put("target_room_id", targetRoomId)
            .put("method", "kick_all_participants");
        mockServer.stubFor(
            post("/v1/meeting/close")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .withRequestBody(equalToJson(jsonData.toString()))
                .willReturn(ok()));
        final ElementClient mec = new ElementClient(getHttpClient(), MOCK_HTTP_URL, TOKEN);
        mec.deleteMeeting(targetRoomId);
    }

    @Test
    public void testDeleteMeetingWrongRoom() throws JSONException {
        final String errorMessage = "target_room_id must be a matrix room id starting with !";
        final JSONObject jsonData = new JSONObject()
            .put("error", "Bad Request")
            .put("message", new JSONArray().put(errorMessage))
            .put("statusCode", 400);
        mockServer.stubFor(
            post("/v1/meeting/close")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_BAD_REQUEST)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(jsonData.toString())));
        final ElementClient mec = new ElementClient(getHttpClient(), MOCK_HTTP_URL, TOKEN);
        final Throwable thrown = catchThrowable(() -> {
            mec.deleteMeeting("doesnotmatter");
        });
        assertThat(thrown).isInstanceOf(OXException.class);
        assertThat(thrown.getMessage()).contains(errorMessage);
    }

    @Test
    public void testDeleteMeetingWrongAuth() throws JSONException {
        final String errorMessage = "Forbidden resource";
        final JSONObject jsonData = new JSONObject()
            .put("error", "Forbidden")
            .put("message", errorMessage)
            .put("statusCode", 403);
        mockServer.stubFor(
            post("/v1/meeting/close")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_FORBIDDEN)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(jsonData.toString())));
        final ElementClient mec = new ElementClient(getHttpClient(), MOCK_HTTP_URL, TOKEN);
        final Throwable thrown = catchThrowable(() -> {
            mec.deleteMeeting("doesnotmatter");
        });
        assertThat(thrown).isInstanceOf(OXException.class);
        assertThat(thrown.getMessage()).contains(errorMessage);
    }

    @Test
    public void testUpdateMeetingWrongRoom() throws JSONException, JsonProcessingException {
        final String fakeRoom = "!fsdfgsdfgsdgsdg:bla";
        @SuppressWarnings("null") final ElementMeeting fakeMeeting = ElementMeeting.builder()
            .withTitle("Another Conference")
            .withDescription("Nice Conference")
            .withEnableAutoDeletion(false)
            .withTargetRoomId(fakeRoom)
            .withStartTime(ZonedDateTime.now())
            .withEndTime(ZonedDateTime.now().plusHours(2))
            .withExternalData(ExternalData.builder()
                .withIoDotOx(OXCalReference.builder()
                    .withFolder("cal://0/1337")
                    .withId("42")
                    .build())
                .build())
            .build();
        final String errorMessage = format("Error Code: M_FORBIDDEN, Error: User @controlroom-bot:element.example.com not in room %s, and room previews are disabled", fakeRoom);
        final JSONObject jsonData = new JSONObject()
            .put("message", errorMessage)
            .put("statusCode", 500);
        mockServer.stubFor(
            put("/v1/meeting/update")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .withRequestBody(equalToJson(JsonUtils.toJson(fakeMeeting)))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(jsonData.toString())));
        final ElementClient mec = new ElementClient(getHttpClient(), MOCK_HTTP_URL, TOKEN);
        final Throwable thrown = catchThrowable(() -> {
            mec.updateMeeting(fakeMeeting);
        });
        assertThat(thrown).isInstanceOf(OXException.class);
        assertThat(thrown.getMessage()).contains(errorMessage);
    }

    private ManagedHttpClient getHttpClient() {
        final SimHttpClientService shcs = new SimHttpClientService();
        return shcs.getHttpClient("TESTCLIENT");
    }

}
