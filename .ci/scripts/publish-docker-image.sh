#!/usr/bin/env bash
set -e
docker login ${LOGIN_URL} -u ${USERNAME} -p ${PASSWORD}
docker build --build-arg ARTIFACTORY_USERNAME=${USERNAME} --build-arg ARTIFACTORY_PASSWORD=${PASSWORD} -t ${REGISTRY}/${REPOSITORY}:${TAG} ./runner
docker push ${REGISTRY}/${REPOSITORY}:${TAG}
