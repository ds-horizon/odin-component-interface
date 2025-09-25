#!/bin/bash
# Common environment configuration for all nginx test scripts

# Determine test mode (local or aws)
TEST_MODE="${ODIN_TEST_MODE:-aws}"

# Get S3 bucket name (default if not set)
S3_BUCKET="${ODIN_S3_BUCKET:-odin-components-state}"

if [ "${TEST_MODE}" = "aws" ]; then
    # Check if AWS profile is set for AWS mode
    if [ -z "${AWS_PROFILE:-}" ]; then
        echo "Error: AWS_PROFILE environment variable is not set" >&2
        echo "Please set AWS_PROFILE before running this script" >&2
        echo "Or run: ../../../setup-env.sh local" >&2
        exit 1
    fi
    S3_ENDPOINT="https://s3.us-east-1.amazonaws.com"
else
    # Local mode using LocalStack
    echo "Running in LocalStack mode"
    S3_ENDPOINT="http://localhost:4566"
    # Set dummy credentials for LocalStack if not already set
    export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
    export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
    export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
fi

# Export for use in scripts
export S3_ENDPOINT
export S3_BUCKET
