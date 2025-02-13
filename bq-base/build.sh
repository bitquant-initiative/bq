#!/bin/bash

docker build . --no-cache --platform linux/amd64,linux/arm64 -t bq-base -t ghcr.io/bitquant-initiative/bq-base

if [[ "${CI}" = "true" ]]; then

    docker push ghcr.io/bitquant-initiative/bq-base

fi