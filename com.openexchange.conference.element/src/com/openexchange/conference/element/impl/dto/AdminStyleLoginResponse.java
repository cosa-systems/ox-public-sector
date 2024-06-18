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

package com.openexchange.conference.element.impl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link AdminStyleLoginResponse}
 * See <a href=https://spec.matrix.org/v1.4/application-service-api/#server-admin-style-permissions>
 * https://spec.matrix.org/v1.4/application-service-api/#server-admin-style-permissions</a>
 * 
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminStyleLoginResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("user_id")
    private String userId;

    public String getAccessToken() {
        return accessToken;
    }

    public String getUserId() {
        return userId;
    }

}
