package com.dream11.spec

import com.fasterxml.jackson.annotation.JsonProperty

enum FlavourStage {
    @JsonProperty("preDeploy")
    PRE_DEPLOY,

    @JsonProperty("deploy")
    DEPLOY,

    @JsonProperty("postDeploy")
    POST_DEPLOY,

    @JsonProperty("undeploy")
    UNDEPLOY,

    @JsonProperty("healthcheck")
    HEALTHCHECK,

    @JsonProperty("validate")
    VALIDATE,

    @JsonProperty("operate")
    OPERATE
}
