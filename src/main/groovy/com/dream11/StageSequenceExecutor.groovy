package com.dream11

import com.dream11.exec.CommandResponse
import com.dream11.lock.LockClient
import com.dream11.lock.LockConfig
import com.dream11.spec.CaughtSpec
import com.dream11.spec.DeploySpec
import com.dream11.spec.FinalisedSpec
import com.dream11.spec.FlavourStage
import com.dream11.spec.HealthCheckSpec
import com.dream11.spec.OperateSpec
import com.dream11.spec.PostDeploySpec
import com.dream11.spec.PreDeploySpec
import com.dream11.spec.Spec
import com.dream11.spec.UnDeploySpec
import groovy.util.logging.Slf4j

import static com.dream11.Constants.STAGE_DEPLOY
import static com.dream11.Constants.STAGE_HEALTHCHECK
import static com.dream11.Constants.STAGE_OPERATE
import static com.dream11.Constants.STAGE_POST_DEPLOY
import static com.dream11.Constants.STAGE_PRE_DEPLOY
import static com.dream11.Constants.STAGE_UNDEPLOY
import static com.dream11.OdinUtil.isSuccessfulExecution

/**
 * This class can run multiple phases based on the chosen stage.
 * For example, when stage = deploy, it runs preDeploy, Deploy, PostDeploy, HealthChecks
 * */
@Slf4j
class StageSequenceExecutor {

    private PreDeploySpec preDeploy
    private DeploySpec deploy
    private PostDeploySpec postDeploy
    private HealthCheckSpec healthCheck
    private UnDeploySpec undeploy
    private CaughtSpec caught
    private FinalisedSpec finalised
    private List<OperateSpec> operations
    private boolean lockStage = true

    StageSequenceExecutor(PreDeploySpec preDeploy, DeploySpec deploy,
                          PostDeploySpec postDeploy, HealthCheckSpec healthCheck,
                          UnDeploySpec undeploy, CaughtSpec caught,
                          FinalisedSpec finalised, List<OperateSpec> operations) {
        this.preDeploy = preDeploy
        this.deploy = deploy
        this.postDeploy = postDeploy
        this.healthCheck = healthCheck
        this.undeploy = undeploy
        this.caught = caught
        this.finalised = finalised
        this.operations = operations
    }

    void acquireLock(boolean acquireLock) {
        this.lockStage = acquireLock
    }

    List<CommandResponse> executeDeployStageSequence(ExecutionContext context) {
        LinkedHashMap<String, Spec> stages = new LinkedHashMap<>()
        stages.put(STAGE_PRE_DEPLOY, preDeploy)
        stages.put(STAGE_DEPLOY, deploy)
        stages.put(STAGE_POST_DEPLOY, postDeploy)
        stages.put(STAGE_HEALTHCHECK, healthCheck)
        return executeStages(stages, context)
    }

    private boolean doesStageRequireStateLock(LinkedHashMap<String, Spec> stages) {
        Spec stageSpec = stages.get(STAGE_OPERATE)
        if (stageSpec instanceof OperateSpec) {
            OperateSpec spec = (OperateSpec) stageSpec
            return !spec.disableLocking()
        }
        return false
    }

    private List<CommandResponse> executeStages(LinkedHashMap<String, Spec> stages, ExecutionContext context) {
        LockConfig lockConfig = context.getMetadata().getLockConfig()
        FlavourStage stage = context.getMetadata().stage
        if (shouldAcquireLock(stage, stages, lockStage, lockConfig)) {
            log.debug("Acquiring lock for : ${lockConfig.getProvider()}")
            LockClient lockClient = context.getMetadata().getOrCreateLockClient()
            try {
                if (!lockClient.acquireStateLock()) {
                    throw new RuntimeException("Failed to acquire lock for : ${lockConfig.getProvider()}")
                }
                List<CommandResponse> allResponses = getResponses(stages, context)
                //only release the lock if you acquired it
                if (!lockClient.releaseStateLock()) {
                    throw new RuntimeException("Failed to release lock for : ${lockConfig.getProvider()}")
                }
                return allResponses
            } catch (Exception e) {
                if (!lockClient.releaseStateLock()) {
                    throw new RuntimeException("Failed to release lock for : ${lockConfig.getProvider()}")
                }
                throw e
            }
        } else {
            log.debug("Skipping lock acquisition")
            return getResponses(stages, context)
        }
    }

