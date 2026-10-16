FROM maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c AS fixtures
WORKDIR /build
COPY pom.xml .
COPY package.json package-lock.json ./
COPY scripts ./scripts
COPY src ./src
RUN mvn -B -ntp -Dtest=CssPrototypeTest test
FROM node:24.10.0@sha256:06e54ecf113a30f0ff9a1d309866a5924d3bda4389eab11a0e1e92f3251d915d AS css-spike
WORKDIR /workspace
COPY tools/ux0 ./tools/ux0
RUN npm --prefix tools/ux0 ci --ignore-scripts --no-audit --no-fund
COPY src/test/resources/ux0 ./src/test/resources/ux0
COPY src/main/resources/META-INF/resources/app.css ./src/main/resources/META-INF/resources/app.css
COPY --from=fixtures /build/work/ux0-fixtures ./work/ux0-fixtures
RUN node tools/ux0/build.mjs
