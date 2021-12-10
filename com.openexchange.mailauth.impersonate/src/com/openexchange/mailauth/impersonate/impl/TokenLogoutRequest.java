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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.nimbusds.oauth2.sdk.AbstractOptionallyIdentifiedRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.openexchange.java.Strings;

/**
 * Token revocation request. Used to logout issued refresh token.
 *
 * <p>Example token revocation request for a confidential client:
 *
 * <pre>
 * POST /logout HTTP/1.1
 * Host: server.example.com
 * Content-Type: application/x-www-form-urlencoded
 * Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
 *
 * refresh_token=45ghiukldjahdnhzdauz
 * </pre>
 * 
 */
public final class TokenLogoutRequest extends AbstractOptionallyIdentifiedRequest {

    /**
     * The token to revoke.
     */
    private final RefreshToken token;

    /**
     * Creates a new token logout request for a confidential client.
     *
     * @param uri The URI of the token revocation endpoint. May be
     *            {@code null} if the {@link #toHTTPRequest} method
     *            will not be used.
     * @param clientAuth The client authentication. Must not be
     *            {@code null}.
     * @param token The refresh token to revoke. Must not be
     *            {@code null}.
     */
    public TokenLogoutRequest(
        final URI uri,
            final ClientAuthentication clientAuth,
            final RefreshToken token) {

        super(uri, clientAuth);

        if (clientAuth == null) {
            throw new IllegalArgumentException("The client authentication must not be null");
        }

        if (token == null)
            throw new IllegalArgumentException("The token must not be null");

        this.token = token;
    }

    /**
     * Creates a new token logout request for a public client.
     *
     * @param uri The URI of the token revocation endpoint. May be
     *            {@code null} if the {@link #toHTTPRequest} method
     *            will not be used.
     * @param clientID The client ID. Must not be {@code null}.
     * @param token The refresh token to revoke. Must not be
     *            {@code null}.
     */
    public TokenLogoutRequest(
        final URI uri,
            final ClientID clientID,
            final RefreshToken token) {

        super(uri, clientID);

        if (clientID == null) {
            throw new IllegalArgumentException("The client ID must not be null");
        }

        if (token == null)
            throw new IllegalArgumentException("The token must not be null");

        this.token = token;
    }

    @Override
    public HTTPRequest toHTTPRequest() {

        if (getEndpointURI() == null)
            throw new SerializeException("The endpoint URI is not specified");

        URL url;

        try {
            url = getEndpointURI().toURL();

        } catch (MalformedURLException e) {

            throw new SerializeException(e.getMessage(), e);
        }

        HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.POST, url);
        httpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);

        Map<String, String> params = new HashMap<>();

        if (getClientID() != null) {
            // public client
            params.put("client_id", getClientID().getValue());
        }

        params.put("refresh_token", token.getValue());

        httpRequest.setQuery(URLUtils.serializeParameters(params));

        if (getClientAuthentication() != null) {
            // confidential client
            getClientAuthentication().applyTo(httpRequest);
        }

        return httpRequest;
    }

    /**
     * Parses a token revocation request from the specified HTTP request.
     *
     * @param httpRequest The HTTP request. Must not be {@code null}.
     *
     * @return The token revocation request.
     *
     * @throws ParseException If the HTTP request couldn't be parsed to a
     *             token revocation request.
     */
    public static TokenLogoutRequest parse(final HTTPRequest httpRequest) throws ParseException {

        // Only HTTP POST accepted
        httpRequest.ensureMethod(HTTPRequest.Method.POST);
        httpRequest.ensureContentType(CommonContentTypes.APPLICATION_URLENCODED);

        Map<String, String> params = httpRequest.getQueryParameters();

        final String tokenValue = params.get("refresh_token");

        if (tokenValue == null || tokenValue.isEmpty()) {
            throw new ParseException("Missing required token parameter");
        }

        // Detect the token type
        RefreshToken token = new RefreshToken(tokenValue);

        URI uri;

        try {
            uri = httpRequest.getURL().toURI();

        } catch (URISyntaxException e) {

            throw new ParseException(e.getMessage(), e);
        }

        // Parse client auth
        ClientAuthentication clientAuth = ClientAuthentication.parse(httpRequest);

        if (clientAuth != null) {
            return new TokenLogoutRequest(uri, clientAuth, token);
        }

        // Public client
        final String clientIDString = params.get("client_id");

        if (Strings.isEmpty(clientIDString)) {
            throw new ParseException("Invalid token revocation request: No client authentication or client_id parameter found");
        }

        return new TokenLogoutRequest(uri, new ClientID(clientIDString), token);
    }
}
