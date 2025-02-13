#!/bin/bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )


if [[ "${CI}" = "true" ]]; then
    GPG_SKIP_OPT="-Dgpg.skip"
fi

./mvnw clean install ${GPG_SKIP_OPT}
