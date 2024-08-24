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

    private boolean enabled = false; // Global toggle for the Rmq4j module. Set to 'true' to enable the module.
    private boolean debugging = false; // Enable debugging mode for detailed logs and error traces. Set to 'true' for more verbose logging.
    private String defaultCluster; // default_cluster, The name of the default cluster that the application will connect to if not specified otherwise.
    private Map<String, Connection> clusters = new HashMap<>(); // clusters
    private List<Config> configs = new ArrayList<>(); // configs. Define specific configurations for exchanges, queues, and bindings within the clusters.

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

        private boolean enabled = false; // Toggle to enable or disable this specific cluster configuration.
        private String host; // Hostname or IP address of the RabbitMQ server.
        private int port; // The port on which the RabbitMQ server is listening.
        private String username; // Username for authenticating with the RabbitMQ server.
        private String password; // Password for authenticating with the RabbitMQ server.
        private String virtualHost; // virtual_host, Virtual host to be used within the RabbitMQ server.
        private boolean useSSL = false; // use_ssl, Boolean flag to indicate if SSL should be used for secure connections.
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

        private String clusterKey; // cluster_key, Reference to the cluster this configuration applies to.
        private String dispatchKey; // dispatch_key, Unique dispatch key to identify this particular configuration.
        private String desc; // Description of this configuration's purpose.
        private boolean enabled = false; // Toggle to enable or disable this specific configuration.
        private boolean debugging = false; // Enable debugging mode for detailed logs within this configuration.
        private Exchange exchange = new Exchange(); // exchange, Exchange settings for publishing messages.
        private Queue queue = new Queue(); // queue, Queue settings for consuming messages.
        private Bind bind = new Bind(); // bind, Binding settings to link the exchange and the queue.

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

        private String name; // Name of the exchange to use.
        private String kind; // Type of exchange: can be 'fanout', 'topic', 'headers', or 'direct'.
        private boolean durable = false; // If true, the exchange will survive server restarts.
        private boolean clearable = false; // If true, the exchange will be deleted once no longer used.
        private boolean internal = false; // If true, the exchange is for internal use only, not for publishing from the client.
        private boolean delayable = false; // If true, the exchange supports delayed message delivery.
        private Map<String, Object> arguments = new HashMap<>(); // arguments, Custom arguments for the exchange as key-value. This can include settings like retry count.

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

        private String name; // Name of the queue to bind to the exchange.
        private boolean durable = false; // If true, the queue will survive server restarts.
        private boolean exclusive = false; // If true, the queue is exclusive to this connection and will be deleted once the connection closes.
        private boolean clearable = false; // If true, the queue will be deleted once no longer used.
        private Map<String, Object> arguments = new HashMap<>(); // arguments, Custom arguments for the exchange as key-value. This can include settings like retry count.

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

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }

    public static class Bind {
        public Bind() {
            super();
        }

        private String routingKey; // routing_key, Routing key for the binding. Used to route messages to the correct queue.
        private Map<String, Object> arguments = new HashMap<>(); // arguments, Custom arguments for the exchange as key-value. This can include settings like retry count.

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }
}
