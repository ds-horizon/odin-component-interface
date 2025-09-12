package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse
import groovy.util.logging.Slf4j
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils

import static com.dream11.OdinUtil.mustExistProperty
import static com.dream11.spec.HttpMethod.GET

@Slf4j
class HttpHealthCheckSpec extends BaseHealthCheck {
    private String path = "/"
    private HttpMethod method = GET
    private String port
    private String scheme = "http://"
    private String[] headers

    HttpHealthCheckSpec(FlavourSpec flavour) {
        super(flavour)
    }

    // optional
    void scheme(String scheme) {
        this.scheme = scheme
    }

    // optional
    void headers(String[] headers) {
        this.headers = headers
    }

    void port(String port) {
        this.port = port
    }

    void path(String path) {
        this.path = path
    }

    // optional
    void method(HttpMethod method) {
        this.method = method
    }

    @Override
    void validate(ExecutionContext context) {
        if (path.startsWith("http")) {
            log.error("API path is not expected to have http scheme")
            throw new IllegalArgumentException("API path is not expected to have http scheme")
        }

        mustExistProperty(() -> port == null,
                "HTTP Health check in ${getFlavour().getFlavour()} flavour", "port")
    }

    @Override
    protected List<CommandResponse> performHealthCheck(ExecutionContext context, List<String> hosts) {
        return hosts.stream()
                .filter(host -> host != "")
                .map(host -> performHealthCheckInternal(host, context.getMetadata().getHealthCheckConnectionTimeout()))
                .toList()
    }

    private Header[] buildHeaders() {
        if (this.headers == null) {
            return null
        }
        return Arrays.stream(this.headers)
                .map(header -> header.split(":"))
                .filter(kv -> kv.length == 2)
                .map(kv -> new BasicHeader(kv[0], kv[1]))
                .toList()
    }

    private CommandResponse performHealthCheckInternal(String host, int timeout) {
        String apiUrl = buildApiUrl(host)
        Header[] headers = buildHeaders()
        log.debug("Performing Http healthcheck on ${method} ${apiUrl}")
        HttpGet request = new HttpGet(apiUrl)
        request.setHeaders(headers)

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build()

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()
             CloseableHttpResponse response = httpClient.execute(request)) {

            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 299) {

                HttpEntity entity = response.getEntity()
                if (entity != null) {
                    log.debug(EntityUtils.toString(entity))
                }

                log.debug("HTTP healthcheck successful on ${method} ${apiUrl}")

                return new CommandResponse(null, new StringBuilder()
                        .append("HTTP healthcheck successful on ${method} ${apiUrl}").toString(),
                        null, 0)
            } else {
                throw new Exception("Expected 2xx status code, got ${response.getStatusLine().getStatusCode()} instead.")
            }
        } catch (Exception e) {
            log.error("HTTP healthcheck failed: ${e.getMessage()}")
            throw e
        }
    }

    private String buildApiUrl(String host) {
        String url = scheme + host + ":" + port
        return path.startsWith("/") ? url + path : url + "/" + path
    }

    String getPath() {
        return path
    }

    HttpMethod getMethod() {
        return method
    }

    String getPort() {
        return port
    }

    // required by deserializer
    String getScheme() {
        return scheme
    }

    // required by deserializer
    String[] getHeaders() {
        return headers
    }
}
