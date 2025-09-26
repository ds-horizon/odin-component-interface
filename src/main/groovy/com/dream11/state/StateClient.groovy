package com.dream11.state

interface StateClient {
    String getState()

    void putState(String workingDirectory)

    void deleteState()
}
