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

import java.util.LinkedHashMap;
import java.util.Map;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.openexchange.annotation.Nullable;

/**
 * Token Exxchange grant.
 * 
 */
public class TokenExchangeGrant extends AuthorizationGrant {

    /**
     * The grant type.
     */
    public static final GrantType GRANT_TYPE = new GrantType("urn:ietf:params:oauth:grant-type:token-exchange");

    private final Secret subjectToken;

    private final String requestedSubject;

    @Nullable
    private final TokenType requestedTokenType;

    public TokenExchangeGrant(
        final String subjectToken,
            final String requestedSubject,
            @Nullable final TokenType requestedTokenType) {

        super(GRANT_TYPE);

        if (subjectToken == null) {
            throw new IllegalArgumentException("The subjectToken must not be null");
        }

        this.subjectToken = new Secret(subjectToken);

        if (requestedSubject == null) {
            throw new IllegalArgumentException("The requestedSubject must not be null");
        }

        this.requestedSubject = requestedSubject;
        this.requestedTokenType = requestedTokenType;
    }

    @Override
    public Map<String, String> toParameters() {

        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", GRANT_TYPE.getValue());
        params.put("subject_token", subjectToken.getValue());
        params.put("requested_subject", requestedSubject);
        if (null != requestedTokenType) {
            params.put("requested_token_type", requestedTokenType.getIdentifier());
        }
        return params;
    }

}
