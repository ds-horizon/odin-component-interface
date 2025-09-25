#!/bin/bash
set -euo pipefail

# Script: deploy.sh
# Purpose: Deploy nginx component using local_docker flavour
# Required: AWS_PROFILE environment variable, mvn, groovy

# Source common environment configuration
# shellcheck source=/dev/null
source ./common-env.sh

# Navigate to project root
cd ../../../../../ || exit 1

# Build the project
echo "Building project with Maven..."
if ! mvn clean package; then
    echo "Error: Maven build failed" >&2
    exit 1
fi
export ODIN_BASE_CONFIG='{"name":"web","internal_port":80}'
if [ "${TEST_MODE}" = "local" ]; then
    export ODIN_DSL_METADATA='{"flavour":"local_docker","stage":"deploy","stateConfig":{"provider":"S3","config":{"uri":"s3://'"${S3_BUCKET}"'/odin-component-interface-nginx-test.tfstate","endpoint":"'"${S3_ENDPOINT}"'","region":"us-east-1","forcePathStyle":true}}}'
else
    export ODIN_DSL_METADATA='{"flavour":"local_docker","stage":"deploy","stateConfig":{"provider":"S3","config":{"uri":"s3://'"${S3_BUCKET}"'/odin-component-interface-nginx-test.tfstate","endpoint":"'"${S3_ENDPOINT}"'","region":"us-east-1","forcePathStyle":false}}}'
fi
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
echo "Executing deploy stage..."
# shellcheck disable=SC2086
groovy -cp ${JAR_PATTERN} component.groovy
