package com.dream11.spec

import com.dream11.BootstrapConfig
import com.dream11.ExecutionContext
import com.dream11.Odin
import com.dream11.StageSequenceExecutor
import com.dream11.exec.CommandResponse
import com.hubspot.jinjava.Jinjava
import com.jayway.jsonpath.PathNotFoundException
import groovy.util.logging.Slf4j
import org.json.JSONObject

import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.util.stream.Collectors

import static com.dream11.Constants.DEFAULT_DATA_FILE_NAME
import static com.dream11.Constants.DEFAULT_SCHEMA_FILE_NAME
import static com.dream11.Constants.META_WORKING_DIR
import static com.dream11.Constants.OPERATIONS_DIR
import static com.dream11.Constants.OPERATION_NAME
import static com.dream11.Constants.TEMPLATE_IGNORE_FILE_NAME
import static com.dream11.Constants.UTF_8
import static com.dream11.OdinUtil.getObjectMapper
import static com.dream11.OdinUtil.getPostfixPermissions
import static com.dream11.OdinUtil.getUsingJsonPath
import static com.dream11.OdinUtil.isJsonFile
import static com.dream11.OdinUtil.isValidatingOperation
import static com.dream11.OdinUtil.joinPath
import static com.dream11.OdinUtil.mergeJsons
import static com.dream11.OdinUtil.mustExistProperty
import static com.dream11.OdinUtil.readFile
import static com.dream11.OdinUtil.validate
import static java.nio.charset.Charset.forName
import static java.nio.file.Files.readString
import static org.apache.commons.io.FileUtils.copyFile
import static org.apache.commons.io.FileUtils.listFiles
import static org.apache.commons.io.FileUtils.readFileToString
import static org.apache.commons.io.FileUtils.writeStringToFile

@Slf4j
class FlavourSpec implements Spec {
    /**
     * There can be multiple flavours in deploy spec. Every flavour must be unique that targets specific cloud provider or runtime.
     * The flavour name represents that. E.g. if your flavour deploys in AWS VM, you could name it aws_vm or if it deploys in
     * AWS k8s, you could name it aws_k8s.
     * */
    private String flavour

    /**
     * It represents "*config" json sent by Odin in raw format.
     * */
    private final String baseConfig
    private String baseConfigWithDefaults = ""

    private final String flavourConfig
    private String flavourConfigWithDefaults = ""

    private final String operationConfig
    private String operationConfigWithDefaults = ""

    private PreDeploySpec preDeploy
    private DeploySpec deploy
    private PostDeploySpec postDeploy
    private HealthCheckSpec healthCheck
    private UnDeploySpec undeploy
    private CaughtSpec caught
    private FinalisedSpec finalised
    private final List<OperateSpec> operations = new ArrayList<>()

    private String relativeBaseDir
    private String runnerImage

    FlavourSpec() {
        if (BootstrapConfig.getBaseConfig() == null) {
            log.error("Base config is not provided.")
            throw new IllegalArgumentException("Base config is not provided.")
        }
        baseConfig = BootstrapConfig.getBaseConfig()

        if (BootstrapConfig.getFlavourConfig() == null) {
            log.error("Flavour config is not provided.")
            throw new IllegalArgumentException("Flavour config is not provided.")
        }
        flavourConfig = BootstrapConfig.getFlavourConfig()

        if (BootstrapConfig.getOperationConfig() != null) {
            operationConfig = BootstrapConfig.getOperationConfig()
        }
    }

    void runnerImage(String image) {
        this.runnerImage = image
    }

    void name(String name) {
        this.flavour = name
        this.relativeBaseDir = name
    }

