#!/bin/bash

set -e

echo $GCLOUD_SERVICE_KEY | base64 -d | docker login -u _json_key --password-stdin https://eu.gcr.io

export VERSIONTAG=$TRAVIS_BUILD_ID"-"$BRANCH

echo "Building with tags [$VERSIONTAG]"

docker build -t eu.gcr.io/census-int-ci/census-rh-service:$VERSIONTAG .
docker push eu.gcr.io/census-int-ci/census-rh-service:$VERSIONTAG

