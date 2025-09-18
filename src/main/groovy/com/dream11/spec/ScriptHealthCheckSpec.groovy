package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import com.dream11.exec.RunCommandExecutor
import com.dream11.exec.RunCommandRequest
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.isFailedExecution
import static com.dream11.OdinUtil.joinPath
import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class ScriptHealthCheckSpec extends BaseHealthCheck {

    private String filePath

    ScriptHealthCheckSpec(FlavourSpec flavour) {
        super(flavour)
    }

    void filePath(String scriptPath) {
        this.filePath = scriptPath
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> filePath == null,
                "Script health check block in ${getFlavour().getFlavour()} flavour", "filePath")
        String filePath = joinPath(context.getMetaWorkingDir(), getFlavour().getFlavour(), filePath.split(" ")[0])
        if (!new File(filePath).exists()) {
            log.error("${filePath} does not exist. Please check relative path.")
            throw new IllegalArgumentException("${filePath} does not exist. Please check relative path.")
        }
    }

    @Override
    protected List<CommandResponse> performHealthCheck(ExecutionContext context, List<String> hosts) {
        log.debug("Performing script healthcheck using ${filePath}")
        RunCommandRequest request = new RunCommandRequest(filePath + " " + String.join(",", hosts), context.getWorkingDir())
        def commandResponse = RunCommandExecutor.execute(request)
        if (!isFailedExecution(commandResponse)) {
            return List.of(commandResponse)
        }
        log.warn("Script healthcheck failed")
        throw new RuntimeException("Script healthcheck failed")
    }

    String getFilePath() {
        return filePath
    }
}
