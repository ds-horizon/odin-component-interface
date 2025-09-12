package com.dream11.lock

class LockClientConfigFactory {
    static LockClientConfig getLockClientConfig(LockConfig lockConfig) {
        switch (LockProvider.valueOf(lockConfig.provider().toUpperCase())) {
            case LockProvider.REDIS:
                return RedisLockClientConfig.fromConfigMap(lockConfig.config())
            default:
                throw new IllegalArgumentException("Unsupported provider ${lockConfig.getProvider()}")
        }
    }

}
