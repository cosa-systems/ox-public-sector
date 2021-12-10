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

import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

public enum CustomJWTParser {
    ;

    private static final Logger LOG = N.logger(JWTParser.class);

    public static Map<String, Object> parseBody(String jwt) {
        LOG.trace("parseBody: String '{}'", jwt);
        try {
            JWT jwt2 = JWTParser.parse(jwt);
            if (null == jwt2) {
                LOG.trace("no jwt returned from parser");
                return N.notNull(Collections.emptyMap());
            }
            JWTClaimsSet jwtClaimsSet = jwt2.getJWTClaimsSet();
            if (null == jwtClaimsSet) {
                LOG.trace("no JWTClaimsSet present");
                return N.notNull(Collections.emptyMap());
            }
            Map<String, Object> map = N.notNull(jwtClaimsSet.getClaims());
            LOG.debug("parseBody return map: '{}'", map);
            return map;
        } catch (ParseException e) {
            LOG.error("not able to parse jwt '{}'", jwt, e);
        }
        return N.notNull(Collections.emptyMap());
    }

}
