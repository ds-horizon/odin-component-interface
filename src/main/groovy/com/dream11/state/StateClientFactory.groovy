package com.dream11.state

class StateClientFactory {
    static StateClient getStateClient(StateConfig stateConfig) {
        StateClientConfig clientConfig = stateConfig.getConfig()
        switch (StateProvider.valueOf(stateConfig.getProvider().toUpperCase())) {
            case StateProvider.S3:
                return S3StateClient.getInstance(clientConfig)
            default:
                throw new IllegalArgumentException("Unsupported provider ${stateConfig.getProvider()}")
        }
    }
}
