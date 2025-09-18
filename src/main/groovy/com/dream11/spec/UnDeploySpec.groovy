package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.OdinUtil
import com.dream11.exec.CommandResponse
import com.dream11.state.StateClientFactory
import groovy.util.logging.Slf4j

import static com.dream11.Constants.ODIN_MARKER_END
import static com.dream11.Constants.ODIN_MARKER_START
import static com.dream11.OdinUtil.mustExistProperty
import static com.dream11.OdinUtil.publishState

@Slf4j
class UnDeploySpec extends EnclosedByFlavour implements Spec {

    private final List<BaseCommand> commands = new ArrayList<>()
    private BaseCommand outCommand

    UnDeploySpec(FlavourSpec flavour) {
        super(flavour)
    }

    void run(String command) {
        this.commands.add(new RunCommandSpec(getFlavour(), command))
    }

    void download(@DelegatesTo(FileDownloadSpec) Closure cl) {
        BaseCommand downloadCommand = new FileDownloadSpec(getFlavour())
        def code = cl.rehydrate(downloadCommand, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        this.commands.add(downloadCommand)
    }

    void out(String out) {
        this.outCommand = new RunCommandSpec(getFlavour(), out, false)
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> commands.isEmpty(), "Undeploy block in ${getFlavour().getFlavour()} flavour", "command")
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

        // Delete state after undeploy is successful
        if (responses.stream().allMatch { !it.hasError() } && outCommand == null) {
            log.debug("Deleting state after successful undeploy")
            StateClientFactory.getStateClient(context.getMetadata().getStateConfig()).deleteState()
        }

        if (outCommand == null) {
            return responses
        }

        List<CommandResponse> outCommandResponses = execute(context, List.of(outCommand))
        // Guaranteed to be non empty
        def response = OdinUtil.buildMarkedCommandResponse(outCommandResponses.get(0), ODIN_MARKER_START, ODIN_MARKER_END)
        publishState(outCommandResponses.get(0).getStdOut(), context.getMetadata().getStateConfig(), context.getWorkingDir())
        responses.add(response)

        return responses
    }

    List<BaseCommand> getCommands() {
        return commands
    }
}
