package com.dream11.spec

import com.dream11.BootstrapConfig
import com.dream11.DslMetadata
import com.dream11.Odin
import com.dream11.OdinUtil
import com.dream11.state.StateClient
import com.dream11.state.StateClientFactory

import java.nio.file.Path

import static com.dream11.OdinUtil.getObjectMapper

class StateSpec {

    private String lastState

    String getLastState() {
        if (checkLastState()) return this.lastState
        DslMetadata metadata = getObjectMapper().readValue(BootstrapConfig.getDslMetadata(), DslMetadata.class)
        metadata.validate()
        StateClient stateClient = StateClientFactory.getStateClient(metadata.getStateConfig())
        this.lastState = stateClient.getState()
        return this.lastState
    }

    void writeStateToFile(String filePath) {
        String lastState = getLastState()
        Path path = Path.of(Odin.getExecutionContext().getWorkingDir(), filePath)
        if (!lastState.isEmpty()) {
            OdinUtil.writeToFile(lastState, path.toString())
        }
    }

    boolean hasLastState() {
        if (checkLastState()) return true
        this.lastState = getLastState()
        return checkLastState()
    }

    private boolean checkLastState(){
        return this.lastState != null && !this.lastState.isEmpty()
    }
}
