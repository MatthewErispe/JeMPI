#!/bin/bash

set -e
set -u

source ../../docker/conf/images/conf-app-images.sh
source ../build-check-jdk.sh

JAR_FILE=${FHIR_JAR}
APP_IMAGE=${FHIR_IMAGE}
APP=fhir

source ../build-app.sh
