package com.dream11.lock

class RedisLockClientConfig implements LockClientConfig {
    private String key
    private String host
    private Integer port = 6379

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
