package com.dream11

import java.time.Duration

class Constants {
    public static final String DSL_NAME = "component.groovy"
    public static final String META_WORKING_DIR = "."
    public static final String OPERATIONS_DIR = "operations"

    public final static String EXPORT_DSL_DATA_COMMAND = "exportDslData"
    public final static String ODIN_MARKER_START = "\n------ODIN-MARKER-START------\n"
    public final static String ODIN_MARKER_END = "\n------ODIN-MARKER-END------"
    public final static String ODIN_DISCOVERY_MARKER_START = "\n------ODIN-DISCOVERY-MARKER-START------\n"
    public final static String ODIN_DISCOVERY_MARKER_END = "\n------ODIN-DISCOVERY-MARKER-END------"

    public final static String ODIN_DSL_METADATA = "ODIN_DSL_METADATA"
    public final static String ODIN_BASE_CONFIG = "ODIN_BASE_CONFIG"
    public final static String ODIN_FLAVOUR_CONFIG = "ODIN_FLAVOUR_CONFIG"
    public final static String ODIN_OPERATION_CONFIG = "ODIN_OPERATION_CONFIG"
    public final static String STAGE_NAME = "stageName"
    public final static String ODIN_COMPONENT_METADATA = "ODIN_COMPONENT_METADATA"
    public final static String DEFAULT_DATA_FILE_NAME = "defaults.json"
    public final static String DEFAULT_SCHEMA_FILE_NAME = "schema.json"
    public final static String TEMPLATE_IGNORE_FILE_NAME = ".templateignore"

    public final static int RETRY_MAX_ATTEMPTS = 5
    public final static int HEALTHCHECK_CONNECTION_TIMEOUT = 5
    public final static Boolean IS_STRICT_HEALTH_CHECKING = Boolean.parseBoolean(System.getProperty("strictHealthCheck", "false"))

    public final static String TEMPLATE_DIR_PREFIX = "templates-"
    public final static String UTF_8 = "utf-8"

    public final static Boolean IS_QUIET = System.getenv("QUIET") == null ? false : Boolean.parseBoolean(System.getenv("QUIET"))

    public final static String STAGE_PRE_DEPLOY = "preDeploy"
    public final static String STAGE_DEPLOY = "deploy"
    public final static String STAGE_POST_DEPLOY = "postDeploy"
    public final static String STAGE_HEALTHCHECK = "healthcheck"
    public final static String STAGE_UNDEPLOY = "unDeploy"
    public final static String STAGE_OPERATE = "operate"

    public final static String OPERATION_NAME = "operationName"
    public final static String LOG_ERROR_MARKER = "::error::"
    public final static String LOG_INFO_MARKER = "::info::"
    public final static String LOG_DEBUG_MARKER = "::debug::"
    public final static String LOG_WARN_MARKER = "::warn::"
    public final static Duration GRACEFUL_SHUTDOWN_WAIT_TIMEOUT = Duration.ofSeconds(5)
    public static final int COMMAND_RESP_READER_THREADS = 2
}
