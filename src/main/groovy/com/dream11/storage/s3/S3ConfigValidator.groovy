package com.dream11.storage.s3

import com.dream11.OdinUtil
import com.dream11.storage.StorageConfigValidator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import groovy.util.logging.Slf4j

@Slf4j
class S3ConfigValidator implements StorageConfigValidator {

    private static final String SCHEMA = '''
    {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "required": ["region"],
      "properties": {
        "region": {
          "type": "string",
          "pattern": "^[a-z]{2}-[a-z]+-[0-9]{1}$",
          "description": "AWS region for S3 bucket"
        },
        "endpoint": {
          "type": "string",
          "format": "uri",
          "description": "Custom S3 endpoint URL"
        },
        "forcePathStyle": {
          "type": "boolean",
          "description": "Force path-style URLs for S3 requests"
        }
      },
      "additionalProperties": false
    }
    '''

    private final JsonSchema schema
    private final ObjectMapper mapper
    private final JsonNode defaultConfiguration

    S3ConfigValidator() {
        this.mapper = OdinUtil.getObjectMapper()
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        JsonNode schemaNode = mapper.readTree(SCHEMA)
        this.schema = factory.getSchema(schemaNode)

        // Define default configuration
        this.defaultConfiguration = mapper.readTree('''
        {
            "region": "us-east-1",
            "forcePathStyle": false
        }
        ''')
    }

    @Override
    void validate(JsonNode attributes) {
        Set<ValidationMessage> errors = schema.validate(attributes)
        if (!errors.isEmpty()) {
            String errorMessages = errors.collect { it.getMessage() }.join(", ")
            throw new IllegalArgumentException("Invalid S3 configuration: ${errorMessages}")
        }
    }

    @Override
    JsonNode getDefaultConfiguration() {
        return defaultConfiguration
    }
}
