package com.dream11.state

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

class StateConfig {
    @JsonProperty("provider")
    String provider

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "provider")
    @JsonSubTypes([@JsonSubTypes.Type(value = S3StateClientConfig.class, name = "S3")])
    @JsonProperty("config")
    StateClientConfig config

    StateConfig() {
        // Default constructor for Jackson
    }

    StateConfig(String provider, StateClientConfig config) {
        this.provider = provider
        this.config = config
    }

    String getProvider() {
        return provider
    }

    StateClientConfig getConfig() {
        return config
    }
}
