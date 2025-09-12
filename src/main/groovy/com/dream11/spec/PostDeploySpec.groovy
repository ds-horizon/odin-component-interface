package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class PostDeploySpec extends EnclosedByFlavour implements Spec {

    private final List<BaseCommand> commands = new ArrayList<>()
    private final DeploySpec deploySpec

    PostDeploySpec(FlavourSpec flavour) {
        super(flavour)
        this.deploySpec = deploySpec
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
        mustExistProperty(() -> commands.isEmpty(), "PostDeploy block in ${getFlavour().getFlavour()} flavour", "command")
        for (BaseCommand command : this.commands) {
            command.validate(context)
        }
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        execute(context, commands)
    }

    List<BaseCommand> getCommands() {
        return commands
    }
}
