package org.rmq4j.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring.rmq4j")
public class Rmq4jProperties implements Serializable {
    public Rmq4jProperties() {
        super();
    }

    private boolean enabled = false;
    private boolean debugging = false;
    private String defaultCluster; // default_cluster
    private Map<String, Connection> clusters = new HashMap<>(); // clusters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public String getDefaultCluster() {
        return defaultCluster;
    }

    public void setDefaultCluster(String defaultCluster) {
        this.defaultCluster = defaultCluster;
    }

    public Map<String, Connection> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, Connection> clusters) {
        this.clusters = clusters;
    }

    public static class Connection {
        public Connection() {
            super();
        }

        private boolean enabled = false;
        private String host;
        private int port;
        private String username;
        private String password;
        private String virtualHost; // virtual_host
        private boolean useSSL = false; // use_ssl
        private String desc;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getVirtualHost() {
            return virtualHost;
        }

        public void setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
        }

        public boolean isUseSSL() {
            return useSSL;
        }

        public void setUseSSL(boolean useSSL) {
            this.useSSL = useSSL;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }
}
