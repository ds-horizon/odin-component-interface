package com.dream11

import com.dream11.lock.LockConfig
import com.dream11.spec.ComponentSpec
import groovy.util.logging.Slf4j

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static com.dream11.Constants.EXPORT_DSL_DATA_COMMAND
import static com.dream11.Constants.META_WORKING_DIR
import static com.dream11.Constants.TEMPLATE_DIR_PREFIX
import static com.dream11.OdinUtil.isSuccessfulExecution
import static com.dream11.OdinUtil.logErrorAndExit
import static com.dream11.OdinUtil.logExportableCommandResponses
import static com.dream11.OdinUtil.objectMapper
import static java.nio.file.Files.createTempDirectory

/**
 * This is the entry point of the DSL. The lifecycle of DSL is as follows:
 * 1. Parse the DSL and set provided values to Spec classes.
 * 2. Run validator on the specs.
 * 3. Execute the chosen flavour and respective flavour's stage.
 * */
@Slf4j
class Odin {
    private static final ExecutionContext EXECUTION_CONTEXT = build()
    private static ComponentSpec component
    private static boolean executionFinished = false;
    private static ExecutorService executorService = Executors.newFixedThreadPool(Constants.COMMAND_RESP_READER_THREADS)

    static void component(@DelegatesTo(ComponentSpec) Closure cl) {
        addShutdownHook {
            handleShutdown()
        }
        component = new ComponentSpec()
        def code = cl.rehydrate(component, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        System.exit(execute(component))
    }

    private static int execute(ComponentSpec componentSpec) {
        try {
            if (shouldExportData()) {
                log.debug(getObjectMapper().writeValueAsString(componentSpec.getActiveFlavour(EXECUTION_CONTEXT)))
                return 0
            } else {
                componentSpec.validate(EXECUTION_CONTEXT)
                def responses = componentSpec.execute(EXECUTION_CONTEXT)

                logExportableCommandResponses(responses)

                return isSuccessfulExecution(responses) ? 0 : 1
            }
        } catch (Exception e) {
            // all unhandled exceptions
            logErrorAndExit(e)
        } finally {
            executionFinished = true
        }
    }

    private static ExecutionContext build() {
        DslMetadata metadata = getObjectMapper().readValue(BootstrapConfig.getDslMetadata(), DslMetadata.class)
        metadata.validate()

        def workingDir = createTempDirectory(TEMPLATE_DIR_PREFIX)
        return new ExecutionContext(metadata, META_WORKING_DIR, workingDir.toFile().getAbsolutePath())
    }

    private static boolean shouldExportData() {
        return System.getProperty(EXPORT_DSL_DATA_COMMAND) != null && System.getProperty(EXPORT_DSL_DATA_COMMAND).toBoolean()
    }

    static ExecutionContext getExecutionContext() {
        return EXECUTION_CONTEXT
    }

    static ExecutorService getExecutorService() {
        return executorService
    }

    private static void handleShutdown() {
        long startTime = System.nanoTime()
        while (!executionFinished && System.nanoTime() <= startTime + Constants.GRACEFUL_SHUTDOWN_WAIT_TIMEOUT.toNanos()) {
            log.debug("Waiting for execution to finish")
            Thread.sleep(1000)
        }
        executorService.shutdown()
        if (!executionFinished) {
            log.error("Timeout while waiting for execution to finish. Forcing shutdown.")
            LockConfig lockConfig = EXECUTION_CONTEXT.getMetadata().getLockConfig()
            if (lockConfig != null && !EXECUTION_CONTEXT.getMetadata().getOrCreateLockClient().releaseStateLock()) {
                throw new RuntimeException("Failed to release lock for : ${EXECUTION_CONTEXT.getMetadata().getLockConfig().provider()}")
            }
        }
    }
}
