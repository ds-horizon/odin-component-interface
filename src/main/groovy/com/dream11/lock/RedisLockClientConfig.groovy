package com.dream11.lock


class RedisLockClientConfig implements LockClientConfig {
    private String key;
    private String clusterEndpoint;

    RedisLockClientConfig(String key, String clusterEndpoint) {
        this.key = key;
        this.clusterEndpoint = clusterEndpoint;
    }

    String getKey() {
        return key;
    }

    String getClusterEndpoint() {
        return clusterEndpoint;
    }

    static RedisLockClientConfig fromConfigMap(Map<String, Object> config) {
        String key = (String) config.get("key");
        String clusterEndpoint = (String) config.get("clusterEndpoint");
        return new RedisLockClientConfig(key, clusterEndpoint);
    }
}
