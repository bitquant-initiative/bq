FROM ghcr.io/bitquant-initiative/bq-base


COPY target/stage/lib/* /app/lib/
COPY target/classes/  /app/classes
ENV JAVA_MAIN_CLASS=bq.loader.Main

USER app
RUN duckdb -c "install aws" 

