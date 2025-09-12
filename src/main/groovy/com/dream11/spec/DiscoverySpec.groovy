package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.OdinUtil
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.Constants.ODIN_DISCOVERY_MARKER_END
import static com.dream11.Constants.ODIN_DISCOVERY_MARKER_START

/**
 * Discovery spec represents how the underlying component will be discovered.
 * In general discovery is usually of 2 types. IP based and DNS based.
 * */
@Slf4j
class DiscoverySpec extends EnclosedByFlavour implements Spec {

    private final List<BaseCommand> commands = new ArrayList<>()

    DiscoverySpec(FlavourSpec flavour) {
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

    @Override
    void validate(ExecutionContext context) {
        if (commands.isEmpty()) {
            log.error("Please provide a commands to be ran to extract discovery value")
            throw new IllegalArgumentException("Please provide a commands to be ran to extract discovery value")
        }
        for (BaseCommand command : this.commands) {
            command.validate(context)
        }
        if (commands.last() !instanceof RunCommandSpec) {
            log.error("Last command in discovery block has to be a run command")
            throw new IllegalArgumentException("Last command in discovery block has to be a run command")
        }
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        def responses = commands.stream()
                .limit(commands.size() - 1)
                .map(command -> command.execute(context).get(0)) // Guaranteed to contain at least 1 element
                .toList()

        def hasError = responses.stream()
                .filter(resp -> resp.hasError())
                .findFirst()

        if (hasError.isPresent()) {
            return responses
        }
        List<CommandResponse> lastCommandResponse = commands.get(commands.size() - 1).execute(context)
        lastCommandResponse.get(0).setExportable(true)

        return List.of(OdinUtil.buildMarkedCommandResponse(lastCommandResponse.get(0), ODIN_DISCOVERY_MARKER_START, ODIN_DISCOVERY_MARKER_END))
    }

    // required by deserializer
    List<BaseCommand> getCommands() {
        return commands
    }
}
