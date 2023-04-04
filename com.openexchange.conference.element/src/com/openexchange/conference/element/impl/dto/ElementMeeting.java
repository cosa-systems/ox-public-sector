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

import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openexchange.annotation.NonNullByDefault;
import javax.annotation.Generated;

/**
 * {@link ElementMeeting}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class ElementMeeting {

    @JsonProperty
    private String        description;
    @JsonProperty("enable_auto_deletion")
    private boolean       enableAutoDeletion;
    @JsonProperty("end_time")
    private ZonedDateTime endTime;
    @JsonProperty("start_time")
    private ZonedDateTime startTime;
    @JsonProperty("target_room_id")
    private String        targetRoomId;
    @JsonProperty
    private String        title;
    @JsonProperty("external_data")
    private ExternalData  externalData;

    @SuppressWarnings("null")
    public ElementMeeting() {
        super();
    }

    @Generated("SparkTools")
    private ElementMeeting(Builder builder) {
        this.description = builder.description;
        this.enableAutoDeletion = builder.enableAutoDeletion;
        this.endTime = builder.endTime;
        this.startTime = builder.startTime;
        this.targetRoomId = builder.targetRoomId;
        this.title = builder.title;
        this.externalData = builder.externalData;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnableAutoDeletion() {
        return enableAutoDeletion;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public String getTargetRoomId() {
        return targetRoomId;
    }

    public String getTitle() {
        return title;
    }

    public ExternalData getExternalData() {
        return externalData;
    }

    @Override
    public String toString() {
        return "ElementMeeting [description=" + description + ", enableAutoDeletion=" + enableAutoDeletion + ", endTime=" + endTime + ", startTime=" + startTime + ", targetRoomId=" + targetRoomId + ", title=" + title + ", externalData=" +
            externalData + "]";
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {

        private String        description;
        private boolean       enableAutoDeletion;
        private ZonedDateTime endTime;
        private ZonedDateTime startTime;
        private String        targetRoomId;
        private String        title;
        private ExternalData  externalData;

        @SuppressWarnings("null")
        private Builder() {}

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withEnableAutoDeletion(boolean enableAutoDeletion) {
            this.enableAutoDeletion = enableAutoDeletion;
            return this;
        }

        public Builder withEndTime(ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withStartTime(ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withTargetRoomId(String targetRoomId) {
            this.targetRoomId = targetRoomId;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withExternalData(ExternalData externalData) {
            this.externalData = externalData;
            return this;
        }

        public ElementMeeting build() {
            return new ElementMeeting(this);
        }
    }

}
