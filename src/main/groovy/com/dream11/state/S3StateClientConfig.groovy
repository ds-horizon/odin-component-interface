package com.dream11.state

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

class S3StateClientConfig implements StateClientConfig {
    @JsonProperty("uri")
    String uri

    @JsonProperty("endpoint")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String endpoint

    @JsonProperty("region")
    String region

    S3StateClientConfig() {
        // Default constructor for Jackson
    }

    String getUri() {
        return uri
    }

    String getEndpoint() {
        return endpoint
    }

    String getRegion() {
        return region
    }

    String toString() {
        return "S3StateClientConfig(uri: ${uri}, endpoint: ${endpoint}, region: ${region})"
    }
}
