package com.dream11.state

import com.fasterxml.jackson.annotation.JsonInclude

class S3StateClientConfig implements StateClientConfig {
    String uri
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String endpoint
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
