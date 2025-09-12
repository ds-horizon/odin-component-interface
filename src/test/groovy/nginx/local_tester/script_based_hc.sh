if [ "$1" = 'error' ]; then
  echo "Intentionally exiting with $1 code 1" >>/dev/stderr
  exit 1
else
  echo "Mock: Performing script based health check on $2"
fi
