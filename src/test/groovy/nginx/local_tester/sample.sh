#!/bin/bash
trap 'echo SIGTERM from sample script;exit 1' SIGTERM

if [ "$1" = 'error' ]; then
  echo "Intentionally exiting with $1" >>/dev/stderr
  exit 1
else
  echo "Intentionally exiting with success"
fi
