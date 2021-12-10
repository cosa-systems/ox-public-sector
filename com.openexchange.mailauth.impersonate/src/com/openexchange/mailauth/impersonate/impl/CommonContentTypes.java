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

import javax.mail.internet.ContentType;
import javax.mail.internet.ParameterList;

/**
 * Common content types used in the OAuth 2.0 protocol and implementing
 * applications. The character set all of content types is set to
 * {@link #DEFAULT_CHARSET UTF-8}.
 */
public final class CommonContentTypes {

    /**
     * The default character set.
     */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * The default content type parameter list.
     */
    private static final ParameterList PARAM_LIST = new ParameterList();

    /**
     * Content type {@code application/json}.
     */
    public static final ContentType APPLICATION_JSON = new ContentType("application", "json", PARAM_LIST);

    /**
     * Content type {@code application/jwt}.
     */
    public static final ContentType APPLICATION_JWT = new ContentType("application", "jwt", PARAM_LIST);

    /**
     * Content type {@code application/x-www-form-urlencoded}.
     */
    public static final ContentType APPLICATION_URLENCODED = new ContentType("application", "x-www-form-urlencoded", PARAM_LIST);

    static {
        PARAM_LIST.set("charset", DEFAULT_CHARSET);
    }
}
