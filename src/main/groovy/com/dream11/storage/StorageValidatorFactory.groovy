package com.dream11.storage

import com.dream11.storage.s3.S3ConfigValidator
import groovy.util.logging.Slf4j

@Slf4j
class StorageValidatorFactory {

    private static final Map<StorageProvider, StorageConfigValidator> validators = [
            (StorageProvider.S3): new S3ConfigValidator()
    ]

    static StorageConfigValidator getValidator(String provider) {
        StorageProvider storageProvider
        try {
            storageProvider = StorageProvider.valueOf(provider)
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unsupported storage provider: ${provider}")
        }

        StorageConfigValidator validator = validators[storageProvider]
        if (!validator) {
            throw new IllegalArgumentException("No validator configured for provider: ${provider}")
        }

        return validator
    }
}
