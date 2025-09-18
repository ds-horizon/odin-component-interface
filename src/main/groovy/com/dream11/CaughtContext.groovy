package com.dream11

import com.dream11.exec.CommandResponse

class CaughtContext {
    private String stage
    private List<CommandResponse> responses

    CaughtContext(String stage, List<CommandResponse> responses) {
        this.stage = stage
        this.responses = responses
    }

    String getStage() {
        return stage
    }
}
