#!/bin/bash

export PROJECT_DIR=$(builtin cd ../../; pwd)
export PROJECT_DATA_DIR=${PROJECT_DIR}/docker_data/data
export PROJECT_DATA_APPS_DIR=${PROJECT_DIR}/docker_data/data-apps
export PROJECT_DATA_MONITOR_DIR=${PROJECT_DIR}/docker_data/data-monitor

export NODE1=$(hostname)
export NODE1_IP=$(hostname -i)

export SCALE_KAFKA_01=1
export SCALE_KAFKA_02=1
export SCALE_KAFKA_03=1
export SCALE_ZERO_01=1
export SCALE_ALPHA_01=1
export SCALE_ALPHA_02=1
export SCALE_ALPHA_03=1
export SCALE_RATEL=1
export SCALE_POSTGRESQL=1

export POSTGRESQL_USERNAME="postgres"
export POSTGRESQL_PASSWORD="postgres"
export POSTGRESQL_DATABASE="notifications"

# DON'T CHANGE
export REGISTRY_NODE_IP=${NODE1}:5000/v2

envsubst < conf-env-1-pc.template > conf.env