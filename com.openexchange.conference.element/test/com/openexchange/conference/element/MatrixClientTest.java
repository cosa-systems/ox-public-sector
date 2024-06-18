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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.conference.element.impl.JsonUtils;
import com.openexchange.conference.element.impl.MatrixClient;
import com.openexchange.conference.element.impl.dto.AdminStyleLoginIdentifier;
import com.openexchange.conference.element.impl.dto.AdminStyleLoginRequest;
import com.openexchange.exception.OXException;

/**
 * {@link MatrixClientTest}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class MatrixClientTest extends AbstractElementTest {

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
    public void testGetLoginToken() throws FileNotFoundException, IOException, OXException {
        final String jsonData = loadFile("MatrixLoginResponse.json");
        final String userIdentifier = "some-user-id";
        LOG.info("ORIG= {}", jsonData);
        AdminStyleLoginRequest loginRequest = AdminStyleLoginRequest.builder()
            .withIdentifier(AdminStyleLoginIdentifier.builder()
                .withType("m.id.user")
                .withUser(userIdentifier)
                .build())
            .withType("m.login.application_service")
            .build();
        mockServer.stubFor(
            post("/")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .withRequestBody(equalToJson(JsonUtils.toJson(loginRequest)))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(jsonData.toString())));
        final MatrixClient mec = new MatrixClient(getHttpClient(), MOCK_HTTP_URL, TOKEN);
        String loginToken = mec.getLoginToken(userIdentifier);
        assertThat(loginToken).isNotNull().isEqualTo("syt_OGI0MGEyODQtODc5Yy0xMDNlLThkNjctODcyODJjZDZiNjZh_QRyveQCyjukAhnGwGtOv_4Jb3Tp");
    }

}
