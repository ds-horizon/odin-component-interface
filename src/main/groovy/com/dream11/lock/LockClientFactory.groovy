package com.dream11.lock

import groovy.util.logging.Slf4j

@Slf4j
class LockClientFactory {
    static LockClient getLockClient(LockConfig lockConfig) {
        LockClientConfig clientConfig = lockConfig.getConfig()
        switch (LockProvider.valueOf(lockConfig.getProvider().toUpperCase())) {
            case LockProvider.REDIS:
                return new RedisLockClient(clientConfig)
            default:
                throw new IllegalArgumentException("Unsupported provider ${lockConfig.getProvider()}")
        }
    }
}
