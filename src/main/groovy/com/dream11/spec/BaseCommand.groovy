package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

@Slf4j
abstract class BaseCommand extends EnclosedByFlavour implements Spec {

    BaseCommand(FlavourSpec flavour) {
        super(flavour)
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        return List.of(this.executeCommand(context))
    }

    protected abstract CommandResponse executeCommand(ExecutionContext context);
}
