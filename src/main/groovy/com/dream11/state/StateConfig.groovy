package com.dream11.state


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

class StateConfig {
    private String provider

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "provider")
    @JsonSubTypes([@JsonSubTypes.Type(value = S3StateClientConfig.class, name = "S3")])
    StateClientConfig config

    String getProvider() {
        return provider
    }

    StateClientConfig getConfig() {
        return config
    }
}
