#!/bin/bash


DOCKER_OPTS=--platform=linux/amd64 

docker run -it ${DOCKER_OPTS} \
-e SYMBOLS="X:BTC X:DOGE" \
-v ${HOME}/.bq:/app/.bq \
-v ${HOME}/.aws:/app/.aws \
-v ./data:/app/data \
ghcr.io/bitquant-initiative/bq-loader