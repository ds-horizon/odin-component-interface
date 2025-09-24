package com.dream11.storage.s3

import groovy.transform.ToString

@ToString(includeNames = true)
class S3StorageConfig {
    String region
    String endpoint
    boolean forcePathStyle

    S3StorageConfig() {
        // Default constructor for Jackson
    }

    S3StorageConfig(String region, String endpoint, boolean forcePathStyle) {
        this.region = region
        this.endpoint = endpoint
        this.forcePathStyle = forcePathStyle
    }
}
