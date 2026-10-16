FROM node:24.10.0@sha256:06e54ecf113a30f0ff9a1d309866a5924d3bda4389eab11a0e1e92f3251d915d AS styles
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts --no-audit --no-fund
COPY scripts/build-css.mjs ./scripts/build-css.mjs
COPY src ./src
RUN npm run build:css
FROM maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY --from=styles /workspace/target/generated-resources ./target/generated-resources
RUN MAVEN_OPTS="-Xmx512m -XX:ActiveProcessorCount=2" mvn -B -ntp package -DskipTests -Dskip.frontend=true
FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61
# Official musl JRE; Ubuntu runtime packages are replaced, not hidden from scanning.
RUN apk add --no-cache libpng=1.6.59-r0 zlib=1.3.2-r1
LABEL org.opencontainers.image.title="StackEmu" \
      org.opencontainers.image.source="https://github.com/ERF88/StackEmu" \
      org.opencontainers.image.licenses="MIT"
WORKDIR /app
COPY LICENSE /licenses/StackEmu-LICENSE
COPY THIRD_PARTY_NOTICES.md /licenses/THIRD_PARTY_NOTICES.md
COPY --from=styles /workspace/node_modules/tailwindcss/LICENSE /licenses/Tailwind-CSS-LICENSE
COPY --from=build /build/target/quarkus-app/ ./
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]
