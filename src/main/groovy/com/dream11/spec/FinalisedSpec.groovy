package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.mustExistProperty

/**
 * Finalised is synonymous to finally block C, Java provides. Can't use 'finally' since its a reserved keyword in groovy.
 * */
@Slf4j
class FinalisedSpec extends EnclosedByFlavour implements Spec {

    private RunCommandSpec command
    private final DeploySpec deploySpec

    FinalisedSpec(FlavourSpec flavour) {
        super(flavour)
        this.deploySpec = deploySpec
    }

    void run(String command) {
        this.command = new RunCommandSpec(getFlavour(), command)
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> command == null, "Caught block in ${getFlavour().getFlavour()} flavour", "command")
        command.validate(context)
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        return execute(context, List.of(command))
    }

    BaseCommand getCommand() {
        return command
    }
}
