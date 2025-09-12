package com.dream11.lock

import groovy.util.logging.Slf4j
import redis.clients.jedis.Jedis

@Slf4j
class RedisClient implements LockClient {
    private Jedis redisClient
    RedisLockClientConfig redisLockClientConfig
    private final Object redisLock = new Object()
    private String LOCK_VALUE

    RedisClient(RedisLockClientConfig lockConfig) {
        this.redisLockClientConfig = lockConfig
    }

    Jedis getOrCreateRedisClient() {
        if (redisClient == null) {
            synchronized (redisLock) {
                if (redisClient == null) {
                    redisClient = new Jedis(redisLockClientConfig.getClusterEndpoint(), 6379)
                }
            }
        }
        return redisClient
    }

    @Override
    boolean acquireStateLock() {
        String lockValue = UUID.randomUUID().toString()
        long result = getOrCreateRedisClient().setnx(redisLockClientConfig.getKey(), lockValue)

        if (result == 1) {
            LOCK_VALUE = lockValue
            log.debug("Acquired lock for key: ${redisLockClientConfig.getKey()} with value: ${LOCK_VALUE}")
            return true
        } else {
            log.debug("Failed to acquire lock for key: ${redisLockClientConfig.getKey()}")
            return false
        }
    }

    @Override
    boolean releaseStateLock() {
        if(LOCK_VALUE == null) {
            log.debug("Lock value is null. Lock not acquired.")
            return true
        }
        else if (!getOrCreateRedisClient().exists(redisLockClientConfig.getKey())) {
            log.debug("Lock for ${redisLockClientConfig.getKey()} doesn't exist.")
            return true
        }
        else if(LOCK_VALUE != getOrCreateRedisClient().get(redisLockClientConfig.getKey())) {
            log.debug("Redis lock value doesn't match with local lock value: ${LOCK_VALUE}. Lock not acquired.")
            return false
        }
        else {
            log.debug("Lock value matches. Lock was acquired.")

            Long deletedKeysCount = getOrCreateRedisClient().del(redisLockClientConfig.getKey())

            if (deletedKeysCount > 0) {
                log.debug("Lock for ${redisLockClientConfig.getKey()} released successfully.")
                return true
            } else {
                log.debug("Lock for ${redisLockClientConfig.getKey()} doesn't exist.")
                return false
            }
        }
    }
}
