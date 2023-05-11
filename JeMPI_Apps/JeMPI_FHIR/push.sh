#!/bin/bash

set -e
set -u

source ../../docker/0-conf.env
source ../../docker/conf/images/conf-app-images.sh

docker tag ${FHIR_IMAGE} ${REGISTRY_NODE_IP}/${FHIR_IMAGE}
docker push ${REGISTRY_NODE_IP}/${FHIR_IMAGE}
docker rmi ${REGISTRY_NODE_IP}/${FHIR_IMAGE}
 
