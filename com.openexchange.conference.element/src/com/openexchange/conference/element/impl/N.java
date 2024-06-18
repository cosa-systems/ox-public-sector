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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.session.Session;

/**
 * {@link N}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public enum N {
    ;

    public static <X> X notNull(final @Nullable X x) throws IllegalArgumentException {
        if (x == null) {
            throw new IllegalArgumentException("unexpected null value");
        }
        return x;
    }

    public static String f(final String f, Object... args) {
        return notNull(String.format(f, args));
    }

    /**
     * Get an SLF4J {@link Logger} without having to ignore the null warning.
     * <p>
     * From its implementation, it is known that {@link LoggerFactory} does not
     * return null values.
     * 
     * @param c the {@link Class} for which to get a {@link Logger}
     * @return a {@link Logger} for the specified {@link Class}
     */
    @SuppressWarnings("null")
    public static Logger logger(final Class<?> c) {
        return LoggerFactory.getLogger(c);
    }

    /**
     * Retrieve OAuth Token from session
     * 
     * @param session
     * @return the token or null if not found
     */
    public static @Nullable String getOAuthTokenFromSession(Session session) {
        if (session.containsParameter(Session.PARAM_OAUTH_ACCESS_TOKEN)) {
            return (String) notNull(session.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN));
        }
        return null;
    }
}
