package com.dream11.storage.s3

import com.dream11.AwsCredentialsProviderType
import com.dream11.storage.StorageConfig
import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.ToString
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.URL
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

@ToString(includeNames = true)
class S3StorageConfig implements StorageConfig {
    @NotNull(message = "S3 region is required")
    @Pattern(regexp = "^[a-z]{2}-[a-z]+-[0-9]\$", message = "S3 region must match AWS region format (e.g., us-east-1)")
    private String region = "us-east-1"

    @URL(message = "S3 override endpoint must be a valid URI")
    private String endpoint

    private boolean forcePathStyle

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AwsCredentialsProviderType credentialsProviderType

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String credentialsProfileName

    String getRegion() {
        return region
    }

    String getEndpoint() {
        return endpoint
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
}
