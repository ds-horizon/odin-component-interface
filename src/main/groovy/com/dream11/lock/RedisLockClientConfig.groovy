package com.dream11.lock

import com.fasterxml.jackson.annotation.JsonProperty

class RedisLockClientConfig implements LockClientConfig {
    @JsonProperty("key")
    String key

    @JsonProperty("host")
    String host

    @JsonProperty("port")
    Integer port = 6379

    RedisLockClientConfig() {
        // Default constructor for Jackson
    }

    String getKey() {
        return key
    }

    String getHost() {
        return host
    }

    Integer getPort() {
        return port
    }
}
