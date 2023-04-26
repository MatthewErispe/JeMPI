#!/bin/bash

#set -e
set -u

pushd .
  SCRIPT_DIR=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
  cd ${SCRIPT_DIR}/../..

  source ./0-conf.env
  docker stack rm ${STACK_NAME}
  sleep 15
  echo

popd
