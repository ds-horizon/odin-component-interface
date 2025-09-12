package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import java.util.function.Supplier

import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class HealthCheckSpec extends EnclosedByFlavour implements Spec {
    private HttpHealthCheckSpec httpHealthCheckSpec
    private TcpHealthCheckSpec tcpHealthCheckSpec
    private ScriptHealthCheckSpec scriptHealthCheckSpec
    private LinearRetryPolicy linearRetryPolicy

    HealthCheckSpec(FlavourSpec flavour) {
        super(flavour)
    }

    void linearRetryPolicy(@DelegatesTo(LinearRetryPolicy) Closure cl) {
        this.linearRetryPolicy = new LinearRetryPolicy()
        def code = cl.rehydrate(linearRetryPolicy, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void http(@DelegatesTo(HttpHealthCheckSpec) Closure cl) {
        httpHealthCheckSpec = new HttpHealthCheckSpec(getFlavour())
        def code = cl.rehydrate(httpHealthCheckSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void tcp(@DelegatesTo(TcpHealthCheckSpec) Closure cl) {
        tcpHealthCheckSpec = new TcpHealthCheckSpec(getFlavour())
        def code = cl.rehydrate(tcpHealthCheckSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    void script(@DelegatesTo(ScriptHealthCheckSpec) Closure cl) {
        scriptHealthCheckSpec = new ScriptHealthCheckSpec(getFlavour())
        def code = cl.rehydrate(scriptHealthCheckSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> httpHealthCheckSpec == null && tcpHealthCheckSpec == null && scriptHealthCheckSpec == null, "Health check in ${getFlavour().getFlavour()} flavour", "http or tcp or script health check")
        validateRetryPolicy()

        if (httpHealthCheckSpec != null) {
            httpHealthCheckSpec.validate(context)
        }

        if (tcpHealthCheckSpec != null) {
            tcpHealthCheckSpec.validate(context)
        }

        if (scriptHealthCheckSpec != null) {
            scriptHealthCheckSpec.validate(context)
        }
        linearRetryPolicy.validate(context)
    }

    private void validateRetryPolicy() {
        mustExistProperty(() -> linearRetryPolicy == null, "Health check in ${getFlavour().getFlavour()} flavour", "linearRetryPolicy")
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        List<CommandResponse> responses = new ArrayList<>()

        if (httpHealthCheckSpec != null) {
            responses.addAll(executeWithRetry(() -> httpHealthCheckSpec.execute(context)))
        }

        if (tcpHealthCheckSpec != null) {
            responses.addAll(executeWithRetry(() -> tcpHealthCheckSpec.execute(context)))
        }

        if (scriptHealthCheckSpec != null) {
            responses.addAll(executeWithRetry(() -> scriptHealthCheckSpec.execute(context)))
        }
        return responses
    }

    private static boolean hasError(List<CommandResponse> responses) {
        return responses.stream()
        .filter {commandResponse -> commandResponse.hasError()}
        .findFirst()
        .isPresent()
    }

    private List<CommandResponse> executeWithRetry(Supplier<List<CommandResponse>> commandToBeExecuted) {
        if (linearRetryPolicy == null) {
            return commandToBeExecuted.get()
        }

        while (linearRetryPolicy.canRetry()) {
            List<CommandResponse> responses = commandToBeExecuted.get()
            // here assumption is that healthcheck command never returns error on successful healthcheck
            if (!hasError(responses)) {
                return responses
            }
            linearRetryPolicy.retry()
        }
        log.debug("All retries have exhausted, performing final attempt")
        // Final attempt after all retries are exhausted
        return commandToBeExecuted.get()
    }

    HttpHealthCheckSpec getHttpHealthCheckSpec() {
        return httpHealthCheckSpec
    }

    TcpHealthCheckSpec getTcpHealthCheckSpec() {
        return tcpHealthCheckSpec
    }
}
