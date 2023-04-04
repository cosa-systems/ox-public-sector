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

import static com.openexchange.conference.element.impl.N.notNull;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.conference.element.exception.ElementExceptionCodes;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;

/**
 * {@link ElementConfiguration}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class ElementConfiguration {

    private static final String ENABLED_PROPERTY          = "com.openexchange.conference.element.enabled";
    private static final String MEETING_HOST_URL_PROPERTY = "com.openexchange.conference.element.meetingHostUrl";
    private static final String AUTH_TOKEN_PROPERTY       = "com.openexchange.conference.element.authToken";
    private static final String HTTP_CLIENTID_PROPERTY    = "com.openexchange.conference.element.httpClientId";
    private static final String HTTP_CLIENTID_DEFAULT     = "OX_ELEMENT";

    @Nullable
    private final String  meetingHostUrl;
    @Nullable
    private final String  authToken;
    private final String  httpClientId;
    private final boolean enabled;

    private ElementConfiguration(boolean enabled, @Nullable String meetingHostUrl, @Nullable String authToken, String httpClientId) {
        super();
        this.enabled = enabled;
        this.meetingHostUrl = meetingHostUrl;
        this.authToken = authToken;
        this.httpClientId = httpClientId;
    }

    public static String[] allProperties() {
        return new String[] {
            ENABLED_PROPERTY,
            MEETING_HOST_URL_PROPERTY,
            AUTH_TOKEN_PROPERTY,
            HTTP_CLIENTID_PROPERTY };
    }

    public static ElementConfiguration getConfig(ConfigurationService configService) throws OXException {
        final boolean enabled = configService.getBoolProperty(ENABLED_PROPERTY, false);
        final String meetingHostUrl = configService.getProperty(MEETING_HOST_URL_PROPERTY);
        if (Strings.isEmpty(meetingHostUrl) && enabled) {
            throw ElementExceptionCodes.CONFIGURATION_ERROR.create(MEETING_HOST_URL_PROPERTY);
        }
        final String authToken = configService.getProperty(AUTH_TOKEN_PROPERTY);
        if (Strings.isEmpty(authToken) && enabled) {
            throw ElementExceptionCodes.CONFIGURATION_ERROR.create(AUTH_TOKEN_PROPERTY);
        }

        return new ElementConfiguration(
            enabled,
            meetingHostUrl,
            authToken,
            notNull(configService.getProperty(HTTP_CLIENTID_PROPERTY, HTTP_CLIENTID_DEFAULT)));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable String getMeetingHostUrl() {
        return meetingHostUrl;
    }

    public @Nullable String getAuthToken() {
        return authToken;
    }

    public String getHttpClientId() {
        return httpClientId;
    }

    @Override
    public String toString() {
        return "ElementConfiguration [meetingHostUrl=" + meetingHostUrl + ", authToken=" + authToken + ", httpClientId=" + httpClientId + ", enabled=" + enabled + "]";
    }

    @SuppressWarnings("null")
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authToken == null) ? 0 : authToken.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((httpClientId == null) ? 0 : httpClientId.hashCode());
        result = prime * result + ((meetingHostUrl == null) ? 0 : meetingHostUrl.hashCode());
        return result;
    }

    @SuppressWarnings({
        "null",
        "unused" })
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ElementConfiguration other = (ElementConfiguration) obj;
        if (authToken == null) {
            if (other.authToken != null)
                return false;
        } else if (!authToken.equals(other.authToken))
            return false;
        if (enabled != other.enabled)
            return false;
        if (httpClientId == null) {
            if (other.httpClientId != null)
                return false;
        } else if (!httpClientId.equals(other.httpClientId))
            return false;
        if (meetingHostUrl == null) {
            if (other.meetingHostUrl != null)
                return false;
        } else if (!meetingHostUrl.equals(other.meetingHostUrl))
            return false;
        return true;
    }

}
