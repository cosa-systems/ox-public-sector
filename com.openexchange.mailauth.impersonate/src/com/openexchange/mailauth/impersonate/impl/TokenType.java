
package com.openexchange.mailauth.impersonate.impl;

import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;

@NonNullByDefault
public enum TokenType {

    ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token");

    public static final String BASE = "urn:ietf:params:oauth:token-type:";

    private final String identifier;

    private TokenType(String identifier) {
        this.identifier = BASE + identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static final @Nullable TokenType parseToken(@Nullable String input) {
        if (null == input) {
            return null;
        }
        for (TokenType tokenType : TokenType.values()) {
            if (tokenType.name().equalsIgnoreCase(input)) {
                return tokenType;
            }
        }
        return null;
        
    }
}
