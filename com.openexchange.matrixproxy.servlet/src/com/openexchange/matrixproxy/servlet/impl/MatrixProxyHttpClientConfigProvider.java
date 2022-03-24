
package com.openexchange.matrixproxy.servlet.impl;

import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;


/**
 * {@link MatrixProxyHttpClientConfigProvider}
 */
@NonNullByDefault
public class MatrixProxyHttpClientConfigProvider extends DefaultHttpClientConfigProvider {
    public static final String CLIENTID = "matrixproxy";

    /**
     * Initializes a new {@link MatrixProxyHttpClientConfigProvider}.
     */
    public MatrixProxyHttpClientConfigProvider() {
        super(CLIENTID, "Open-Xchange Matrix Proxy HTTP Client");
    }
}
