package com.dream11

import groovy.util.logging.Slf4j

import static com.dream11.Constants.ODIN_BASE_CONFIG
import static com.dream11.Constants.ODIN_COMPONENT_METADATA
import static com.dream11.Constants.ODIN_DSL_METADATA
import static com.dream11.Constants.ODIN_FLAVOUR_CONFIG
import static com.dream11.Constants.ODIN_OPERATION_CONFIG
import static com.dream11.OdinUtil.logErrorAndExit

/**
 * This config will be sent by Odin.
 * It contains component data provided by the end user and metadata provided by Odin in raw format.
 * */
@Slf4j
class BootstrapConfig {
    private static final def dslMetadata = System.getenv(ODIN_DSL_METADATA)
    private static final def baseConfig = System.getenv(ODIN_BASE_CONFIG)
    private static final def flavourConfig = System.getenv(ODIN_FLAVOUR_CONFIG)
    private static final def operationConfig = System.getenv(ODIN_OPERATION_CONFIG)
    private static final def componentMetadata = System.getenv(ODIN_COMPONENT_METADATA)

    static {
        if (dslMetadata == null) {
            logErrorAndExit("Bootstrap config is missing ODIN_DSL_METADATA")
        }

        /*
         * When action is operation, baseConfig & flavourConfig will be present with original values.
         */
        if (baseConfig == null) {
            logErrorAndExit("Bootstrap config is missing ODIN_BASE_CONFIG")
        }

        if (flavourConfig == null) {
            logErrorAndExit("Bootstrap config is missing ODIN_FLAVOUR_CONFIG")
        }
    }

    def static getDslMetadata() {
        return dslMetadata
    }

    def static getBaseConfig() {
        return baseConfig
    }

    def static getFlavourConfig() {
        return flavourConfig
    }

    def static getOperationConfig() {
        return operationConfig
    }

    def static getComponentMetadata() {
        // this is being set to default because not every component would need component metadata
        return componentMetadata == null ? '{}' : componentMetadata
    }

}
