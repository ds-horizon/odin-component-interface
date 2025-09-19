package com.dream11.state

class StateClientFactory {
    static StateClient getStateClient(StateConfig stateConfig) {
        StateClientConfig clientConfig = stateConfig.getConfig()
        switch (StateProvider.valueOf(stateConfig.getProvider().toUpperCase())) {
            case StateProvider.S3:
                return new S3StateClient(clientConfig)
            default:
                throw new IllegalArgumentException("Unsupported provider ${stateConfig.getProvider()}")
        }
    }
}