    void preDeploy(@DelegatesTo(PreDeploySpec) Closure cl) {
        this.preDeploy = new PreDeploySpec(this)
        def code = cl.rehydrate(preDeploy, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void deploy(@DelegatesTo(DeploySpec) Closure cl) {
        deploy = new DeploySpec(this)
        def code = cl.rehydrate(deploy, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void postDeploy(@DelegatesTo(PostDeploySpec) Closure cl) {
        this.postDeploy = new PostDeploySpec(this)
        def code = cl.rehydrate(postDeploy, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void healthcheck(@DelegatesTo(HealthCheckSpec) Closure cl) {
        healthCheck = new HealthCheckSpec(this)
        def code = cl.rehydrate(healthCheck, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void undeploy(@DelegatesTo(UnDeploySpec) Closure cl) {
        undeploy = new UnDeploySpec(this)
        def code = cl.rehydrate(undeploy, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void caught(@DelegatesTo(CaughtSpec) Closure cl) {
        caught = new CaughtSpec(this)
        def code = cl.rehydrate(caught, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void finalised(@DelegatesTo(CaughtSpec) Closure cl) {
        finalised = new FinalisedSpec(this)
        def code = cl.rehydrate(finalised, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void operate(@DelegatesTo(OperateSpec) Closure cl) {
        def operation = new OperateSpec(this)
        operations.add(operation)
        def code = cl.rehydrate(operation, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    @Override
    void validate(ExecutionContext context) {
        validateSelfAttributes()

        if (preDeploy != null) {
            preDeploy.validate(context)
        }

        deploy.validate(context)
        if (postDeploy != null) {
            postDeploy.validate(context)
        }
        healthCheck.validate(context)
        undeploy.validate(context)

        operations.each { it.validate(context) }
    }

    private String getFlavourRootDir() {
        return joinPath(META_WORKING_DIR, relativeBaseDir)
    }

    private String getOperationsRootDir(String operationName) {
        return joinPath(META_WORKING_DIR, relativeBaseDir, OPERATIONS_DIR, operationName)
    }

    private void validateSelfAttributes() {
        mustExistProperty(() -> flavour == null, "Flavour", "flavour name")
        mustExistProperty(() -> deploy == null, "Flavour ${flavour}", "deploy block")
        mustExistProperty(() -> healthCheck == null, "Flavour ${flavour}", "healthcheck block")
        mustExistProperty(() -> undeploy == null, "Flavour ${flavour}", "undeploy block")

        File schemaFile = new File(joinPath(getFlavourRootDir(), DEFAULT_SCHEMA_FILE_NAME))
        if (!schemaFile.exists()) {
            log.error(String.format("%s file in %s flavour's directory doesn't exist.", DEFAULT_SCHEMA_FILE_NAME, this.flavour))
            throw new IllegalArgumentException(String.format("%s file in %s flavour's directory doesn't exist.", DEFAULT_SCHEMA_FILE_NAME, this.flavour))
        } else if (!isJsonFile(schemaFile)) {
            log.error(String.format("%s file in %s flavour's directory is not a valid json file", DEFAULT_SCHEMA_FILE_NAME, this.flavour))
            throw new IllegalArgumentException(String.format("%s file in %s flavour's directory is not a valid json file", DEFAULT_SCHEMA_FILE_NAME, this.flavour))
        }
    }

    def getFlavour() {
        return flavour
    }

    private String generateTemplates(ExecutionContext context) {
        String templateData = generateTemplateData(context)

        log.debug("Generating templates at : ${context.getWorkingDir()}")
        List<String> templateIgnoreFiles = getTemplateIgnoreFiles(context)
        def inputDir = new File("." + File.separator + relativeBaseDir)

        listFiles(inputDir, null, true)
                .stream()
                .forEach(file -> generateFileTemplate(templateData.toString(), file,
                        getOutputFilePath(file, inputDir.getAbsolutePath(), context.getWorkingDir()), templateIgnoreFiles))
        copyFile(context.getDSLFileInMetaDir(), context.getDSLFileInWorkingDir())
        return context.getWorkingDir()
    }

    String generateTemplateData(ExecutionContext context) {
        JSONObject templateData = new JSONObject()

        baseConfigWithDefaults = getBaseConfigWithDefaults()
        File baseSchemaFile = new File(joinPath(context.getMetaWorkingDir(), DEFAULT_SCHEMA_FILE_NAME))
        validate(readFile(baseSchemaFile), baseConfigWithDefaults)
        // Add component metadata to base config for generating templates
        templateData.put("baseConfig", new JSONObject(baseConfigWithDefaults))

        flavourConfigWithDefaults = getFlavourConfigWithDefaults()
        File flavourSchemaFile = new File(joinPath(getFlavourRootDir(), DEFAULT_SCHEMA_FILE_NAME))
        validate(readFile(flavourSchemaFile), flavourConfigWithDefaults)
        templateData.put("flavourConfig", new JSONObject(flavourConfigWithDefaults))

        operationConfigWithDefaults = getOperationConfigWithDefaults()
        if (context.getMetadata().isOperating() || isValidatingOperation(context)) {
            String operationName = context.getMetadata().getConfig().get(OPERATION_NAME)
            File operationSchemaFile = new File(joinPath(getOperationsRootDir(operationName), DEFAULT_SCHEMA_FILE_NAME))
            validate(readFile(operationSchemaFile), operationConfigWithDefaults)
            templateData.put("operationConfig", new JSONObject(operationConfigWithDefaults))
        }

        templateData.put("componentMetadata", new JSONObject(BootstrapConfig.getComponentMetadata()))
        return templateData.toString()
    }

    private static void generateFileTemplate(String templateData, File inputFile, String outputFilePath, List<String> templateIgnoreFiles) {
        if (templateIgnoreFiles.contains(inputFile.getPath())) {
            copyFile(inputFile, new File(outputFilePath))
        } else {
            applyJinjaTemplating(templateData, inputFile, outputFilePath)
        }
    }

    private void validateOperation(ExecutionContext context) {
        String operationName = context.getMetadata().getConfig().get(OPERATION_NAME)
        String operationRootDir = getOperationsRootDir(operationName)
        if (!new File(operationRootDir).exists()) {
            log.error("Unknown operation ${context.getMetadata().getConfig().get(OPERATION_NAME)} for flavour ${getFlavour()}")
            throw new IllegalArgumentException("Unknown operation ${context.getMetadata().getConfig().get(OPERATION_NAME)} for flavour ${getFlavour()}")
        }
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        if (context.getMetadata().getFlavour() == null) {
            return Collections.emptyList()
        }

        if (context.getMetadata().isOperating() || isValidatingOperation(context)) {
            validateOperation(context)
        }
        generateTemplates(context)
        return executeAppropriateStages(context)
    }

    private List<CommandResponse> executeAppropriateStages(ExecutionContext context) {
        StageSequenceExecutor stageSequenceExecutor = new StageSequenceExecutor(preDeploy, deploy,
                postDeploy, healthCheck,
                undeploy, caught, finalised, operations)

        if (context.getMetadata().isPreDeploying()) {
            return stageSequenceExecutor.executePreDeployStageSequence(context)

        } else if (context.getMetadata().isPostDeploying()) {
            return stageSequenceExecutor.executePostDeployStageSequence(context)

        } else if (context.getMetadata().isDeploying()) {
            return stageSequenceExecutor.executeDeployStageSequence(context)

        } else if (context.getMetadata().isHealthChecking()) {
            return stageSequenceExecutor.executeHealthCheckStageSequence(context)

        } else if (context.getMetadata().isUnDeploying()) {
            return stageSequenceExecutor.executeUnDeployStageSequence(context)

        } else if (context.getMetadata().isValidating()) {
            stageSequenceExecutor.acquireLock(false)
            return stageSequenceExecutor.executeValidateStageSequence()
        } else if (context.getMetadata().isOperating()) {
            return stageSequenceExecutor.executeOperateStageSequence(context)
        } else {
            log.error("Unmapped flavour stage: ${context.getMetadata().getStage()}")
            throw new IllegalArgumentException("Unmapped flavour stage: ${context.getMetadata().getStage()}")
        }
    }

    private static String getOutputFilePath(File source, String sourceRootDir, String destRootDir) {
        return source.getAbsolutePath().replaceAll(sourceRootDir, destRootDir)
    }

    private static void applyJinjaTemplating(String templateData, File inputFile, String outputFilePath) {
        def jinja = new Jinjava()
        def context = getObjectMapper().readValue(templateData, Map.class)
        def outputFile = new File(outputFilePath)

        try {
            def fileContents = readString(inputFile.toPath())
            def result = jinja.render(fileContents, context)

            writeStringToFile(outputFile, result, forName(UTF_8))
            Files.setPosixFilePermissions(outputFile.toPath(), getPostfixPermissions())
        } catch (MalformedInputException ignored) {
            log.debug("Could not apply jinja templating on ${inputFile.absolutePath}")
            copyFile(inputFile, outputFile)
        }
    }

    private String getOperationDefaults(ExecutionContext context) {
        String operationName = context.getMetadata().getConfig().get(OPERATION_NAME)
        File operationDefaultJson = new File(joinPath(getOperationsRootDir(operationName), DEFAULT_DATA_FILE_NAME))
        if (operationDefaultJson.exists()) {
            if (!isJsonFile(operationDefaultJson)) {
                log.error(String.format("%s file in %s operation root directory is not a valid json file", DEFAULT_DATA_FILE_NAME, operationName))
                throw new IllegalArgumentException(String.format("%s file in %s operation root directory is not a valid json file", DEFAULT_DATA_FILE_NAME, operationName))
            }
            return readFileToString(operationDefaultJson, forName(UTF_8))
        }
        return null
    }

    private String getFlavourDefaults() {
        File flavourDefaultJson = new File(joinPath(getFlavourRootDir(), DEFAULT_DATA_FILE_NAME))
        if (flavourDefaultJson.exists()) {
            if (!isJsonFile(flavourDefaultJson)) {
                log.error(String.format("%s file in %s flavour's directory is not a valid json file", DEFAULT_DATA_FILE_NAME, this.flavour))
                throw new IllegalArgumentException(String.format("%s file in %s flavour's directory is not a valid json file", DEFAULT_DATA_FILE_NAME, this.flavour))
            }
            return readFileToString(flavourDefaultJson, forName(UTF_8))
        }
        return null
    }

    private String getComponentDefaults(ExecutionContext context) {
        File componentDefaultJson = new File(joinPath(context.getMetaWorkingDir(), DEFAULT_DATA_FILE_NAME))
        if (componentDefaultJson.exists()) {
            if (!isJsonFile(componentDefaultJson)) {
                log.error(String.format("%s file in component's root directory is not a valid json file", DEFAULT_DATA_FILE_NAME))
                throw new IllegalArgumentException(String.format("%s file in component's root directory is not a valid json file", DEFAULT_DATA_FILE_NAME))
            }
            return readFileToString(componentDefaultJson, forName(UTF_8))
        }
        return null
    }

    private static List<String> getTemplateIgnoreFiles(ExecutionContext context) {
        // TODO add flavour specific ignore file and glob pattern support
        File templateIgnoreFile = new File(joinPath(context.getMetaWorkingDir(), TEMPLATE_IGNORE_FILE_NAME))
        if (templateIgnoreFile.exists()) {
            return readFileToString(templateIgnoreFile, forName(UTF_8)).split("\n")
                    .toList()
                    .stream()
                    .filter(fileName -> fileName.trim() != "")
                    .map(fileName -> fileName.startsWith("./") ? fileName : "./" + fileName)
                    .collect(Collectors.toList())
        }
        return []
    }

    Object data(String... jsonPath) {
        if (BootstrapConfig.dslMetadata.contains(flavour)) {
            // run replacement only if its a current flavour because only that time we will have relevant info in data
            try {
                return getUsingJsonPath(generateTemplateData(Odin.getExecutionContext()), jsonPath)
            } catch (PathNotFoundException ignored) {
                log.error("Error while getting data from json path: ${jsonPath.join('.')}")
            }
        }
        return null
    }

    String getBaseConfigWithDefaults() {
        if (Odin.getExecutionContext().getMetadata().getFlavour() == flavour) {
            return getComponentDefaults(Odin.getExecutionContext()) != null
                    ? mergeJsons(List.of(baseConfig, getComponentDefaults(Odin.getExecutionContext())))
                    : String.copyValueOf(baseConfig.toCharArray())
        }
        return "{}"
    }

    String getFlavourConfigWithDefaults() {
        if (Odin.getExecutionContext().getMetadata().getFlavour() == flavour) {
            return getFlavourDefaults() != null
                    ? mergeJsons(List.of(flavourConfig, getFlavourDefaults()))
                    : String.copyValueOf(flavourConfig.toCharArray())
        }
        return "{}"
    }

    String getOperationConfigWithDefaults() {
        if (Odin.getExecutionContext().getMetadata().getFlavour() == flavour && operationConfig != null) {
            return getOperationDefaults(Odin.getExecutionContext()) != null
                    ? mergeJsons(List.of(operationConfig, getOperationDefaults(Odin.getExecutionContext())))
                    : String.copyValueOf(operationConfig.toCharArray())
        }
        return "{}"
    }

    DeploySpec getDeploySpec() {
        return deploy
    }

    // required by deserializer
    PreDeploySpec getPreDeploy() {
        return preDeploy
    }

    DeploySpec getDeploy() {
        return deploy
    }

    // required by deserializer
    PostDeploySpec getPostDeploy() {
        return postDeploy
    }

    HealthCheckSpec getHealthCheck() {
        return healthCheck
    }

    UnDeploySpec getUndeploy() {
        return undeploy
    }

    // required by deserializer
    String getRunnerImage() {
        return runnerImage
    }

    String getRelativeBaseDir() {
        return relativeBaseDir
    }
}
