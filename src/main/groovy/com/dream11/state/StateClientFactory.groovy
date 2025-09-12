package com.dream11.state

class StateClientFactory {
    static StateClient getStateClient(StateConfig stateConfig) {
        switch (StateProvider.valueOf(stateConfig.getProvider().toUpperCase())) {
            case StateProvider.S3:
                return new S3Client(stateConfig)
            default:
                throw new IllegalArgumentException("Unsupported provider ${stateConfig.getProvider()}")
        }
    }
}
