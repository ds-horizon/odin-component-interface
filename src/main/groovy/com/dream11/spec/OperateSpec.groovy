package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.Constants.DEFAULT_SCHEMA_FILE_NAME
import static com.dream11.Constants.ODIN_MARKER_END
import static com.dream11.Constants.ODIN_MARKER_START
import static com.dream11.Constants.OPERATIONS_DIR
import static com.dream11.OdinUtil.buildMarkedCommandResponse
import static com.dream11.OdinUtil.isJsonFile
import static com.dream11.OdinUtil.joinPath
import static com.dream11.OdinUtil.mustExistProperty
import static com.dream11.OdinUtil.publishState

@Slf4j
class OperateSpec extends EnclosedByFlavour implements Spec {

    private String name
    private final List<BaseCommand> commands = new ArrayList<>()
    private BaseCommand outCommand
    private Boolean healthcheck = true
    private Boolean disableLocking = false

    OperateSpec(FlavourSpec flavour) {
        super(flavour)
    }

    void name(String name) {
        this.name = name
    }

    void run(String command) {
        this.commands.add(new RunCommandSpec(getFlavour(), command))
    }

    void healthcheck(Boolean healthcheck) {
        this.healthcheck = healthcheck
    }

    void disableLocking(Boolean stateLock) {
        this.disableLocking = stateLock
    }
    boolean disableLocking() {
        return disableLocking
    }

    void download(@DelegatesTo(FileDownloadSpec) Closure cl) {
        BaseCommand downloadCommand = new FileDownloadSpec(getFlavour())
        def code = cl.rehydrate(downloadCommand, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        this.commands.add(downloadCommand)
    }

    void out(String out) {
        this.outCommand = new RunCommandSpec(getFlavour(), out, true)
    }

    String getName() {
        return name
    }

    List<BaseCommand> getCommands() {
        return commands
    }

    boolean performHealthcheck() {
        return healthcheck
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> name == null, "Operate block in ${getFlavour().getFlavour()} flavour", "name")
        mustExistProperty(commands::isEmpty, "Operate block in ${getFlavour().getFlavour()} flavour", "run")
        File schemaFile = new File(joinPath(context.getMetaWorkingDir(), this.getFlavour().getRelativeBaseDir(), OPERATIONS_DIR, name, DEFAULT_SCHEMA_FILE_NAME))
        if (!schemaFile.exists()) {
            log.error(String.format("%s file in %s operation root directory doesn't exist.", DEFAULT_SCHEMA_FILE_NAME, name))
            throw new IllegalArgumentException(String.format("%s file in %s operation root directory doesn't exist.", DEFAULT_SCHEMA_FILE_NAME, name))
        } else if (!isJsonFile(schemaFile)) {
            log.error(String.format("%s file in %s operation root directory is not a valid json file", DEFAULT_SCHEMA_FILE_NAME, name))
            throw new IllegalArgumentException(String.format("%s file in %s operation root directory is not a valid json file", DEFAULT_SCHEMA_FILE_NAME, name))
        }
        for (BaseCommand command : this.commands) {
            command.validate(context)
        }
        if (outCommand != null) {
            outCommand.validate(context)
        }
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        def responses = execute(context, commands)
        if (outCommand == null) {
            return responses
        }

        List<CommandResponse> outCommandResponses = execute(context, List.of(outCommand))

        // Guaranteed to be non empty
        def response = buildMarkedCommandResponse(outCommandResponses.get(0), ODIN_MARKER_START, ODIN_MARKER_END)
        publishState(outCommandResponses.get(0).getStdOut(), context.getMetadata().getStateConfig(), context.getWorkingDir())
        responses.add(response)

        return responses
    }
}
