
package com.openexchange.mailauth.impersonate.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.annotation.NonNull;
import com.openexchange.mailauth.impersonate.helper.N;

public class ConfigHelper {

    public static final String BASE      = "com.openexchange.mailauth.impersonate.";
    public static final String ADMINBASE = BASE + "admin.";

    private Config enabled               = new DefaultConfig("enabled");
    private Config clientId              = new DefaultConfig("clientId");
    private Config clientSecret          = new DefaultConfig("clientSecret");
    private Config authType              = new DefaultConfig("authType");
    private Config tokenType             = new DefaultConfig("tokenType");
    private Config accessTokenLoginClaim = new DefaultConfig("accessTokenLoginClaim");
    private Config adminEnabled          = new AdminConfig("enabled");
    private Config adminUsername         = new AdminConfig("username");
    private Config adminPassword         = new AdminConfig("password");
    private Config tokenEndpoint         = new DefaultConfig("tokenEndpoint");
    private Config tokenLogoutEndpoint   = new DefaultConfig("tokenLogoutEndpoint");
    private Config refershTtl            = new DefaultConfig("refreshTtl");

    private final Set<Config> allEntries = new HashSet<>();
    {
        allEntries.add(enabled);
        allEntries.add(clientId);
        allEntries.add(clientSecret);
        allEntries.add(authType);
        allEntries.add(tokenType);
        allEntries.add(accessTokenLoginClaim);
        allEntries.add(adminEnabled);
        allEntries.add(adminUsername);
        allEntries.add(adminPassword);
        allEntries.add(tokenEndpoint);
        allEntries.add(tokenLogoutEndpoint);
        allEntries.add(refershTtl);
    }

    static ConfigHelper builder() {
        return new ConfigHelper();
    }

    ConfigHelper setEnabled(String value) {
        enabled.setValue(value);
        return this;
    }

    ConfigHelper setClientId(String value) {
        clientId.setValue(value);
        return this;
    }

    ConfigHelper setClientSecret(String value) {
        clientSecret.setValue(value);
        return this;
    }

    ConfigHelper setAuthType(String value) {
        authType.setValue(value);
        return this;
    }

    ConfigHelper setTokenType(String value) {
        tokenType.setValue(value);
        return this;
    }

    ConfigHelper setAccessTokenLoginClaim(String value) {
        accessTokenLoginClaim.setValue(value);
        return this;
    }

    ConfigHelper setAdminEnabled(String value) {
        adminEnabled.setValue(value);
        return this;
    }

    ConfigHelper setAdminUsername(String value) {
        adminUsername.setValue(value);
        return this;
    }

    ConfigHelper setAdminPassword(String value) {
        adminPassword.setValue(value);
        return this;
    }

    ConfigHelper setTokenEndpoint(String value) {
        tokenEndpoint.setValue(value);
        return this;
    }

    ConfigHelper setTokenLogoutEndpoint(String value) {
        tokenLogoutEndpoint.setValue(value);
        return this;
    }

    ConfigHelper setRefreshTtl(String value) {
        refershTtl.setValue(value);
        return this;
    }

    public abstract class Config {

        private final String fqdn;
        private String       value;

        public Config(String fqdn) {
            super();
            this.fqdn = fqdn;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getFqdn() {
            return fqdn;
        }
    }

    public class DefaultConfig extends Config {

        public DefaultConfig(String fqdn) {
            super(BASE + fqdn);
        }

    }

    public class AdminConfig extends Config {

        public AdminConfig(String fqdn) {
            super(ADMINBASE + fqdn);
        }

    }

    @NonNull
    public Object[] getAllValuesAsStringAray() {
        List<Object> returnList = new ArrayList<>();
        for (Config conf : allEntries) {
            if (null != conf.getValue()) {
                returnList.add(conf.getFqdn());
                returnList.add(conf.getValue());
            }
        }
        return N.notNull(returnList.stream().toArray(Object[]::new));
    }
}
