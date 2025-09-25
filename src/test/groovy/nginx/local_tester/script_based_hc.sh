#!/bin/bash
set -euo pipefail

# Script: script_based_hc.sh
# Purpose: Mock health check with configurable error simulation
# Args: $1 - 'error' to simulate failure, anything else for success
#       $2 - target identifier for health check

if [ "${1:-}" = 'error' ]; then
  echo "Intentionally exiting with ${1} code 1" >&2
  exit 1
else
  echo "Mock: Performing script based health check on ${2:-unknown}"
fi
