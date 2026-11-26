FROM maven:3.9-eclipse-temurin-25 AS build
ARG STACKEMU_VERSION=0.1.0-SNAPSHOT
WORKDIR /workspace
COPY pom.xml .
COPY stackemu-console/pom.xml stackemu-console/pom.xml
COPY stackemu-console/package.json stackemu-console/package-lock.json stackemu-console/
COPY stackemu-console/src stackemu-console/src
COPY stackemu-cli/pom.xml stackemu-cli/pom.xml
COPY assets assets
RUN mvn -B -ntp -pl stackemu-console -am -Drevision=${STACKEMU_VERSION} -DskipTests package

FROM ministackorg/ministack:1.5.25@sha256:1754efa38cac6fbaa696d9ce0fe30cd28f74e67e4ba60167a8a96bd167dfd9b1
RUN apk add --no-cache openjdk25-jre s6
COPY --from=build /workspace/stackemu-console/target/quarkus-app /opt/stackemu
COPY docker/s6 /etc/s6
COPY docker/entrypoint.sh /usr/local/bin/stackemu-entrypoint
RUN chmod +x /usr/local/bin/stackemu-entrypoint /etc/s6/*/run
ENV STACKEMU_ENDPOINT=http://127.0.0.1:4566 STACKEMU_OPERATIONS_PATH=/data/stackemu/operations STATE_DIR=/data/stackemu/ministack-state S3_DATA_DIR=/data/stackemu/s3 SERVICES=s3,sqs,sns PERSIST_STATE=1 S3_PERSIST=1 QUARKUS_HTTP_HOST=0.0.0.0
EXPOSE 4566 8080
ENTRYPOINT ["/usr/local/bin/stackemu-entrypoint"]
