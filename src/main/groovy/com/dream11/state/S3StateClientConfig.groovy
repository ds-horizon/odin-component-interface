package com.dream11.state

import com.fasterxml.jackson.annotation.JsonInclude

class S3StateClientConfig implements StateClientConfig {
    private String uri

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String endpoint

    private String region
    boolean forcePathStyle

    String getUri() {
        return uri
    }

    String getEndpoint() {
        return endpoint
    }

    String getRegion() {
        return region
    }

    boolean getForcePathStyle() {
        return forcePathStyle
    }

    String toString() {
        return "S3StateClientConfig(uri: ${uri}, endpoint: ${endpoint}, region: ${region}, forcePathStyle: ${forcePathStyle})"
    }
}
