
package com.openexchange.matrixproxy.servlet.data;

import javax.annotation.Generated;

/**
 * {@link MatrixIdentifier}
 */
public class MatrixIdentifier {

    private String type;
    private String user;

    @Generated("SparkTools")
    private MatrixIdentifier(Builder builder) {
        this.type = builder.type;
        this.user = builder.user;
    }

    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "MatrixIdentifier [type=" + type + ", user=" + user + "]";
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {

        private String type;
        private String user;

        private Builder() {}

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public MatrixIdentifier build() {
            return new MatrixIdentifier(this);
        }
    }

}
