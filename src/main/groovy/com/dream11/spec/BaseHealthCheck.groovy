package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.OdinUtil
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.Constants.IS_STRICT_HEALTH_CHECKING
import static com.dream11.Constants.ODIN_DISCOVERY_MARKER_END
import static com.dream11.Constants.ODIN_DISCOVERY_MARKER_START
import static com.dream11.OdinUtil.getContentBetweenMarkers

@Slf4j
abstract class BaseHealthCheck extends EnclosedByFlavour implements Spec {

    BaseHealthCheck(FlavourSpec flavour) {
        super(flavour)
    }

    @Override
    List<CommandResponse> execute(ExecutionContext context) {
        List<String> hosts = new ArrayList<>()
        List<CommandResponse> healthcheckCommandResponse = new ArrayList<>()
        if (getFlavour().getDeploySpec().getDiscoverySpec() != null) {
            CommandResponse discoveryCommandResponse = executeDiscoveryCommand(context)
            hosts.addAll(getHosts(discoveryCommandResponse.getStdOut()))
            healthcheckCommandResponse.add(discoveryCommandResponse)
        }
        if (!IS_STRICT_HEALTH_CHECKING && hosts.size() == 0) {
            String warnMessage = "Skipping health checks as no hosts found and strict health checking is disabled"
            log.warn(warnMessage)
            healthcheckCommandResponse.add(new CommandResponse("Healthcheck", warnMessage, "", 0))
        }
        try {
            healthcheckCommandResponse.addAll(performHealthCheck(context, hosts))
            return healthcheckCommandResponse
        } catch (Exception ignored) {
            healthcheckCommandResponse.add(new CommandResponse("Healthcheck", null, "Giving up on healthcheck after ${context.getMetadata().getRetryMaxAttempts()} retries", 1))
            return healthcheckCommandResponse
        }
    }

    protected abstract List<CommandResponse> performHealthCheck(ExecutionContext context, List<String> hosts);

    private CommandResponse executeDiscoveryCommand(ExecutionContext context) {
        CommandResponse response = getFlavour().getDeploySpec()
                .getDiscoverySpec()
                .execute(context)
        // execute never returns empty list. It either returns list with single element or throws exception
        // hence direct index access is safe here
                .get(0)

        if (response.hasError()) {
            throw new IllegalStateException(response.getStdErr())
        }

        if (response.getStdOut().split("\n").size() != 4) {
            throw new IllegalStateException("Output of discovery block must be a single line containing a json or comma separated string")
        }
        return response

    }

    protected static List<String> getHosts(String discoveryOutput) {
        String output = getContentBetweenMarkers(discoveryOutput, ODIN_DISCOVERY_MARKER_START, ODIN_DISCOVERY_MARKER_END)

        if (output == null || output == "") {
            return new String[0]
        }

        if (output.startsWith("{")) {
            Map<String, String> addresses = OdinUtil.getObjectMapper().readValue(output, Map.class)
            // split the comma separated values in case of multiple addresses
            return addresses.values().collect({ it.toString().split(',') }).flatten()
        }

        return output.split(",")
    }
}
