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

package com.openexchange.mailauth.impersonate.osgi;

import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.mail.MailAuthenticator;
import com.openexchange.mailauth.impersonate.helper.N;
import com.openexchange.mailauth.impersonate.impl.ImpersonationMailAuthenticator;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;

@NonNullByDefault
/**
 * 
 * {@link ImpersonationMailActivator}
 *
 * @author <a href="mailto:felix.marx@open-xchange.com">Felix Marx</a>
 */
public class ImpersonationMailActivator extends HousekeepingActivator {

    private static final Logger LOG = N.logger(ImpersonationMailActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] {
            LeanConfigurationService.class,
            HttpClientService.class,
            SessionOAuthTokenService.class,
            ThreadPoolService.class,
            TimerService.class,
        };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle: com.openexchange.mailauth.impersonate");
            MailAuthenticator authenticator = new ImpersonationMailAuthenticator(
                getServiceSafe(LeanConfigurationService.class),
                getServiceSafe(HttpClientService.class),
                getServiceSafe(SessionOAuthTokenService.class),
                getServiceSafe(ThreadPoolService.class),
                getServiceSafe(TimerService.class));
            registerService(MailAuthenticator.class, authenticator);
            LOG.info("Complete starting bundle: com.openexchange.mailauth.impersonate");
        } catch (Exception e) {
            LOG.error("Error while starting bundle com.openexchange.mailauth.impersonate", e);
            throw e;
        }
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

}
