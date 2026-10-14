# Phase 0 results / Resultados da Fase 0

Date: 2026-10-13 (America/Sao_Paulo). Environment: Windows, Docker Desktop Linux/WSL2, amd64. Local checks and [GitHub CI run 38489404176](https://github.com/ERF88/StackEmu/actions/runs/38489404176) passed, including four tests, Linux container build, Compose and smoke test. Arm64 is not yet confirmed. The first GitHub run failed resolving Maven dependencies before tests; CI was aligned to the same Maven 3.9.11/Java 25 container that built successfully locally.

## Pinned and resolved versions

- Local JDK: Temurin 25.0.4.1; Maven 3.9.9.
- Quarkus/core and Amazon services platform BOM: 3.40.1.
- Resolved S3 extension: 3.22.1; AWS SDK S3: 2.54.13.
- Docker builder: Maven 3.9.11 / Temurin 25. The initially attempted Maven 3.9.9/Java 25 image tag does not exist; 3.9.11 was verified and used.
- Runtime: `eclipse-temurin:25-jre` (major tag; immutable digest pinning remains a follow-up).
- MiniStack: 1.5.25; observed digest `sha256:1754efa38cac6fbaa696d9ce0fe30cd28f74e67e4ba60167a8a96bd167dfd9b1`.
- htmx: 2.0.8, vendored locally; native CSS, no asset bundler/Node pipeline.

## Executed evidence

1. `mvn -B -ntp verify` with JDK 25 and `STACKEMU_INTEGRATION=true`: **4 tests, 0 failures/errors/skips**, build success. The integration test creates a unique empty bucket, lists it through the SDK-backed full page and Qute fragment, then removes only that test bucket.
2. `docker compose up -d ministack`: healthy at loopback 4566.
3. AWS CLI inside MiniStack (1.45.63): created `stackemu-phase0-demo`; `s3api list-buckets` returned it using `http://localhost:4566`.
4. `docker compose up -d --build gui`: Linux container build succeeded; GUI uses `http://ministack:4566` internally.
5. `scripts/smoke.ps1`: full-page connected state, Portuguese partial response and process liveness passed. A separate HTTP check found the CLI-created bucket in GUI HTML.
6. `docker compose exec -T gui id`: UID 10001 (non-root). Both published ports bind to 127.0.0.1; no Docker socket mounted.
7. Restarted MiniStack; the browser still listed `stackemu-phase0-demo` afterwards. State and object storage volumes are configured; object-byte persistence across container recreation was not separately tested.
8. Browser inspection of Portuguese page and refresh action succeeded. Full browser automation of network requests/history, keyboard accessibility, and both themes remains future work.

## Phase-0 scope and remaining work

F0.1–F0.4 and F0.6 have executable local evidence. F0.5 proves checked Qute templates, local htmx asset delivery and GET-only fragment handling; CSRF for future writes and browser history behavior remain unverified. F0.7 remains an explicit hypothesis record, not completed interviews. Initial ADRs cover stack, scope and configuration/local security.

This proof does not establish full S3 compatibility, upload/download, mutation security, production readiness, arm64, native-image viability, or performance baselines. Legal-name validation remains pending. No broad module abstractions were introduced.

## Official sources checked

- [Quarkus 3.40 LTS](https://quarkus.io/blog/quarkus-3-40-released/)
- [S3 extension configuration](https://docs.quarkiverse.io/quarkus-amazon-services/dev/amazon-s3.html)
- [Platform Amazon BOM 3.40.1](https://repo.maven.apache.org/maven2/io/quarkus/platform/quarkus-amazon-services-bom/3.40.1/quarkus-amazon-services-bom-3.40.1.pom)
- [MiniStack configuration/health/persistence](https://ministack.org/docs/configuration)

## Reprodução / Reproduction

See [the Phase 0 guide](../development/phase-0.md). Os resultados acima foram executados localmente: quatro testes passaram, a GUI listou o bucket criado via CLI e funcionou no Compose. CI, arm64, CSRF de mutações, histórico do navegador e persistência dos bytes após recriação ainda não foram confirmados.
