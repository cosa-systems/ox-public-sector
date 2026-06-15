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

package com.openexchange.mail.scheduled.capability.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.capabilities.CapabilityChecker;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.session.Session;

/**
 * {@link Activator}
 *
 * Declares the {@code scheduled_mail} capability and grants it to every user for
 * whom the scheduled-mail feature ({@code com.openexchange.mail.scheduled.enabled})
 * is enabled.
 *
 * <p>Background: this OX build ships the scheduled-mail scheduler
 * ({@code com.openexchange.mail.scheduled.*}) and the App Suite UI's "Send later"
 * composer action (gated on {@code capabilities.has('scheduled_mail')}), but
 * nothing declares the {@code scheduled_mail} capability — so the middleware never
 * advertises it and never returns it in the per-compose capabilities, leaving the
 * UI button unable to draw/enable. This bundle closes that gap so the capability is
 * advertised globally (UI draws the button) and is part of the user's effective
 * capability set (the /mail/compose response includes it, enabling the button) —
 * without changing the deterministic-derivation or any other behaviour.</p>
 */
public class Activator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    /** The capability the App Suite composer checks for the "Send later" action. */
    private static final String CAPABILITY = "scheduled_mail";

    /** Config-cascade-aware switch that enables the scheduled-mail feature. */
    private static final String ENABLED_PROPERTY = "com.openexchange.mail.scheduled.enabled";

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { CapabilityService.class, ConfigViewFactory.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle {}", context.getBundle());

        final ConfigViewFactory configViews = getServiceSafe(ConfigViewFactory.class);

        // Grant scheduled_mail to sessions where the scheduled-mail feature is enabled.
        Dictionary<String, Object> properties = new Hashtable<>(1);
        properties.put(CapabilityChecker.PROPERTY_CAPABILITIES, CAPABILITY);
        registerService(CapabilityChecker.class, new CapabilityChecker() {

            @Override
            public boolean isEnabled(String capability, Session session) throws OXException {
                if (!CAPABILITY.equals(capability)) {
                    // Not our capability; do not influence its evaluation.
                    return true;
                }
                if (session == null) {
                    return false;
                }
                ConfigView view = configViews.getView(session.getUserId(), session.getContextId());
                return view.opt(ENABLED_PROPERTY, Boolean.class, Boolean.FALSE).booleanValue();
            }
        }, properties);

        // Declare the capability so the platform treats it as known and advertises it.
        getServiceSafe(CapabilityService.class).declareCapability(CAPABILITY);

        LOG.info("Declared capability '{}' (granted when {} is true)", CAPABILITY, ENABLED_PROPERTY);
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle {}", context.getBundle());
        super.stopBundle();
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }
}
