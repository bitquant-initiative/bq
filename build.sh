#!/bin/bash

set -e
set -x

PROJECT_ROOT=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )


if [[ "${CI}" = "true" ]]; then
    GPG_SKIP_OPT="-Dgpg.skip"
    MAVEN_OPTS="${GPG_SKIP_OPT} --no-transfer-progress -B ${MAVEN_OPTS}"

    ${PROJECT_ROOT}/.github/workflows/pre-build.sh
fi

./mvnw clean install ${MAVEN_OPTS}
