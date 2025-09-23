#!/bin/bash
set -euo pipefail

# Script: caught.sh
# Purpose: Exception handler simulation for testing
# Args: $1 - 'error' to propagate failure, anything else for recovery
#       $2 - name of the block that failed

echo "*****Running caught*****"
if [ "${1:-}" = 'error' ]; then
  echo "[Caught Block] Intentionally exiting with error (Triggered because block [${2:-unknown}] failed)" >&2
  exit 1
else
  echo "[Caught Block] Exiting with success (Triggered because block [${2:-unknown}] failed)"
fi
