#!/bin/bash
set -euo pipefail

# Script: sample.sh
# Purpose: Sample test script with signal handling
# Args: $1 - 'error' to simulate failure, anything else for success

trap 'echo SIGTERM from sample script;exit 1' SIGTERM

if [ "${1:-}" = 'error' ]; then
  echo "Intentionally exiting with ${1}" >&2
  exit 1
else
  echo "Intentionally exiting with success"
fi
