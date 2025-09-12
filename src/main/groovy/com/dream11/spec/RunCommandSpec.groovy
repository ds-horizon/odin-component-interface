package com.dream11.spec

import com.dream11.Constants
import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import com.dream11.exec.RunCommandExecutor
import com.dream11.exec.RunCommandRequest


class RunCommandSpec extends BaseCommand {

    private String command
    private Boolean silent

    RunCommandSpec(FlavourSpec flavour) {
        super(flavour)
    }

    RunCommandSpec(FlavourSpec flavour, String command) {
        this(flavour, command, Constants.IS_QUIET)
    }

    RunCommandSpec(FlavourSpec flavour, String command, Boolean silent) {
        this(flavour)
        this.command = command
        this.silent = silent
    }

    @Override
    void validate(ExecutionContext context) {
        if (command == null) {
            log.error("Please provide a command to be executed")
            throw new IllegalArgumentException("Please provide a command to be executed")
        }
    }

    @Override
    protected CommandResponse executeCommand(ExecutionContext context) {
        RunCommandRequest request = new RunCommandRequest(this.command, context.getWorkingDir(), this.silent)
        return RunCommandExecutor.execute(request)
    }

    String getCommand() {
        return command
    }

    void setCommand(command) {
        this.command = command
    }
}
