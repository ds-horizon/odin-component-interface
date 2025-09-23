package com.dream11.lock

class RedisLockClientConfig implements LockClientConfig {
    String key
    String host
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
