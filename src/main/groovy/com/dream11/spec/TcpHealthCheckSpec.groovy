package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class TcpHealthCheckSpec extends BaseHealthCheck {

    private String port

    TcpHealthCheckSpec(FlavourSpec flavour) {
        super(flavour)
    }

    void port(String port) {
        this.port = port
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> port == null,
                "TCP Health check in ${getFlavour().getFlavour()} flavour", "port")
    }

    @Override
    protected List<CommandResponse> performHealthCheck(ExecutionContext context, List<String> hosts) {
        return hosts.stream()
                .filter(host -> host != "")
                .map(host -> performHealthCheckInternal(host, context.getMetadata().getHealthCheckConnectionTimeout()))
                .toList()
    }

    private CommandResponse performHealthCheckInternal(String host, int timeout) {
        log.debug("Performing TCP healthcheck on ${host}:${port}")
        SocketAddress socketAddress = new InetSocketAddress(host, Integer.parseInt(port))
        try (Socket socket = new Socket()) {
            socket.connect(socketAddress, timeout * 1000)
            socket.close()

            log.debug("TCP healthcheck successful on ${host}:${port}")
            return new CommandResponse(null, new StringBuilder()
                    .append("TCP healthcheck successful on ${host}:${port}").toString(),
                    null, 0)

        } catch (Exception e) {
            log.debug("TCP healthcheck failed: ${e.getMessage()}")
            throw e
        }
    }

    String getPort() {
        return port
    }
}
