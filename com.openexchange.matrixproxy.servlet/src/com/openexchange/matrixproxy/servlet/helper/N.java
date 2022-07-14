
package com.openexchange.matrixproxy.servlet.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;

/**
 * {@link N}
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

    public static String f(final String format, final @Nullable Object... args) {
        final String result = String.format(format, args);
        if (result == null) {
            throw new IllegalStateException("result of String.format is null");
        }
        return result;
    }

}
