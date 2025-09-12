package com.dream11.spec

import com.dream11.ExecutionContext
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class LinearRetryPolicy implements RetryPolicy {
    Integer counter
    Integer intervalSeconds

    int currentCount

    void count(Integer count) {
        this.counter = count
    }

    void intervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> counter == null, "LinearRetryPolicy", "count")
        mustExistProperty(() -> intervalSeconds == null, "LinearRetryPolicy", "intervalSeconds")

        if (counter < 0 || intervalSeconds < 0) {
            log.error("Incorrect values of counter and/or intervalSeconds in linearRetryPolicy")
            throw new IllegalArgumentException("Incorrect values of counter and/or intervalSeconds in linearRetryPolicy")
        }
    }

    @Override
    boolean canRetry() {
        return currentCount < counter
    }

    @Override
    void retry() {
        if (!canRetry()) {
            throw new IllegalStateException("Retries exhausted")
        }
        currentCount++
        log.debug("Retried {} times", currentCount)
        Thread.sleep(intervalSeconds * 1000)
    }
}
