package com.dream11.spec

import com.dream11.Constants
import com.dream11.ExecutionContext
import com.dream11.OdinUtil
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.Constants.DEFAULT_SCHEMA_FILE_NAME
import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class ComponentSpec implements Spec {
    private final List<FlavourSpec> flavours = new ArrayList<>()
    private String dslVersion

    void dslVersion(String dslVersion) {
        this.dslVersion = dslVersion
    }

    void flavour(@DelegatesTo(FlavourSpec) Closure cl) {
        def flavour = new FlavourSpec()
        flavours.add(flavour)
        def code = cl.rehydrate(flavour, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> flavours.isEmpty(), "Component", "flavour block")
        mustExistProperty(() -> dslVersion == null, "Component", "DSL version")

        if (!context.getDSLFileInMetaDir().exists()) {
            log.error("DSL file should be named ${Constants.DSL_NAME} and must be in root directory of component")
            throw new IllegalArgumentException("DSL file should be named ${Constants.DSL_NAME} and must be in root directory of component")
        }

        File schemaFile = new File(OdinUtil.joinPath(context.getMetaWorkingDir(), DEFAULT_SCHEMA_FILE_NAME))
        if (!schemaFile.exists()) {
            log.error(String.format("%s file in component's root directory doesn't exist.", DEFAULT_SCHEMA_FILE_NAME))
            throw new IllegalArgumentException(String.format("%s file in component's root directory doesn't exist.", DEFAULT_SCHEMA_FILE_NAME))
        } else if (!OdinUtil.isJsonFile(schemaFile)) {
            log.error(String.format("%s file in component's root directory is not a valid json file", DEFAULT_SCHEMA_FILE_NAME))
            throw new IllegalArgumentException(String.format("%s file in component's root directory is not a valid json file", DEFAULT_SCHEMA_FILE_NAME))
        }
        flavours.forEach(d -> d.validate(context))
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        return getActiveFlavour(context).execute(context)
    }

    FlavourSpec getActiveFlavour(ExecutionContext context) {
        try {
            return flavours.stream()
                    .filter(currentFlavour -> context.getMetadata().getFlavour() == currentFlavour.getFlavour())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unknown flavour ${context.getMetadata().getFlavour()}"))
        } catch (RuntimeException e) {
            log.error("Exception while getting active flavour: ",e)
            throw e
        }
    }

    List<FlavourSpec> getFlavours() {
        return flavours
    }

    String getDslVersion() {
        return dslVersion
    }
}
