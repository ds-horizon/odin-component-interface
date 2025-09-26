package com.dream11.lock


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class LockConfig {
    @NotBlank(message = "Lock provider cannot be blank")
    private String provider


    @NotNull
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "provider")
    @JsonSubTypes([@JsonSubTypes.Type(value = RedisLockClientConfig.class, name = "redis")])
    @Valid
    LockClientConfig config

    String getProvider() {
        return provider
    }

    LockClientConfig getConfig() {
        return config
    }
}
