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

package com.openexchange.mailauth.impersonate.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.config.lean.DefaultProperty;

/**
 * Helper class used in the minimal API
 * {@link N}
 *
 * @author <a href="mailto:felix.marx@open-xchange.com">Felix Marx</a>
 */
@NonNullByDefault
public enum N {
    ;

    @SuppressWarnings("null")
    public static Logger logger(final Class<?> c) {
        return LoggerFactory.getLogger(c);
    }

    public static <X> X notNull(final @Nullable X x) throws IllegalArgumentException {
        if (x == null) {
            throw new IllegalArgumentException("unexpected null value");
        }
        return x;
    }

    public static DefaultProperty defaultProp(String fqdn, Object defaultValue) {
        return notNull(DefaultProperty.valueOf(fqdn, defaultValue));
    }

    /**
     * Perform a {@code String.format()} without having to null-check the result.
     * <p>
     * {@link String#format} is known for never returning {@code null} yet Eclipse doesn't know
     * about that and in order to avoid having to null-check the result of that frequently used
     * method, use this one instead.
     * 
     * @param format
     *            A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param args
     *            Arguments referenced by the format specifiers in the format
     *            string. If there are more arguments than format specifiers, the
     *            extra arguments are ignored. The number of arguments is
     *            variable and may be zero. The maximum number of arguments is
     *            limited by the maximum dimension of a Java array as defined by
     *            <cite>The Java&trade; Virtual Machine Specification</cite>.
     *            The behaviour on a
     *            {@code null} argument depends on the <a
     *            href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws java.util.IllegalFormatException
     *             If a format string contains an illegal syntax, a format
     *             specifier that is incompatible with the given arguments,
     *             insufficient arguments given the format string, or other
     *             illegal conditions. For specification of all possible
     *             formatting errors, see the <a
     *             href="../util/Formatter.html#detail">Details</a> section of the
     *             formatter class specification.
     * @return A formatted string
     */
    public static String f(final String format, final @Nullable Object... args) {
        final String result = String.format(format, args);
        if (result == null) {
            throw new IllegalStateException("result of String.format is null");
        }
        return result;
    }
}
