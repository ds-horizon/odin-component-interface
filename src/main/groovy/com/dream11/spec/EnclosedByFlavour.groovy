package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j

/**
 * It is to be inherited by Specs that will be enclosed within flavour block in the DSL.
 * For example deploy, discovery, healthcheck etc. blocks.
 * */
@Slf4j
class EnclosedByFlavour extends StateSpec {

    @JsonIgnore
    private FlavourSpec flavour

    EnclosedByFlavour(FlavourSpec flavour) {
        this.flavour = flavour
    }

    Object data(String... jsonPath) {
        return flavour.data(jsonPath)
    }

    String getBaseConfigWithDefaults(){
        return flavour.getBaseConfigWithDefaults()
    }

    String getFlavourConfigWithDefaults(){
        return flavour.getFlavourConfigWithDefaults()
    }

    String getOperationConfigWithDefaults(){
        return flavour.getOperationConfigWithDefaults()
    }

    FlavourSpec getFlavour() {
        return flavour
    }

    static List<CommandResponse> execute(ExecutionContext context, List<BaseCommand> providedCommands) {
        List<CommandResponse> responses = new ArrayList<>()

        for (BaseCommand command : providedCommands) {
            try {
                def resp = command.execute(context).get(0) // guaranteed to have 1 element
                responses.add(resp)
                if (resp.hasError()) {
                    return responses
                }
            } catch (Exception e) {
                log.error("Error while executing command:{}", command, e)
                throw e
            }
        }
        return responses
    }
}
