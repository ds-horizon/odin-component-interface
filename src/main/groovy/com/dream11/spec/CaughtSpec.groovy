package com.dream11.spec

import com.dream11.CaughtContext
import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.mustExistProperty

/**
 * Caught is synonymous to catch block C, Java provides. Can't use 'catch' since its a reserved keyword in groovy.
 * */
@Slf4j
class CaughtSpec extends EnclosedByFlavour implements Spec {

    private RunCommandSpec command
    private final DeploySpec deploySpec
    private boolean preWarmed
    private CaughtContext caughtContext

    CaughtSpec(FlavourSpec flavour) {
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
        if (!isPreWarmed()) {
            throw new IllegalStateException("Caught block needs to be pre warmed before execution.")
        }
        def stage = caughtContext.getStage()
        resetPreWarming()
        this.command.setCommand(this.command.getCommand() + " " + stage)
        return execute(context, List.of(this.command))
    }

    void preWarm(CaughtContext caughtContext) {
        preWarmed = true
        this.caughtContext = caughtContext
    }

    private void resetPreWarming() {
        preWarmed = false
        caughtContext = null
    }

    private boolean isPreWarmed() {
        return preWarmed && caughtContext != null
    }

    BaseCommand getCommand() {
        return command
    }
}
