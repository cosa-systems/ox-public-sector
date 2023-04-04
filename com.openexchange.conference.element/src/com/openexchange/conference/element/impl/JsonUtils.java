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

import static com.openexchange.conference.element.impl.N.logger;
import static com.openexchange.conference.element.impl.N.notNull;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.slf4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openexchange.annotation.NonNullByDefault;

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
}
