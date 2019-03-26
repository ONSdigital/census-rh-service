#!/bin/bash

set -e

echo $GCLOUD_SERVICE_KEY | base64 -d | docker login -u _json_key --password-stdin https://eu.gcr.io

export TAG=`if [ "$TRAVIS_PULL_REQUEST_BRANCH" == "" ]; then echo "latest"; else echo $TRAVIS_PULL_REQUEST_BRANCH; fi`

echo "Building with tag [$TAG]"

docker build -t eu.gcr.io/census-int-ci/census-rh-service:$TAG .

docker push eu.gcr.io/census-int-ci/census-rh-service:$TAG
