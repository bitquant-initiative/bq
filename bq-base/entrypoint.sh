#!/bin/bash

if [[ "$1" = "bash" ]]; then
  exec bash
fi

 if [[ -z "${JAVA_MAIN_CLASS}" ]]; then
  echo 'add ENV JAVA_MAIN_CLASS= to your Dockerfile'
  exit 1
 fi

exec java ${JAVA_OPTS} -cp '/app/classes:/app/lib/*' ${JAVA_MAIN_CLASS}  $@