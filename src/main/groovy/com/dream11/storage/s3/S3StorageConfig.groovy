package com.dream11.storage.s3

import com.dream11.storage.StorageConfig
import groovy.transform.ToString
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.URL

@ToString(includeNames = true)
class S3StorageConfig implements StorageConfig {
    @NotNull(message = "S3 region is required")
    @Pattern(regexp = "^[a-z]{2}-[a-z]+-[0-9]\$", message = "S3 region must match AWS region format (e.g., us-east-1)")
    String region = "us-east-1"

    @URL(message = "S3 override endpoint must be a valid URI")
    String endpoint

    boolean forcePathStyle
}
