package com.dream11

import groovy.util.logging.Slf4j

import static com.dream11.Constants.BASE_CONFIG
import static com.dream11.Constants.COMPONENT_METADATA
import static com.dream11.Constants.DSL_METADATA
import static com.dream11.Constants.FLAVOUR_CONFIG
import static com.dream11.Constants.OPERATION_CONFIG
import static com.dream11.OdinUtil.logErrorAndExit

/**
 * This config will be sent by Odin.
 * It contains component data provided by the end user and metadata provided by Odin in raw format.
 * */
@Slf4j
class BootstrapConfig {
    private static final def dslMetadata = System.getenv(DSL_METADATA)
    private static final def baseConfig = System.getenv(BASE_CONFIG)
    private static final def flavourConfig = System.getenv(FLAVOUR_CONFIG)
    private static final def operationConfig = System.getenv(OPERATION_CONFIG)
    private static final def componentMetadata = System.getenv(COMPONENT_METADATA)

    static {
        if (dslMetadata == null) {
            logErrorAndExit("Bootstrap config is missing DSL_METADATA")
        }

        /*
         * When action is operation, baseConfig & flavourConfig will be present with original values.
         */
        if (baseConfig == null) {
            logErrorAndExit("Bootstrap config is missing BASE_CONFIG")
        }

        if (flavourConfig == null) {
            logErrorAndExit("Bootstrap config is missing FLAVOUR_CONFIG")
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
