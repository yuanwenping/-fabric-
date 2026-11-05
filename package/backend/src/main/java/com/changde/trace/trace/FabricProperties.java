package com.changde.trace.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "fabric")
public class FabricProperties {
    private boolean enabled = true;
    private String channel;
    private String chaincode;
    private String endpointOrderer;
    private String ordererAuthority;
    private Resource ordererTls;
    private Map<String, Organization> organizations = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChaincode() {
        return chaincode;
    }

    public void setChaincode(String chaincode) {
        this.chaincode = chaincode;
    }

    public String getEndpointOrderer() {
        return endpointOrderer;
    }

    public void setEndpointOrderer(String endpointOrderer) {
        this.endpointOrderer = endpointOrderer;
    }

    public String getOrdererAuthority() {
        return ordererAuthority;
    }

    public void setOrdererAuthority(String ordererAuthority) {
        this.ordererAuthority = ordererAuthority;
    }

    public Resource getOrdererTls() {
        return ordererTls;
    }

    public void setOrdererTls(Resource ordererTls) {
        this.ordererTls = ordererTls;
    }

    public Map<String, Organization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Map<String, Organization> organizations) {
        this.organizations = organizations;
    }

    public static class Organization {
        private String endpoint;
        private String authority;
        private Resource cert;
        private Resource key;
        private Resource tls;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public Resource getCert() {
            return cert;
        }

        public void setCert(Resource cert) {
            this.cert = cert;
        }

        public Resource getKey() {
            return key;
        }

        public void setKey(Resource key) {
            this.key = key;
        }

        public Resource getTls() {
            return tls;
        }

        public void setTls(Resource tls) {
            this.tls = tls;
        }
    }
}
