#!/usr/bin/env bash
set -eo pipefail

cleanup() {
  exit_code=$?
  while ! pgrep -x dockerd; do
    sleep 1;
  done;
  pkill -SIGTERM -x dockerd;
  exit $exit_code
}

trap 'cleanup' EXIT
trap 'echo Exiting gracefully, sending SIGTERM to components; kill 0; wait; exit 1' SIGTERM SIGINT

configure_gcp_service_account() {
  KEY_FILE=~/key.json
  aws secretsmanager get-secret-value --secret-id "gcp_serviceaccount_$1" | xargs -0 python3 -c "import sys, json; print(json.loads(sys.argv[1])['SecretString'])" | cat > ${KEY_FILE}
  gcloud auth activate-service-account --key-file=${KEY_FILE}
  export GOOGLE_APPLICATION_CREDENTIALS=${KEY_FILE}
}

curl_with_retry(){
    RETRIES=${RETRIES:-5}
    DELAY=${DELAY:-10}
    STATUS=0
    while [[ ${STATUS} != 200 && ${RETRIES} -gt 0 ]]; do
        username=$1
        url=$2
        output_path=$3
        STATUS=$(curl -sS --location -u "${username}" -X GET "${url}" --output "${output_path}" --write-out "%{http_code}\\n")
        if [[ ! ${STATUS} == 200 ]]; then
            response=$(cat "${output_path}")
            echo -e "ERROR while downloading artifact from jfrog. STATUS: ${STATUS}\nERROR: ${response}"
            ((RETRIES--))
            if [[ ${RETRIES} == 0 ]]; then
                exit 1
            fi
            sleep ${DELAY}
        fi
    done
}

if [[ "${RUNNER_MODE}" == "dev" ]]; then
  echo "Running in dev mode. Artifact will not be downloaded from artifactory. Please mount artifact root directory /dsl-execution/app"
else
  ARTIFACT_FULL_NAME="${ARTIFACT_NAME}-${ARTIFACT_VERSION}${ARTIFACT_EXTENSION}"
  curl_with_retry "${ARTIFACTORY_USERNAME}":"${ARTIFACTORY_PASSWORD}" "${ARTIFACTORY_URL}/${ARTIFACTORY_PATH}/${ARTIFACT_FULL_NAME}" "${ARTIFACT_FULL_NAME}"
  mkdir "./app"
  tar -xzf "${ARTIFACT_FULL_NAME}" -C ./app
fi

# Set kubeconfig
if [[ -n "${BASE_64_ENCODED_KUBECONFIG}" ]]; then
  export KUBECONFIG=~/.kube/config
  mkdir -p $(dirname ${KUBECONFIG})
  echo ${BASE_64_ENCODED_KUBECONFIG} | base64 -d > ${KUBECONFIG}

fi

# Configure cloud credentials
if [[ "${CLOUD_PROVIDER}" == "GCP" ]]; then
  PROJECT_ID=$(echo "${CLOUD_PROVIDER_DATA}" | jq -r '.projectId')
  configure_gcp_service_account ${PROJECT_ID}
fi

# read "dslVersion" from component.groovy
if [[ -n "${DSL_VERSION}" ]]; then
  dslVersion=${DSL_VERSION}
else
  dslVersion=$(cat ./app/*/component.groovy | grep dslVersion | cut -d'"' -f2)
fi

dslFileDir=${MOUNT_PATH}/${dslVersion}
dslFilePath=${dslFileDir}/odin-component-interface.jar

# based on the version, see if jar is available on mount
if [[ -e "${dslFilePath}" ]];
then
  echo "DSL found at location: ${dslFilePath}"
else
  mkdir -p "${dslFileDir}"
  curl_with_retry "${DSL_ARTIFACTORY_USERNAME}":"${DSL_ARTIFACTORY_PASSWORD}" "${DSL_ARTIFACTORY_URL}/d11-repo/com/dream11/odin-component-interface/${dslVersion}/odin-component-interface-${dslVersion}-jar-with-dependencies.jar" "${dslFilePath}"
fi
# invoke the DSL
cd ./app/"${ARTIFACT_NAME}"

if [[ "${WAIT}" = "infinite" ]];
then
  tail -f /dev/null
else
  /groovy-4.0.6/bin/groovy -cp "${dslFilePath}" component.groovy &
  wait $!
fi
