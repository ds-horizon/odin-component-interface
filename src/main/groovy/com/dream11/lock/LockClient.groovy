package com.dream11.lock

interface LockClient {
    boolean acquireStateLock();

    boolean releaseStateLock();
}
