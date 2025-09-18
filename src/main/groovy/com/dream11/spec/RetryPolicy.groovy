package com.dream11.spec

import com.dream11.ExecutionContext

interface RetryPolicy {
    void validate(ExecutionContext context)

    boolean canRetry()

    void retry()
}
