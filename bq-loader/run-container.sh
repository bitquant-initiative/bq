#!/bin/bash


docker run -it \
-e SYMBOLS="X:BTC X:DOGE" \
-v ${HOME}/.bq:/app/.bq \
-v ${HOME}/.aws:/app/.aws \
-v ./data:/app/data \
ghcr.io/bitquant-initiative/bq-loader