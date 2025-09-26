package com.dream11

import com.dream11.lock.LockClient
import com.dream11.lock.LockClientFactory
import com.dream11.lock.LockConfig
import com.dream11.spec.FlavourStage
import com.dream11.state.StateConfig
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import static com.dream11.Constants.HEALTHCHECK_CONNECTION_TIMEOUT
import static com.dream11.Constants.RETRY_MAX_ATTEMPTS
import static com.dream11.OdinUtil.mustExistProperty

/**
 * This is a static type representation of the metadata sent by Odin.
 * */
@JsonIgnoreProperties(ignoreUnknown = true)
class DslMetadata {
    /**
     * Flavour to be executed.
     * A component can have multiple flavours based on what all type it supports.
     * e.g. A component can be deployed on AWS VM or AWS K8s or can be managed service such as RDS
     * */
    private String flavour

    /**
     * Flavour stage to be executed. Can be deploy/undeploy/healthcheck
     * */
    private FlavourStage stage

    /**
     * Lock configuration for component
     * */
    private LockConfig lockConfig

    private LockClient lockClient

    /**
     * State config for component
     * */
    private StateConfig stateConfig

    /**
     * Default is 5, can be overridden by Odin by sending this value in metadata.
     * */
    private int retryMaxAttempts = RETRY_MAX_ATTEMPTS

    /**
     * Default is 5, can be overridden by Odin by sending this value in metadata.
     * */
    private int healthCheckConnectionTimeout = HEALTHCHECK_CONNECTION_TIMEOUT


    private Map<String, Object> config = [:]

    String getFlavour() {
        return flavour
    }

    void setFlavour(String flavour) {
        this.flavour = flavour
    }

    FlavourStage getStage() {
        return stage
    }

    boolean isDeploying() {
        return getStage() == FlavourStage.DEPLOY
    }

    boolean isUnDeploying() {
        return getStage() == FlavourStage.UNDEPLOY
    }

    boolean isHealthChecking() {
        return getStage() == FlavourStage.HEALTHCHECK
    }

    boolean isValidating() {
        return getStage() == FlavourStage.VALIDATE
    }

    boolean isPreDeploying() {
        return getStage() == FlavourStage.PRE_DEPLOY
    }

    boolean isPostDeploying() {
        return getStage() == FlavourStage.POST_DEPLOY
    }

    boolean isOperating() {
        return getStage() == FlavourStage.OPERATE
    }

    void setStage(FlavourStage stage) {
        this.stage = stage
    }

    void setConfig(Map<String, Object> config) {
        this.config = config
    }

    private LockClient initialiseLockClient() {
        if (lockConfig == null) {
            throw new RuntimeException("LockConfig is not set")
        }
        this.lockClient = LockClientFactory.getLockClient(lockConfig)
        return lockClient
    }

    Map<String, Object> getConfig() {
        return config
    }

    void validate() {
        mustExistProperty(() -> flavour == null, "DslMetadata", "flavour")
        mustExistProperty(() -> stage == null, "DslMetadata", "stage")

        if (isOperating()) {
            mustExistProperty(() -> config.get(Constants.OPERATION_NAME) == null, "DslMetadata", String.format("config.%s", Constants.OPERATION_NAME))
        }
        if (isValidating()) {
            mustExistProperty(() -> config.get(Constants.STAGE_NAME) == null, "DslMetadata", String.format("config.%s", Constants.STAGE_NAME))
            if (config.get(Constants.STAGE_NAME) == Constants.STAGE_OPERATE) {
                mustExistProperty(() -> config.get(Constants.OPERATION_NAME) == null, "DslMetadata", String.format("config.%s",
                        Constants.OPERATION_NAME))
            }
        }
    }

    int getRetryMaxAttempts() {
        return retryMaxAttempts
    }

    // required by serializer
    void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts
    }


    int getHealthCheckConnectionTimeout() {
        return healthCheckConnectionTimeout
    }

    LockConfig getLockConfig() {
        return lockConfig
    }

    LockClient getOrCreateLockClient() {
        if (lockClient == null) {
            return initialiseLockClient()
        }
        return lockClient
    }

    StateConfig getStateConfig() {
        return stateConfig
    }
}
