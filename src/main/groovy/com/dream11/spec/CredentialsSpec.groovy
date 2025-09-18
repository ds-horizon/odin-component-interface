package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

@Slf4j
class CredentialsSpec extends EnclosedByFlavour implements Spec {

    String username
    String password

    CredentialsSpec(FlavourSpec flavour) {
        super(flavour)
    }

    void username(String username) {
        this.username = username
    }

    void password(String password) {
        this.password = password
    }

    @Override
    void validate(ExecutionContext context) {

    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        return List.of(new CommandResponse(null, new StringBuilder()
                .append("Nothing to execute in credential spec. Returning success response")
                .toString(),
                null, 0))
    }
}
