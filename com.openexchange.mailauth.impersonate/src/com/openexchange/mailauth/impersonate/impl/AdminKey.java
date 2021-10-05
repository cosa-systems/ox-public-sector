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

package com.openexchange.mailauth.impersonate.impl;

import static com.openexchange.mailauth.impersonate.helper.N.notNull;
import java.net.URI;
import java.util.Objects;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;

@NonNullByDefault
public class AdminKey {

    private final URI      tokenEndpoint;
    @Nullable
    private final URI      tokenLogoutEndpoint;
    private final String   adminUsername;
    private final Secret   adminPassword;
    private final ClientID clientId;
    private final Secret   clientSecret;

    public AdminKey(URI tokenEndpoint, @Nullable URI tokenLogoutEndpoint, String adminUsername, Secret adminPassword, ClientID clientId, Secret clientSecret) {
        super();
        this.tokenEndpoint = tokenEndpoint;
        this.tokenLogoutEndpoint = tokenLogoutEndpoint;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public URI getTokenEndpoint() {
        return tokenEndpoint;
    }

    @Nullable
    public URI getTokenLogoutEndpoint() {
        return tokenLogoutEndpoint;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public Secret getAdminPassword() {
        return adminPassword;
    }

    public ClientID getClientId() {
        return clientId;
    }

    public Secret getClientSecret() {
        return clientSecret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(adminPassword, adminUsername, clientId, clientSecret, tokenEndpoint, tokenLogoutEndpoint);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AdminKey other = (AdminKey) obj;
        return Objects.equals(adminPassword, other.adminPassword) && Objects.equals(adminUsername, other.adminUsername) && Objects.equals(clientId, other.clientId) && Objects.equals(clientSecret, other.clientSecret)
            && Objects.equals(tokenEndpoint, other.tokenEndpoint) && Objects.equals(tokenLogoutEndpoint, other.tokenLogoutEndpoint);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AdminKey [tokenEndpoint=");
        sb.append(tokenEndpoint);
        sb.append(", tokenLogoutEndpoint=");
        sb.append(tokenLogoutEndpoint);
        sb.append(", adminUsername=");
        sb.append(adminUsername);
        sb.append(", clientId=");
        sb.append(clientId);
        sb.append("]");
        return notNull(sb.toString());
    }

}
