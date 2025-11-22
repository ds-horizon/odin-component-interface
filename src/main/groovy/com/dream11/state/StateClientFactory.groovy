package com.dream11.state

class StateClientFactory {
    static StateClient getStateClient(StateConfig stateConfig, boolean singleton = true) {
        StateClientConfig clientConfig = stateConfig.getConfig()
        switch (StateProvider.valueOf(stateConfig.getProvider().toUpperCase())) {
            case StateProvider.S3:
                return singleton ? S3StateClient.getInstance(clientConfig)
                        : S3StateClient.createNew(clientConfig)
            default:
                throw new IllegalArgumentException("Unsupported provider ${stateConfig.getProvider()}")
        }
    }
}
