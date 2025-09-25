package com.dream11.storage

import com.dream11.OdinUtil
import com.dream11.storage.s3.S3StorageConfig
import com.fasterxml.jackson.databind.JsonNode

final class StorageConfigParser {
    private StorageConfigParser() {

    }

    static StorageConfig parse(String provider, JsonNode jsonNode) {
        switch (StorageProvider.valueOf(provider.toUpperCase())) {
            case StorageProvider.S3:
                return OdinUtil.getObjectMapper().convertValue(jsonNode, S3StorageConfig.class)
            default:
                throw new IllegalArgumentException("Unsupported storage provider: ${provider}")
        }
    }
}
