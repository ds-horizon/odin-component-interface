package com.dream11.lock


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

class LockConfig {
    private String provider

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "provider")
    @JsonSubTypes([@JsonSubTypes.Type(value = RedisLockClientConfig.class, name = "redis")])
    LockClientConfig config

    String getProvider() {
        return provider
    }

    LockClientConfig getConfig() {
        return config
    }
}