    private boolean shouldAcquireLock(FlavourStage stage, LinkedHashMap<String, Spec> stages, boolean lockStage, LockConfig lockConfig) {
        return isLockMandatory(stage, stages) &&
                lockStage &&
                lockConfig != null &&
                lockConfig.getProvider() != null
    }

    boolean isLockMandatory(FlavourStage stage, LinkedHashMap<String, Spec> stages) {
        return stage in [FlavourStage.PRE_DEPLOY, FlavourStage.DEPLOY, FlavourStage.POST_DEPLOY, FlavourStage.UNDEPLOY]
                || (stage == FlavourStage.OPERATE && doesStageRequireStateLock(stages))
    }

    private List<CommandResponse> getResponses(LinkedHashMap<String, Spec> stages, ExecutionContext context) {
        List<CommandResponse> allResponses = new ArrayList<>()
        for (Map.Entry<String, Spec> entry : stages.entrySet()) {
            def stage = entry.getKey()
            if (entry.getValue() == null) {
                log.debug("Skipping stage ${stage}")
            } else {
                def stageResponses = executeSingleStage(stage, entry.getValue(), context)
                allResponses.addAll(stageResponses)

                if (!isSuccessfulExecution(stageResponses)) {
                    break
                }
            }
        }
        log.debug("Finalised block will be executed (if configured).")

        if (finalised != null) {
            finalised.execute(context)
        }
        return allResponses
    }

    private List<CommandResponse> executeSingleStage(String stage, Spec spec, ExecutionContext context) {
        log.debug("Executing ${stage} stage..")

        List<CommandResponse> responses = new ArrayList<>()
        responses.addAll(spec.execute(context))

        if (!isSuccessfulExecution(responses)) {
            log.debug("'${stage}' stage did not execute successfully. Skipping subsequent scheduled stages." +
                    " Caught block will be executed (if configured).")
            responses.addAll(executeCaughtStageSequence(context, new CaughtContext(stage, responses)))
        }
        return responses
    }

    List<CommandResponse> executeHealthCheckStageSequence(ExecutionContext context) {
        LinkedHashMap<String, Spec> stages = new LinkedHashMap<>()
        stages.put(STAGE_HEALTHCHECK, healthCheck)
        return executeStages(stages, context)
    }

    List<CommandResponse> executeUnDeployStageSequence(ExecutionContext context) {
        LinkedHashMap<String, Spec> stages = new LinkedHashMap<>()
        stages.put(STAGE_UNDEPLOY, undeploy)
        return executeStages(stages, context)
    }

    List<CommandResponse> executePreDeployStageSequence(ExecutionContext context) {
        LinkedHashMap<String, Spec> stages = new LinkedHashMap<>()
        stages.put(STAGE_PRE_DEPLOY, preDeploy)
        return executeStages(stages, context)
    }

    List<CommandResponse> executePostDeployStageSequence(ExecutionContext context) {
        LinkedHashMap<String, Spec> stages = new LinkedHashMap<>()
        stages.put(STAGE_POST_DEPLOY, postDeploy)
        return executeStages(stages, context)
    }

    List<CommandResponse> executeCaughtStageSequence(ExecutionContext context, CaughtContext caughtContext) {
        if (caught == null) {
            return Collections.emptyList()
        }

        caught.preWarm(caughtContext)
        return caught.execute(context)
    }

    List<CommandResponse> executeOperateStageSequence(ExecutionContext context) {
        LinkedHashMap<String, Spec> stages = new LinkedHashMap<>()
        OperateSpec operation = operations.stream()
                .filter(operation -> context.getMetadata().getConfig().get(Constants.OPERATION_NAME) == operation.getName())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown operation ${context.getMetadata().getConfig().get(Constants.OPERATION_NAME)} for flavour ${context.getMetadata().getFlavour()}"))
        stages.put(STAGE_OPERATE, operation)
        if (operation.performHealthcheck()) {
            log.debug("Healthcheck will be performed after operation.")
            stages.put(STAGE_HEALTHCHECK, healthCheck)
        }
        return executeStages(stages, context)
    }

    List<CommandResponse> executeValidateStageSequence() {
        log.info("Validation successful")
        return Collections.emptyList()
    }
}
