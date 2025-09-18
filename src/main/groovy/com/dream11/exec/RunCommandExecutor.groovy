package com.dream11.exec

import com.dream11.Constants
import com.dream11.OdinUtil
import groovy.util.logging.Slf4j
import org.slf4j.MDC
import org.slf4j.event.Level

import java.util.function.Consumer
import java.util.stream.Collectors

@Slf4j
class RunCommandExecutor {
    private static final Map<String, Level> MARKER_LEVEL_MAP = Map.of(Constants.LOG_ERROR_MARKER, Level.ERROR,
            Constants.LOG_WARN_MARKER, Level.WARN,
            Constants.LOG_INFO_MARKER, Level.INFO,
            Constants.LOG_DEBUG_MARKER, Level.DEBUG
    )

    static CommandResponse execute(RunCommandRequest request) {
        log.debug("Executing [${request.getCommand().replace('\n', ' ')}], working dir [${request.getWorkingDir()}]")
        String[] bashCmd = new String[]{"/bin/bash", "-c", request.getCommand()}
        ProcessBuilder pb = new ProcessBuilder(bashCmd)
        pb.directory(new File(request.getWorkingDir()))
        Process proc = pb.start()
        StringBuilder output = new StringBuilder()
        StringBuilder error = new StringBuilder()
        OdinUtil.executeTasks(List.of(() -> readFromStream(proc.getInputStream(), request.isSilent(), output), () -> readFromStream(proc.getErrorStream(), false, error)))
        proc.waitFor() // wait for process to complete
        return new CommandResponse(request.getCommand(), output.toString(), error.toString(), proc.exitValue())
    }

    private static void readFromStream(InputStream stream, boolean silent, StringBuilder resultCollector) {
        List<String> lines = new ArrayList<>()
        String line
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while ((line = reader.readLine()) != null) {
                if (!silent) {
                    logUsingMarkerLevel(line)
                }
                lines.add(line)
            }
        }
        resultCollector.append(lines.stream().collect(Collectors.joining("\n")))
    }

    private static void logUsingMarkerLevel(String line) {
        MDC.put("logFormat", "noDate")
        for (Map.Entry<String, Level> entry : MARKER_LEVEL_MAP.entrySet()) {
            String marker = entry.getKey()
            Level level = entry.getValue()

            if (line.contains(marker)) {
                logWithLevel(level, extractLogMessage(line, marker))
                MDC.remove("logFormat")
                return
            }
        }
        // default
        log.debug(line)
        MDC.remove("logFormat")
    }

    private static void logWithLevel(Level level, String message) {
        Consumer<String> logger

        switch (level) {
            case Level.ERROR:
                logger = log::error
                break
            case Level.WARN:
                logger = log::warn
                break
            case Level.INFO:
                logger = log::info
                break
            case Level.DEBUG:
                logger = log::debug
                break
            default:
                logger = log::debug
                break
        }

        logger.accept(message)
    }

    private static String extractLogMessage(String line, String marker) {
        return line.substring(line.indexOf(marker) + marker.length(), line.length())
    }
}
