#!/bin/bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

cd ${SCRIPT_DIR}/bq-util && mvn clean install
cd ${SCRIPT_DIR}/bq-ta4j && mvn clean install
cd ${SCRIPT_DIR}/bq-duckdb && mvn clean install
cd ${SCRIPT_DIR}/bq-ducktape && mvn clean install
