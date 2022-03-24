
package com.openexchange.matrixproxy.servlet.impl;

import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.config.lean.Property;

/**
 * {@link MatrixProxyProperties}
 */
@NonNullByDefault
public enum MatrixProxyProperties implements Property {
    serverUrl("https://matrix.dpx-wp7a.at-univention.de"),
    botUrl("https://meetings-widget-api.dpx-wp7a.at-univention.de"),
    asToken(""),
    apiWhitelist("/v1/meeting/create")
    ;

    public static final String MATRIXPROXY_CONFIGFILE = "matrixproxy-servlet.properties";

    /**
     * The default value of the property.
     */
    private final Object defaultValue;

    /**
     * The prefix of the propeties
     */
    private static final String PREFIX = "com.openexchange.matrixproxy.servlet.";

    private MatrixProxyProperties(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
