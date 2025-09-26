package com.dream11.state


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class StateConfig {
    @NotBlank(message = "State provider cannot be blank")
    private String provider

    @NotNull(message = "StateClientConfig of StateConfig can not be null")
    @Valid
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "provider")
    @JsonSubTypes([@JsonSubTypes.Type(value = S3StateClientConfig.class, name = "S3")])
    private StateClientConfig config

    String getProvider() {
        return provider
    }

    StateClientConfig getConfig() {
        return config
    }
}
