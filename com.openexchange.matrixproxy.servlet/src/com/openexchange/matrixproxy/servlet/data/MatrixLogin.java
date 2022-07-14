
package com.openexchange.matrixproxy.servlet.data;

import javax.annotation.Generated;

/**
 * {@link MatrixLogin}
 */
public class MatrixLogin {

    private String           type;
    private MatrixIdentifier identifier;

    @Generated("SparkTools")
    private MatrixLogin(Builder builder) {
        this.type = builder.type;
        this.identifier = builder.identifier;
    }

    public String getType() {
        return type;
    }

    public MatrixIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "MatrixLogin [type=" + type + ", identifier=" + identifier + "]";
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {

        private String           type;
        private MatrixIdentifier identifier;

        private Builder() {}

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withIdentifier(MatrixIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public MatrixLogin build() {
            return new MatrixLogin(this);
        }
    }

}
