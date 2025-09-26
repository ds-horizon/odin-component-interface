package com.dream11.state

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.URL

class S3StateClientConfig implements StateClientConfig {
    @NotBlank(message = "S3 URI cannot be empty")
    @Pattern(regexp = "^s3://.*", message = "S3 URI must start with 's3://'")
    private String uri

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @URL(message = "S3 override endpoint must be a valid URI")
    String endpoint

    @NotNull(message = "S3 region is required")
    @Pattern(regexp = "^[a-z]{2}-[a-z]+-[0-9]\$", message = "S3 region must match AWS region format (e.g., us-east-1)")
    private String region = "us-east-1"

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
