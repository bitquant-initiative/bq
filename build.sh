#!/bin/bash

set -e
set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )


if [[ "${CI}" = "true" ]]; then
    GPG_SKIP_OPT="-Dgpg.skip"
    MAVEN_OPTS="${MAVEN_OPTS} ${GPG_SKIP_OPT}"
fi

echo ./mvnw clean install ${MAVEN_OPTS}
