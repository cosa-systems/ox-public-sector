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

package com.openexchange.mailauth.impersonate.impl;

import java.util.Objects;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.mailauth.impersonate.helper.N;
import com.openexchange.session.Session;
import com.openexchange.session.UserAndContext;
import static com.openexchange.mailauth.impersonate.helper.N.notNull;

@NonNullByDefault
public class UserKey {

    private final String         loginName;
    private final UserAndContext userAndContext;
    private final Session        session;
    private final String         primaryMail;
    @Nullable
    private final String         sessiondId;

    public UserKey(Session session, String loginName, String primaryMail) {
        super();
        this.session = session;
        this.primaryMail = primaryMail;
        this.loginName = loginName;
        this.userAndContext = notNull(UserAndContext.newInstance(session));
        this.sessiondId = session.getSessionID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(loginName, primaryMail, sessiondId, userAndContext);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserKey other = (UserKey) obj;
        return Objects.equals(loginName, other.loginName) && Objects.equals(primaryMail, other.primaryMail) && Objects.equals(sessiondId, other.sessiondId) && Objects.equals(userAndContext, other.userAndContext);
    }

    public String getLoginName() {
        return loginName;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserKey [loginName=");
        sb.append(loginName);
        sb.append(", userAndContext=");
        sb.append(userAndContext);
        sb.append(", primaryMail=");
        sb.append(primaryMail);
        sb.append("]");
        return N.notNull(sb.toString());
    }

}
