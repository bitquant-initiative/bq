#!/bin/bash

set -e 

if [[ ! -z "${CI}" ]]; then
    MAVEN_OPTS=-B
fi

../mvnw ${MAVEN_OPTS} clean install 
mkdir -p target/stage/lib

cp target/lib/*.jar target/stage/lib/
rm -rf target/stage/lib/bq-loader*
#cp target/bq-loader*.jar target/stage/

#rm -f target/stage/*source*.jar
#rm -f target/stage/*javadoc*.jar




docker build --platform linux/amd64,linux/arm64 . -t bq-loader

docker tag bq-loader ghcr.io/bitquant-initiative/bq-loader

if [[ ! -z "${CI}" ]]; then
    docker push ghcr.io/bitquant-initiative/bq-loader
fi