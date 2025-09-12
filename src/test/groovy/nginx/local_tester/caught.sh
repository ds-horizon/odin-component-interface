echo "*****Running caught*****"
if [ "$1" = 'error' ]; then
  echo "[Caught Block] Intentionally exiting with error (Triggered because block [$2] failed)" >>/dev/stderr
  exit 1
else
  echo "[Caught Block] Exiting with success (Triggered because block [$2] failed)"
fi
