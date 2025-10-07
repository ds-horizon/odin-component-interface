package com.dream11.state

import com.dream11.AwsCredentialsProviderType
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.URL
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

class S3StateClientConfig implements StateClientConfig {
    @NotBlank(message = "S3 URI cannot be empty")
    @Pattern(regexp = "^s3://.*", message = "S3 URI must start with 's3://'")
    private String uri

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @URL(message = "S3 override endpoint must be a valid URI")
    private String endpoint

    @NotNull(message = "S3 region is required")
    @Pattern(regexp = "^[a-z]{2}-[a-z]+-[0-9]\$", message = "S3 region must match AWS region format (e.g., us-east-1)")
    private String region = "us-east-1"

    private boolean forcePathStyle

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AwsCredentialsProviderType credentialsProviderType

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String credentialsProfileName

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

    AwsCredentialsProviderType getCredentialsProviderType() {
        return credentialsProviderType
    }

    void setCredentialsProviderType(AwsCredentialsProviderType credentialsProviderType) {
        this.credentialsProviderType = credentialsProviderType
    }

    String getCredentialsProfileName() {
        return credentialsProfileName
    }

    void setCredentialsProfileName(String credentialsProfileName) {
        this.credentialsProfileName = credentialsProfileName
    }

    /**
     * Gets the AWS credentials provider.
     * Creates a provider from the credentialsProviderType if set.
     * If not set, returns null (SDK will use default provider chain).
     */
    AwsCredentialsProvider getCredentialsProvider() {
        if (credentialsProviderType != null) {
            return credentialsProviderType.createProvider(credentialsProfileName)
        }
        return null
    }

    String toString() {
        return "S3StateClientConfig(uri: ${uri}, endpoint: ${endpoint}, region: ${region}, forcePathStyle: ${forcePathStyle}, credentialsProviderType: ${credentialsProviderType}, credentialsProfileName: ${credentialsProfileName})"
    }
}
