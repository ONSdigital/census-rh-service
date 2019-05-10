#!/bin/bash

set -e

echo $GCLOUD_SERVICE_KEY | base64 -d | docker login -u _json_key --password-stdin https://eu.gcr.io

export VERSION=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
export VERSIONTAG=`if [ "$TRAVIS_PULL_REQUEST_BRANCH" == "" ]; then echo $VERSION; else echo $VERSION"-"$TRAVIS_PULL_REQUEST_BRANCH; fi`
export PURPOSETAG=`if [ "$TRAVIS_PULL_REQUEST_BRANCH" == "" ]; then echo "latest"; else echo "snapshot"; fi`

echo "Building with tags [$VERSIONTAG $PURPOSETAG]"

docker build -t eu.gcr.io/census-int-ci/census-rh-service:$VERSIONTAG .
docker push eu.gcr.io/census-int-ci/census-rh-service:$VERSIONTAG
docker tag eu.gcr.io/census-int-ci/census-rh-service:$VERSIONTAG eu.gcr.io/census-int-ci/census-rh-service:$PURPOSETAG
docker push eu.gcr.io/census-int-ci/census-rh-service:$PURPOSETAG
