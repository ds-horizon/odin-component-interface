package com.dream11.storage

import com.dream11.OdinUtil
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

interface StorageConfigValidator {
    void validate(JsonNode attributes)

    JsonNode getDefaultConfiguration()

    default JsonNode applyDefaults(JsonNode attributes) {
        ObjectMapper mapper = OdinUtil.getObjectMapper()

        // Start with defaults as the base layer
        Map<String, Object> mergedConfig = mapper.convertValue(getDefaultConfiguration(), Map.class)

        // Apply user-provided attributes on top of defaults
        if (attributes != null && !attributes.isNull()) {
            Map<String, Object> userConfig = mapper.convertValue(attributes, Map.class)
            // User config overrides defaults
            mergedConfig.putAll(userConfig)
        }

        // Convert merged configuration back to JsonNode
        return mapper.valueToTree(mergedConfig)
    }
}
