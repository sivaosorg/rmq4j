package org.rmq4j.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private List<Config> configs = new ArrayList<>(); // configs

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

    public List<Config> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Config> configs) {
        this.configs = configs;
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

    public static class Config {
        public Config() {
            super();
        }

        private String clusterKey; // cluster_key
        private String dispatchKey; // dispatch_key
        private String desc;
        private boolean enabled = false;
        private boolean debugging = false;
        private Exchange exchange = new Exchange(); // exchange
        private Queue queue = new Queue(); // queue
        private Bind bind = new Bind(); // bind

        public String getClusterKey() {
            return clusterKey;
        }

        public void setClusterKey(String clusterKey) {
            this.clusterKey = clusterKey;
        }

        public String getDispatchKey() {
            return dispatchKey;
        }

        public void setDispatchKey(String dispatchKey) {
            this.dispatchKey = dispatchKey;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

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

        public Exchange getExchange() {
            return exchange;
        }

        public void setExchange(Exchange exchange) {
            this.exchange = exchange;
        }

        public Queue getQueue() {
            return queue;
        }

        public void setQueue(Queue queue) {
            this.queue = queue;
        }

        public Bind getBind() {
            return bind;
        }

        public void setBind(Bind bind) {
            this.bind = bind;
        }
    }

    @SuppressWarnings({"SpellCheckingInspection"})
    public static class Exchange {
        public Exchange() {
            super();
        }

        private String name;
        private String kind;
        private boolean durable = false;
        private boolean clearable = false;
        private boolean internal = false;
        private boolean delayable = false;
        private Map<String, Object> arguments = new HashMap<>(); // arguments

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public boolean isDurable() {
            return durable;
        }

        public void setDurable(boolean durable) {
            this.durable = durable;
        }

        public boolean isClearable() {
            return clearable;
        }

        public void setClearable(boolean clearable) {
            this.clearable = clearable;
        }

        public boolean isInternal() {
            return internal;
        }

        public void setInternal(boolean internal) {
            this.internal = internal;
        }

        public boolean isDelayable() {
            return delayable;
        }

        public void setDelayable(boolean delayable) {
            this.delayable = delayable;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }

    @SuppressWarnings({"SpellCheckingInspection"})
    public static class Queue {
        public Queue() {
            super();
        }

        private String name;
        private boolean durable = false;
        private boolean exclusive = false;
        private boolean clearable = false;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isDurable() {
            return durable;
        }

        public void setDurable(boolean durable) {
            this.durable = durable;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public void setExclusive(boolean exclusive) {
            this.exclusive = exclusive;
        }

        public boolean isClearable() {
            return clearable;
        }

        public void setClearable(boolean clearable) {
            this.clearable = clearable;
        }
    }

    public static class Bind {
        public Bind() {
            super();
        }

        private String routingKey; // routing_key

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }
}
