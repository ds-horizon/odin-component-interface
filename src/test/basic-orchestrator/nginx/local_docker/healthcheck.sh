#!/bin/bash
set -euo pipefail

# Script: healthcheck.sh
# Purpose: Perform health check on deployed nginx component
# Required: AWS_PROFILE environment variable, groovy

# Check if AWS profile is set
if [ -z "${AWS_PROFILE}" ]; then
    echo "Error: AWS_PROFILE environment variable is not set" >&2
    echo "Please set AWS_PROFILE before running this script" >&2
    exit 1
fi

# Navigate to project root
cd ../../../../../ || exit 1
export ODIN_BASE_CONFIG='{"name":"web","internal_port":80}'
export ODIN_DSL_METADATA='{"flavour":"local_docker","stage":"healthcheck","stateConfig":{"provider":"S3","config":{"uri":"s3://odin-components-state-stag/odin-component-interface-nginx-test.tfstate","endpoint":"https://s3.us-east-1.amazonaws.com","region":"us-east-1"}}}'
export ODIN_FLAVOUR_CONFIG='{"external_port":80}'

# Navigate to component directory
cd src/test/groovy/nginx || exit 1

# Verify JAR exists
JAR_PATTERN="../../../../target/odin-component-interface-*-SNAPSHOT-jar-with-dependencies.jar"
# shellcheck disable=SC2086
if ! ls ${JAR_PATTERN} 1> /dev/null 2>&1; then
    echo "Error: JAR file not found matching pattern: ${JAR_PATTERN}" >&2
    exit 1
fi

# Execute component
echo "Executing healthcheck stage..."
# shellcheck disable=SC2086
groovy -cp ${JAR_PATTERN} component.groovy
