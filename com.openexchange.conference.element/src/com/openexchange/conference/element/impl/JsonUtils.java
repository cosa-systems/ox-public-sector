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

import static com.openexchange.conference.element.impl.N.logger;
import static com.openexchange.conference.element.impl.N.notNull;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import org.slf4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;

/**
 * {@link JsonUtils}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public enum JsonUtils {
    ;

    protected static final Logger LOG = logger(JsonUtils.class);

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonMappingException, JsonProcessingException {
        return notNull(
            JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build()
                .readValue(json, clazz));
    }

    public static <T> String toJson(T instance) throws JsonProcessingException {
        final TimeZone tz = TimeZone.getDefault();
        LOG.debug("using timezone: {}", tz);
        return notNull(
            JsonMapper.builder()
                //                .enable(SerializationFeature.INDENT_OUTPUT)
                .addModule(new JavaTimeModule())
                .defaultTimeZone(tz)
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.XXX"))
                .build()
                .writeValueAsString(instance));
    }

    public static @Nullable String getUserUuidClaimValue(String accessToken, String claimName) {
        final String[] chunks = accessToken.split("\\.");
        if (chunks.length < 2) {
            LOG.error("invalid access token: \"{}\"", accessToken);
            return null;
        }
        final Base64.Decoder decoder = Base64.getUrlDecoder();
        final String payload;
        try {
            payload = new String(decoder.decode(chunks[1]));
        } catch (Exception e) {
            LOG.error("unable to decode access token payload: \"{}\": {}", chunks[1], e.getMessage(), e);
            return null;
        }
        try {
            final JSONValue json = JSONObject.parse(payload);
            final JSONObject jobj = json.toObject();
            if (null != jobj && jobj.hasAndNotNull(claimName)) {
                final String claimValue = (String) jobj.get(claimName);
                LOG.debug("value of claim with name {} = \"{}\"", claimName, claimValue);
                return claimValue;
            }
        } catch (JSONException e) {
            LOG.error("unable to get claim name \"{}\" from payload \"{}\": {}", claimName, payload, e.getMessage(), e);
            return null;
        }
        LOG.error("unable to get claim name \"{}\" from payload \"{}\": {}", claimName, payload);
        return null;
    }
}
