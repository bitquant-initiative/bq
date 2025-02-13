#!/bin/bash


if [[ "${CI}" = "true" ]]; then
    GPG_SKIP_OPT="-Dgpg.skip"
fi

 mvn -B clean test install ${GPG_SKIP_OPT}