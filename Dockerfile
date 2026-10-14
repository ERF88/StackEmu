FROM maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64
# StackEmu starts Java directly; the inherited Pebble supervisor is unused.
# Remove the exact inherited binary rather than suppressing its Go CVE findings.
RUN rm /usr/bin/pebble
LABEL org.opencontainers.image.title="StackEmu" \
      org.opencontainers.image.source="https://github.com/ERF88/StackEmu" \
      org.opencontainers.image.licenses="MIT"
WORKDIR /app
COPY LICENSE /licenses/StackEmu-LICENSE
COPY THIRD_PARTY_NOTICES.md /licenses/THIRD_PARTY_NOTICES.md
COPY --from=build /build/target/quarkus-app/ ./
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]
