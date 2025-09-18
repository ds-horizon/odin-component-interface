package com.dream11

import com.dream11.exec.CommandResponse
import com.dream11.spec.FlavourStage
import com.dream11.state.StateClientFactory
import com.dream11.state.StateConfig
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.jayway.jsonpath.JsonPath
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import groovy.util.logging.Slf4j
import lombok.SneakyThrows
import org.apache.commons.io.FileUtils

import java.nio.charset.Charset
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

import static com.dream11.Constants.STAGE_NAME
import static com.dream11.Constants.UTF_8
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import static java.util.stream.Collectors.toSet

@Slf4j
class OdinUtil {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .withConfigOverride(ArrayNode.class, override -> override.setMergeable(false)) // prevent merging of values with type `ArrayNode`
            .build()

    static ObjectMapper getObjectMapper() {
        return MAPPER
    }

    static void validate(String schemaJson, String jsonData) {
        def factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)

        def jsonSchema = factory.getSchema(schemaJson)
        def jsonNode = MAPPER.readTree(jsonData)

        def errors = jsonSchema.validate(jsonNode)

        if (!errors.isEmpty()) {
            def errorMessages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("\n"))

            throw new IllegalArgumentException(errorMessages)
        }
    }

    static String readFile(File file) {
        try {
            FileUtils.readFileToString(file, Charset.forName(UTF_8))
        } catch (Exception ignored) {
            log.error("Error reading file: ${file.absolutePath}")
            throw new RuntimeException("Error reading file: ${file.absolutePath}")
        }
    }

    /**
     * Merges jsons in the order they are provided. First element is most important and last element is least important.
     * @param jsons List of json strings
     * @return merged json
     */
    static String mergeJsons(List<String> jsons) {
        if (jsons.isEmpty()) {
            log.error("No jsons to merge")
            throw new NoSuchElementException("No jsons to merge")
        }
        String result = jsons.get(jsons.size() - 1)
        for (int i = jsons.size() - 2; i >= 0; i--) {
            JsonNode node = MAPPER.readValue(result, JsonNode.class)
            result = MAPPER.readerForUpdating(node).readValue(jsons.get(i))
        }
        return result
    }

    static void logErrorAndExit(String msg) {
        log.error(msg)
        System.exit(1)
    }

    static void logErrorAndExit(Exception e) {
        if (e.getMessage() == null) {
            logErrorAndExit(Arrays.stream(e.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n")))
        } else {
            logErrorAndExit(e.getMessage())
        }
    }

    /**
     * Gets object from a json using json paths. First json path is applied first and subsequent json paths are applied on the result
     * @param root Json string
     * @param jsonPath List of json paths
     * @return Object at specified json path
     */

    @SneakyThrows
    static Object getUsingJsonPath(String root, String... jsonPaths) {
        String searchJson = root
        Object result = null
        for (String jsonPath : jsonPaths) {
            result = JsonPath.read(searchJson, jsonPath)
            searchJson = MAPPER.writeValueAsString(result)
        }
        return result
    }

    static void mustExistProperty(Supplier<Boolean> fn, String className, String propertyName) {
        if (fn.get()) {
            log.error("${className} is missing ${propertyName}")
            throw new IllegalArgumentException("${className} is missing ${propertyName}")
        }
    }

    static void logExportableCommandResponses(List<CommandResponse> commandResponses) {
        commandResponses
                .stream()
                .filter(resp -> resp.getExportable())
                .forEach(resp -> {
                    if (resp.hasError()) {
                        log.error("Failed to execute: " + resp.getCommand())
                        log.error(resp.getStdErr())
                    } else if (resp.getStdOut() != null && resp.getStdOut() != "") {
                        log.debug(resp.getStdOut())
                    }
                })
    }

    static boolean isSuccessfulExecution(List<CommandResponse> commandResponses) {
        return commandResponses
                .stream()
                .filter(OdinUtil::isFailedExecution)
                .findFirst()
                .isEmpty()
    }

    static boolean isFailedExecution(CommandResponse commandResponse) {
        return commandResponse.hasError()
    }

    static CommandResponse buildMarkedCommandResponse(CommandResponse commandResponse, String startMarker, String endMarker) {
        final CommandResponse markedCommandResponse

        StringBuilder markedRespBuilder = new StringBuilder(startMarker)

        if (commandResponse.hasError()) {
            markedRespBuilder.append(commandResponse.getStdErr())
            markedRespBuilder.append(endMarker)
            markedCommandResponse = new CommandResponse(commandResponse.getCommand(), commandResponse.getStdOut(),
                markedRespBuilder.toString(), commandResponse.getExportable(), 1)
        } else {
            markedRespBuilder.append(commandResponse.getStdOut())
            markedRespBuilder.append(endMarker)
            markedCommandResponse = new CommandResponse(commandResponse.getCommand(), markedRespBuilder.toString(),
                commandResponse.getStdErr(), commandResponse.getExportable(), 0)
        }
        return markedCommandResponse
    }

    static Set<PosixFilePermission> getPostfixPermissions() {
        return Stream.of(OWNER_WRITE, OWNER_READ, OWNER_EXECUTE,
                GROUP_WRITE, GROUP_READ, GROUP_EXECUTE,
                OTHERS_WRITE, OTHERS_READ, OTHERS_EXECUTE)
                .collect(toSet())
    }

    static joinPath(String... paths) {
        return paths.join(File.separator)
    }

    static isJsonFile(File file) {
        return isJson(FileUtils.readFileToString(file, Charset.forName(UTF_8)))
    }

    static isJson(String json) {
        try {
            MAPPER.readValue(json, Map.class)
            return true
        } catch (JsonProcessingException e) {
            log.error("Invalid json: ${e.getMessage()}", e)
            return false
        }
    }

    static String getContentBetweenMarkers(String content, String startMarker, String endMarker) {
        String regex = String.format("(%s)(.*?)(%s)", startMarker, endMarker)
        Pattern pattern = Pattern.compile(regex)
        Matcher matcher = pattern.matcher(content)

        if (matcher.find()) {
            return matcher.group(2)
        } else {
            log.warn("No string found between markers {} and {}", startMarker, endMarker)
            return ""
        }
    }

    static void writeToFile(String content, String filePath) {
        log.debug("Writing content to file ${filePath}")
        def file = new File(filePath)
        file.text = content
    }

    static void publishState(String content, StateConfig stateConfig, String workingDirectory) {
        log.debug("Publishing state to ${stateConfig.getProvider()}")
        writeToFile(content, workingDirectory + "/odin.state")
        StateClientFactory.getStateClient(stateConfig).putState(workingDirectory)
    }

    static boolean isValidatingOperation(ExecutionContext context) {
        return context.getMetadata().isValidating() &&
                context.getMetadata().getConfig().get(STAGE_NAME)
                        .toString().equalsIgnoreCase(FlavourStage.OPERATE.toString())
    }

    static void executeTasks(List<Runnable> tasks) {
        CompletableFuture.allOf(tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, Odin.getExecutorService()))
                .toArray(CompletableFuture[]::new))
                .join()
    }
}
