#!/bin/bash
set -euo pipefail

# Script: setup-env.sh
# Purpose: Configure environment for local (LocalStack) or AWS testing
# Usage: ./setup-env.sh [local|aws]

MODE="${1:-}"

if [[ -z "${MODE}" ]]; then
    echo "Usage: $0 [local|aws]"
    echo "  local - Use LocalStack for S3 (no AWS account needed)"
    echo "  aws   - Use real AWS S3 (requires AWS_PROFILE)"
    exit 1
fi

case "${MODE}" in
    local)
        echo "Setting up LocalStack environment..."

        # Start LocalStack if not already running
        if ! docker ps | grep -q odin-localstack; then
            echo "Starting LocalStack..."
            docker compose up -d

            # Wait for LocalStack to be ready
            echo "Waiting for LocalStack to be ready..."
            max_attempts=30
            attempt=0
            while ! curl -sf http://localhost:4566/_localstack/health > /dev/null 2>&1; do
                attempt=$((attempt + 1))
                if [[ ${attempt} -ge ${max_attempts} ]]; then
                    echo "Error: LocalStack failed to start after ${max_attempts} attempts"
                    exit 1
                fi
                echo -n "."
                sleep 2
            done
            echo " Ready!"
        else
            echo "LocalStack is already running"
        fi

        # Export environment variables for LocalStack
        export ODIN_TEST_MODE="local"
        export ODIN_S3_BUCKET="odin-components-state"
        export AWS_ACCESS_KEY_ID="test"
        export AWS_SECRET_ACCESS_KEY="test"
        export AWS_DEFAULT_REGION="us-east-1"
        # Note: AWS_PROFILE is not needed for LocalStack mode

        echo "LocalStack environment configured successfully"
        echo "You can now run test scripts without AWS credentials"
        echo "Using S3 bucket: odin-components-state"
        ;;

    aws)
        echo "Setting up AWS environment..."

        # Check if AWS_PROFILE is set
        if [[ -z "${AWS_PROFILE:-}" ]]; then
            echo "Error: AWS_PROFILE environment variable is not set"
            echo "Please set AWS_PROFILE before running in AWS mode"
            exit 1
        fi

        # Verify AWS credentials are configured
        if ! aws sts get-caller-identity > /dev/null 2>&1; then
            echo "Error: Unable to authenticate with AWS using profile: ${AWS_PROFILE}"
            echo "Please check your AWS credentials"
            exit 1
        fi

        # Check if default bucket exists
        DEFAULT_BUCKET="odin-components-state"
        BUCKET_TO_USE=""

        echo "Checking for S3 bucket: ${DEFAULT_BUCKET}"
        if aws s3api head-bucket --bucket "${DEFAULT_BUCKET}" 2>/dev/null; then
            echo "✓ Bucket '${DEFAULT_BUCKET}' exists and is accessible"
            BUCKET_TO_USE="${DEFAULT_BUCKET}"
        else
            echo "✗ Bucket '${DEFAULT_BUCKET}' not found or not accessible"
            echo ""
            echo "Please provide an existing S3 bucket name to use for state storage,"
            echo "or press Enter to create '${DEFAULT_BUCKET}' (requires permissions):"
            read -r USER_BUCKET

            if [[ -z "${USER_BUCKET}" ]]; then
                # Try to create the default bucket
                echo "Attempting to create bucket '${DEFAULT_BUCKET}'..."
                if aws s3 mb "s3://${DEFAULT_BUCKET}" 2>/dev/null; then
                    echo "✓ Successfully created bucket '${DEFAULT_BUCKET}'"
                    BUCKET_TO_USE="${DEFAULT_BUCKET}"
                else
                    echo "✗ Failed to create bucket. Please provide an existing bucket name:"
                    read -r USER_BUCKET
                    if [[ -z "${USER_BUCKET}" ]]; then
                        echo "Error: No bucket name provided"
                        exit 1
                    fi
                    BUCKET_TO_USE="${USER_BUCKET}"
                fi
            else
                BUCKET_TO_USE="${USER_BUCKET}"
            fi

            # Verify the provided bucket exists
            if ! aws s3api head-bucket --bucket "${BUCKET_TO_USE}" 2>/dev/null; then
                echo "Error: Bucket '${BUCKET_TO_USE}' not found or not accessible"
                exit 1
            fi
            echo "✓ Using bucket: ${BUCKET_TO_USE}"
        fi

        export ODIN_TEST_MODE="aws"
        export ODIN_S3_BUCKET="${BUCKET_TO_USE}"

        echo "AWS environment configured successfully"
        echo "Using AWS profile: ${AWS_PROFILE}"
        echo "Using S3 bucket: ${BUCKET_TO_USE}"
        ;;

    *)
        echo "Error: Invalid mode '${MODE}'"
        echo "Valid modes are: local, aws"
        exit 1
        ;;
esac

# Print environment summary
echo ""
echo "====================================="
echo "Environment Summary:"
echo "  Mode: ${ODIN_TEST_MODE}"
echo "  S3 Bucket: ${ODIN_S3_BUCKET}"
if [ "${ODIN_TEST_MODE}" = "local" ]; then
    echo "  S3 Endpoint: http://localhost:4566"
    echo "  AWS_ACCESS_KEY_ID: test"
    echo "  AWS_SECRET_ACCESS_KEY: test"
else
    echo "  S3 Endpoint: https://s3.us-east-1.amazonaws.com"
    echo "  AWS_PROFILE: ${AWS_PROFILE}"
fi
echo "====================================="
echo ""
echo "Export the following environment variables in your shell:"
if [ "${ODIN_TEST_MODE}" = "local" ]; then
    echo "  export ODIN_TEST_MODE=\"local\""
    echo "  export ODIN_S3_BUCKET=\"${ODIN_S3_BUCKET}\""
    echo "  export AWS_ACCESS_KEY_ID=\"test\""
    echo "  export AWS_SECRET_ACCESS_KEY=\"test\""
    echo "  export AWS_DEFAULT_REGION=\"us-east-1\""
else
    echo "  export ODIN_TEST_MODE=\"aws\""
    echo "  export ODIN_S3_BUCKET=\"${ODIN_S3_BUCKET}\""
    echo "  export AWS_PROFILE=\"${AWS_PROFILE}\""
fi
echo ""
echo "To run tests:"
echo "  cd nginx/local_docker"
echo "  ./deploy.sh"
