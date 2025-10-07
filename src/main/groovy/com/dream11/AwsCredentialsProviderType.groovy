package com.dream11

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider

enum AwsCredentialsProviderType {
    /**
     * Uses AWS SDK default credentials provider chain.
     * This is the default behavior if no provider is specified.
     */
    DEFAULT,

    /**
     * Anonymous credentials provider for public S3 buckets or local testing.
     */
    ANONYMOUS,

    /**
     * Loads credentials from environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY.
     */
    ENVIRONMENT_VARIABLE,

    /**
     * Loads credentials from Java system properties aws.accessKeyId and aws.secretAccessKey.
     */
    SYSTEM_PROPERTY,

    /**
     * Loads credentials from AWS shared credentials file (~/.aws/credentials).
     * Uses the default profile unless specified.
     */
    PROFILE,

    /**
     * Loads credentials from EC2 instance metadata service (IMDS).
     */
    INSTANCE_PROFILE,

    /**
     * Loads credentials from ECS/Fargate container credentials endpoint.
     */
    CONTAINER

    /**
     * Returns the string representation of the enum for JSON serialization.
     * @return The name of the enum in uppercase
     */
    @JsonValue
    String toValue() {
        return name()
    }

    /**
     * Creates an enum instance from a string in a case-insensitive manner.
     * This enables JSON deserialization to work with any case (e.g., "anonymous", "Anonymous", "ANONYMOUS").
     * @param value The string value to parse
     * @return The corresponding enum instance
     * @throws IllegalArgumentException if the value doesn't match any enum constant
     */
    @JsonCreator
    static AwsCredentialsProviderType fromValue(String value) {
        if (value == null) {
            return null
        }
        String upperValue = value.toUpperCase()
        try {
            return valueOf(upperValue)
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid credentials provider type: '${value}'. " +
                "Valid values are: ${values().collect { it.name() }.join(', ')}", e
            )
        }
    }

    /**
     * Creates the appropriate AWS credentials provider based on the enum type.
     * @param profileName Optional profile name for PROFILE type (ignored for other types)
     * @return An instance of AwsCredentialsProvider
     */
    AwsCredentialsProvider createProvider(String profileName = null) {
        switch (this) {
            case DEFAULT:
                return DefaultCredentialsProvider.create()
            case ANONYMOUS:
                return AnonymousCredentialsProvider.create()
            case ENVIRONMENT_VARIABLE:
                return EnvironmentVariableCredentialsProvider.create()
            case SYSTEM_PROPERTY:
                return SystemPropertyCredentialsProvider.create()
            case PROFILE:
                return profileName != null ?
                        ProfileCredentialsProvider.create(profileName) :
                        ProfileCredentialsProvider.create()
            case INSTANCE_PROFILE:
                return InstanceProfileCredentialsProvider.create()
            case CONTAINER:
                return ContainerCredentialsProvider.builder().build()
            default:
                return DefaultCredentialsProvider.create()
        }
    }
}
