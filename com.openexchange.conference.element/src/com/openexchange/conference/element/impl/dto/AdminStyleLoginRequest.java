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

import com.openexchange.annotation.NonNullByDefault;
import javax.annotation.Generated;

/**
 * {@link AdminStyleLoginRequest}
 *
 * See <a href=https://spec.matrix.org/v1.4/application-service-api/#server-admin-style-permissions>
 * https://spec.matrix.org/v1.4/application-service-api/#server-admin-style-permissions</a>
 * 
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class AdminStyleLoginRequest {

    private String                    type;
    private AdminStyleLoginIdentifier identifier;

    public String getType() {
        return type;
    }

    public AdminStyleLoginIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "AdminStyleLoginRequest [type=" + type + ", identifier=" + identifier + "]";
    }

    @Generated("SparkTools")
    private AdminStyleLoginRequest(Builder builder) {
        this.type = builder.type;
        this.identifier = builder.identifier;
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {

        private String                    type;
        private AdminStyleLoginIdentifier identifier;

        @SuppressWarnings("null")
        private Builder() {}

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withIdentifier(AdminStyleLoginIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public AdminStyleLoginRequest build() {
            return new AdminStyleLoginRequest(this);
        }
    }

}
