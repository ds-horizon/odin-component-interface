package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.OdinUtil
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.Constants.ODIN_MARKER_END
import static com.dream11.Constants.ODIN_MARKER_START
import static com.dream11.OdinUtil.mustExistProperty
import static com.dream11.OdinUtil.publishState

@Slf4j
class DeploySpec extends EnclosedByFlavour implements Spec {

    private final List<BaseCommand> commands = new ArrayList<>()
    private BaseCommand outCommand

    private DiscoverySpec discovery

    DeploySpec(FlavourSpec flavour) {
        super(flavour)
    }

    void run(String command) {
        this.commands.add(new RunCommandSpec(getFlavour(), command))
    }

    void out(String command) {
        this.outCommand = new RunCommandSpec(getFlavour(), command, true)
    }

    void discovery(@DelegatesTo(DiscoverySpec) Closure cl) {
        discovery = new DiscoverySpec(getFlavour())
        def code = cl.rehydrate(discovery, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void download(@DelegatesTo(FileDownloadSpec) Closure cl) {
        BaseCommand downloadCommand = new FileDownloadSpec(getFlavour())
        def code = cl.rehydrate(downloadCommand, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        this.commands.add(downloadCommand)
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> commands.isEmpty(), "Deploy block in ${getFlavour().getFlavour()} flavour", "command")
        for (BaseCommand command : this.commands) {
            command.validate(context)
        }
        if (outCommand != null) {
            outCommand.validate(context)
        }
        if (discovery != null) {
            discovery.validate(context)
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
        def response = OdinUtil.buildMarkedCommandResponse(outCommandResponses.get(0), ODIN_MARKER_START, ODIN_MARKER_END)
        publishState(outCommandResponses.get(0).getStdOut(), context.getMetadata().getStateConfig(), context.getWorkingDir())
        responses.add(response)

        return responses
    }

    DiscoverySpec getDiscoverySpec() {
        return discovery
    }

    List<BaseCommand> getCommands() {
        return commands
    }

    DiscoverySpec getDiscovery() {
        return discovery
    }
}
