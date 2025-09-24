package com.dream11.lock


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

class LockConfig {
    String provider

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "provider")
    @JsonSubTypes([@JsonSubTypes.Type(value = RedisLockClientConfig.class, name = "redis")])
    LockClientConfig config

    LockConfig() {
        // Default constructor for Jackson
    }

    LockConfig(String provider, LockClientConfig config) {
        this.provider = provider
        this.config = config
    }

    String getProvider() {
        return provider
    }

    LockClientConfig getConfig() {
        return config
    }
}
