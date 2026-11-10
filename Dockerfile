FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -ntp -DskipTests -Dskip.frontend=true package

FROM ministackorg/ministack:1.5.25
RUN apk add --no-cache openjdk25-jre s6
COPY --from=build /workspace/target/quarkus-app /opt/stackemu
COPY docker/s6 /etc/s6
COPY docker/entrypoint.sh /usr/local/bin/stackemu-entrypoint
RUN chmod +x /usr/local/bin/stackemu-entrypoint /etc/s6/*/run
ENV STACKEMU_ENDPOINT=http://127.0.0.1:4566 STACKEMU_OPERATIONS_PATH=/data/stackemu/operations SERVICES=s3,sqs,sns PERSIST_STATE=1 S3_PERSIST=1 QUARKUS_HTTP_HOST=0.0.0.0
EXPOSE 4566 8080
ENTRYPOINT ["/usr/local/bin/stackemu-entrypoint"]
