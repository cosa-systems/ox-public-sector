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

package com.openexchange.conference.element;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.conference.element.impl.JsonUtils;
import com.openexchange.conference.element.impl.dto.ElementMeeting;
import com.openexchange.conference.element.impl.dto.ExternalData;
import com.openexchange.conference.element.impl.dto.OXCalReference;

/**
 * {@link JsonUtilsTest}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class JsonUtilsTest extends AbstractElementTest {

    @Test
    void testFromJson() throws IOException {
        final ElementMeeting um = JsonUtils.fromJson(loadFile("MeetingUpdate.json"), ElementMeeting.class);
        assertThat(um).isNotNull();
        assertThat(um.getDescription()).isBlank();
        assertThat(um.getTitle()).isNotBlank().isEqualTo("Konferenz");
        assertThat(um.isEnableAutoDeletion()).isFalse();
        assertThat(um.getTargetRoomId()).isNotBlank().isEqualTo("!jUMFuPtcFZgavxdOeP:matrix.viktor.develop.souvap-univention.de");
        assertThat(um.getStartTime()).isNotNull().isEqualTo("2023-03-15T12:00:00+01:00");
        assertThat(um.getEndTime()).isNotNull().isEqualTo("2023-03-15T13:00:00+01:00");
        final ExternalData ed = um.getExternalData();
        assertThat(ed).isNotNull();
        final OXCalReference oxData = ed.getIoDotOx();
        assertThat(oxData).isNotNull();
        assertThat(oxData.getFolder()).isNotBlank().isEqualTo("cal://0/1595");
        assertThat(oxData.getId()).isNotBlank().isEqualTo("2");
        assertThat(oxData.getRrules()).isNotNull().isEmpty();
    }

    @Test
    void testToJson() throws JsonMappingException, JsonProcessingException, FileNotFoundException, IOException {
        final ElementMeeting um = JsonUtils.fromJson(loadFile("MeetingUpdate.json"), ElementMeeting.class);

        @SuppressWarnings("null") final ElementMeeting comp = ElementMeeting.builder()
            .withTitle("Konferenz")
            .withDescription("")
            .withEnableAutoDeletion(false)
            .withTargetRoomId("!jUMFuPtcFZgavxdOeP:matrix.viktor.develop.souvap-univention.de")
            .withStartTime(ZonedDateTime.parse("2023-03-15T12:00:00+01:00"))
            .withEndTime(ZonedDateTime.parse("2023-03-15T13:00:00+01:00"))
            .withExternalData(ExternalData.builder()
                .withIoDotOx(OXCalReference.builder()
                    .withFolder("cal://0/1595")
                    .withId("2")
                    .build())
                .build())
            .build();
        //        @SuppressWarnings("null") final String jsonData = JsonUtils.toJson(comp, TimeZone.getTimeZone("Europe/Berlin"));
        final String jsonData = JsonUtils.toJson(comp);
        LOG.info(jsonData);
        assertThat(jsonData).matches(format(".*title.*%s.*.*?,.*", um.getTitle()));
        assertThat(jsonData).matches(format(".*target_room_id.*%s.*.*?,.*", um.getTargetRoomId()));
        assertThat(jsonData).matches(format(".*enable_auto_deletion.*%s.*.*?,.*", um.isEnableAutoDeletion()));
        assertThat(jsonData).matches(format(".*description.*%s.*.*?,.*", um.getDescription()));
        assertThat(jsonData).matches(format(".*start_time.*%s.*.*?,.*", um.getStartTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
        assertThat(jsonData).matches(format(".*end_time.*%s.*.*?,.*", um.getEndTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
        assertThat(jsonData).matches(format(".*folder.*%s.*.*?,.*", um.getExternalData().getIoDotOx().getFolder()));
        assertThat(jsonData).matches(format(".*id.*%s.*.*?,.*", um.getExternalData().getIoDotOx().getId()));
    }

    @Test
    void testGetUserUuidClaimValue() {
        String userId = JsonUtils.getUserUuidClaimValue(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI4TXQ4YTlqWmdmZXk2bHVDZnB3NEFOOW5OaVliNVd4NEVaU3JkMkNWVkNnIn0.eyJleHAiOjE3MTgzNDY4NzAsImlhdCI6MTcxODM0NjU3MCwiYXV0aF90aW1lIjoxNzE4MzQ0MTQ2LCJqdGkiOiJkYmVkYmVjNS05Njc2LTRkNWQtOTEzMC0yNGMwZTI5NThlMjEiLCJpc3MiOiJodHRwczovL2lkLm94LWNob2VnZXIub3BlbmRlc2suc2l0ZS9yZWFsbXMvb3BlbmRlc2siLCJzdWIiOiJmOjlhZThhNDc0LWQ2YjktNGU2NS1hYzA0LTcxMmM2ZWEzYzc3ZjpkZWZhdWx0LnVzZXIiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJvcGVuZGVzay1veGFwcHN1aXRlIiwibm9uY2UiOiI2alBoWWxYR3VyQXRCZFY3RlkyV0h3X0Y3RS0yVnU3cnVPSDNsV09LVmlVIiwic2Vzc2lvbl9zdGF0ZSI6Ijg4YzNjY2QyLWU1YzEtNGE0Mi05N2FhLTRjZTdhZmNmMTNkMyIsInNjb3BlIjoib3BlbmlkIG9wZW5kZXNrIHJlYWRfY29udGFjdHMgd3JpdGVfY29udGFjdHMiLCJzaWQiOiI4OGMzY2NkMi1lNWMxLTRhNDItOTdhYS00Y2U3YWZjZjEzZDMiLCJvcGVuZGVza191c2VybmFtZSI6ImRlZmF1bHQudXNlciIsImNvbnRleHQiOiIxIiwib3BlbmRlc2tfdXNlcnV1aWQiOiJkMmVlMmVjZS1iNzgyLTEwM2UtOTgwMC05MTRiYjMzOTgzMTYifQ.MgW62cw7RlVUy7I9L6ae7ZM6ZGkCrGpz08fCmH5D5Wqqwcd1Q7EF6QJazE3iyMTQtdFdSy-gGWnAUBBaqQrzhLlCsFg7TNCE5VDdSIj3ICVK__91J0PnQzYPqA6CJZ-5C2Tlo5h1D_Yfy6SaLBAkgC9YB-QiBMMjECiXrSQkSQzSzqOoVFBctwN1PSCaF3fxXsCqDBLVI8V65BCV3WaMeHl8n9x8_YMRETD_ZKULViaMoLTaaeZSDjTJydXQHCnLz-AkTEcaVKbM70jXyrdBHZVpUV9ZfKaIQEiSw2nFNCDEvlzT3kFv_YAq9ckd5NIOHNbU_ilKHayrJSkhLG6Imw",
            "opendesk_useruuid");
        assertThat(userId).isNotNull().isEqualTo("d2ee2ece-b782-103e-9800-914bb3398316");
    }
}
