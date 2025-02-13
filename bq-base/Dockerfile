FROM public.ecr.aws/lambda/java:21

LABEL org.opencontainers.image.source=https://github.com/bitquant-initiative/bq-base

RUN dnf install -y unzip shadow-utils findutils awscli less && \
curl --fail --location --progress-bar --output duckdb.zip https://github.com/duckdb/duckdb/releases/download/v1.1.3/duckdb_cli-linux-$(uname -i | sed s/x86_64/amd64/).zip && \
unzip duckdb.zip && \
rm duckdb.zip && \
mv duckdb /usr/bin/duckdb && \
chmod +x /usr/bin/duckdb && \
/usr/bin/duckdb --version && \
/usr/sbin/useradd -m -u 1000 -d /app app && \
mkdir /app/classes && \
mkdir /app/lib && \
rm -rf /var/cache/yum



WORKDIR /app
ADD entrypoint.sh /app/entrypoint.sh


#USER app
ENTRYPOINT [ "/app/entrypoint.sh" ]
