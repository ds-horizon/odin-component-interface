#!/bin/bash
# Check if AWS profile is set
if [ -z "${AWS_PROFILE}" ]; then
    echo "Error: AWS_PROFILE environment variable is not set" >&2
    echo "Please set AWS_PROFILE before running this script" >&2
    exit 1
fi

cd ../../../../../

export BASE_CONFIG='{"name":"web","internal_port":80}'
export DSL_METADATA='{"flavour":"local_docker","stage":"validate","stateConfig":{"provider":"S3","config":{"uri":"s3://odin-components-state-stag/odin-component-interface-nginx-test.tfstate","endpoint":"https://s3.us-east-1.amazonaws.com","region":"us-east-1"}}, "config": {"stageName": "deploy"}}'
export FLAVOUR_CONFIG='{"external_port":8765}'
export OPERATION_CONFIG='{"count":2}'

cd src/test/groovy/nginx || exit
groovy -cp ../../../../target/odin-component-interface-*-SNAPSHOT-jar-with-dependencies.jar component.groovy
