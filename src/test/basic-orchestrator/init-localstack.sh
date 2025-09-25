#!/bin/bash
set -euo pipefail

# Script: init-localstack.sh
# Purpose: Initialize LocalStack S3 with required bucket
# Note: This script runs inside the LocalStack container on startup

echo "Initializing LocalStack S3..."

# Use the default bucket name (can be overridden via environment)
BUCKET_NAME="${ODIN_S3_BUCKET:-odin-components-state}"

# Create the S3 bucket
echo "Creating bucket: ${BUCKET_NAME}"
awslocal s3 mb "s3://${BUCKET_NAME}" || true

# Verify bucket was created
awslocal s3 ls

echo "LocalStack S3 initialization complete"
