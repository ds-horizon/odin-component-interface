package com.dream11.lock

import groovy.util.logging.Slf4j

@Slf4j
class LockClientFactory {
    static LockClient getLockClient(LockConfig lockConfig) {
        switch (LockProvider.valueOf(lockConfig.provider().toUpperCase())) {
            case LockProvider.REDIS:
                return new RedisClient(LockClientConfigFactory.getLockClientConfig(lockConfig))
            default:
                throw new IllegalArgumentException("Unsupported provider ${lockConfig.provider()}")
        }
    }
}

