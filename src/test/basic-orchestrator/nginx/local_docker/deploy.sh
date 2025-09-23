#!/bin/bash
set -euo pipefail

# Script: deploy.sh
# Purpose: Deploy nginx component using local_docker flavour
# Required: AWS_PROFILE environment variable, mvn, groovy

# Check if AWS profile is set
if [ -z "${AWS_PROFILE}" ]; then
    echo "Error: AWS_PROFILE environment variable is not set" >&2
    echo "Please set AWS_PROFILE before running this script" >&2
    exit 1
fi

# Navigate to project root
cd ../../../../../ || exit 1

# Build the project
echo "Building project with Maven..."
if ! mvn clean package; then
    echo "Error: Maven build failed" >&2
    exit 1
fi
export BASE_CONFIG='{"name":"web","internal_port":80}'
export DSL_METADATA='{"flavour":"local_docker","stage":"deploy","stateConfig":{"provider":"S3","config":{"uri":"s3://odin-components-state-stag/odin-component-interface-nginx-test.tfstate","endpoint":"https://s3.us-east-1.amazonaws.com","region":"us-east-1"}}}'
export FLAVOUR_CONFIG='{"external_port":80}'

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
