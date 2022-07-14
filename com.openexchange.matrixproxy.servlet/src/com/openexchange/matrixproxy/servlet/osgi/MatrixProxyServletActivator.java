
package com.openexchange.matrixproxy.servlet.osgi;

import static com.openexchange.matrixproxy.servlet.helper.N.logger;
import java.util.concurrent.atomic.AtomicReference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.matrixproxy.servlet.impl.MatrixProxyHttpClientConfigProvider;
import com.openexchange.matrixproxy.servlet.impl.MatrixProxyServlet;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.SpecificHttpClientConfigProvider;

/**
 * {@link MatrixProxyServletActivator}
 */
@NonNullByDefault
public final class MatrixProxyServletActivator extends HousekeepingActivator {

    private static final Logger LOG = logger(MatrixProxyServletActivator.class);

    private final AtomicReference<String> aliasRef = new AtomicReference<String>(null);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            HttpService.class,
            HttpClientService.class,
            LeanConfigurationService.class };
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected void startBundle() throws Exception {
        HttpService httpService = dependency(HttpService.class);

        MatrixProxyServlet servlet = new MatrixProxyServlet(
            dependency(HttpClientService.class),
            dependency(LeanConfigurationService.class));

        if (false == this.aliasRef.compareAndSet(null, MatrixProxyServlet.PATH)) {
            LOG.error("Servlet is already registered under alias '{}'", MatrixProxyServlet.PATH);
            return;
        }

        registerService(SpecificHttpClientConfigProvider.class, new MatrixProxyHttpClientConfigProvider());
        httpService.registerServlet(MatrixProxyServlet.PATH, servlet, null, null);
        registerService(Reloadable.class, servlet);

        LOG.info("Bundle {} started", this.context.getBundle().getSymbolicName());
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            HttpService httpService = getService(HttpService.class);
            if (null == httpService) {
                return;
            }
            String alias = aliasRef.getAndSet(null);
            if (null != alias) {
                httpService.unregister(alias);
            }
        } finally {
            super.stopBundle();
        }
    }

    private final <T> T dependency(final Class<T> serviceClass) {
        T instance = getService(serviceClass);
        if (instance == null) {
            throw new IllegalStateException("Dependency is missing: " + serviceClass.getName());
        }
        return instance;
    }
}
