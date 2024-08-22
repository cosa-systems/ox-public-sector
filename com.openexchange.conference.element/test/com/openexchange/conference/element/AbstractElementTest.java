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

package com.openexchange.conference.element;

import static com.openexchange.conference.element.impl.N.f;
import static com.openexchange.conference.element.impl.N.logger;
import static com.openexchange.conference.element.impl.N.notNull;
import static java.lang.String.format;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.rest.client.httpclient.SimHttpClientService;

/**
 * {@link AbstractElementTest}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public abstract class AbstractElementTest {

    protected static final Logger LOG = logger(AbstractElementTest.class);

    protected static final int    MOCK_HTTP_PORT = 3333;
    protected static final String TOKEN          = "verysecrettoken";
    @SuppressWarnings("null")
    protected static final String AUTH_HEADER    = format("Bearer %s", TOKEN);
    @SuppressWarnings("null")
    protected static final String MOCK_HTTP_URL  = format("http://localhost:%d", MOCK_HTTP_PORT);

    static {
        // Override Timezone to ensure all Tests are being executed in Europe/Berlin Timezone since that's where
        // MeetingUpdate.json has been generated
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
    }

    abstract int getMockPort();

    protected String getMockHttpUrl() {
        return f("http://localhost:%d", getMockPort());
    }

    protected String loadFile(final String name) throws FileNotFoundException, IOException {
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(new File("test-resources", name))))) {
            return notNull(r.lines().collect(Collectors.joining("\n")));
        }
    }

    protected ManagedHttpClient getHttpClient() {
        final SimHttpClientService shcs = new SimHttpClientService();
        return shcs.getHttpClient("TESTCLIENT");
    }

}
