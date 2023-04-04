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

package com.openexchange.conference.element.impl.dto;

import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.openexchange.annotation.NonNullByDefault;

/**
 * {@link OXCalReference}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@JsonRootName("io.ox")
@NonNullByDefault
public class OXCalReference {

    @JsonProperty
    private String       folder;
    @JsonProperty
    private String       id;
    @JsonProperty
    private List<Object> rrules;

    @Generated("SparkTools")
    private OXCalReference(Builder builder) {
        this.folder = builder.folder;
        this.id = builder.id;
        this.rrules = builder.rrules;
    }

    @SuppressWarnings("null")
    public OXCalReference() {
        super();
    }

    public String getFolder() {
        return folder;
    }

    public String getId() {
        return id;
    }

    public List<Object> getRrules() {
        return rrules;
    }

    @Override
    public String toString() {
        return "OXCalReference [folder=" + folder + ", id=" + id + ", rrules=" + rrules + "]";
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {

        private String       folder;
        private String       id;
        @SuppressWarnings("null")
        private List<Object> rrules = Collections.emptyList();

        @SuppressWarnings("null")
        private Builder() {}

        public Builder withFolder(String folder) {
            this.folder = folder;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withRrules(List<Object> rrules) {
            this.rrules = rrules;
            return this;
        }

        public OXCalReference build() {
            return new OXCalReference(this);
        }
    }

}
