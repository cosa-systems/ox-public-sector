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

package com.openexchange.conference.element.osgi;

import static com.openexchange.conference.element.impl.N.logger;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.conference.element.impl.ElementConferenceHandler;
import com.openexchange.config.ConfigurationService;
import com.openexchange.conversion.ConversionService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;

/**
 * 
 * {@link Activator}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class Activator extends HousekeepingActivator {

    private static final Logger LOG = logger(Activator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            HttpClientService.class,
            ConversionService.class,
            ConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("starting bundle {}", context.getBundle());
        try {
            registerService(CalendarHandler.class, 
                new ElementConferenceHandler(
                    getServiceSafe(HttpClientService.class),
                    getServiceSafe(ConfigurationService.class)));
        } catch (Exception e) {
            LOG.error("error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

}
