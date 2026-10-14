# Known limitations / Limitações conhecidas

## Baseline status / Status do baseline

The Phase 0 implementation lists S3 buckets against MiniStack 1.5.25 using Qute and the AWS SDK. Four local tests and the Compose smoke test passed; see [evidence](spikes/phase-0-results.md). All capabilities outside this narrow proof remain unverified.

A Fase 0 implementa listagem de buckets S3 com Qute e SDK contra MiniStack 1.5.25. Quatro testes locais e o smoke test Compose passaram; as demais capacidades permanecem não verificadas.

## MVP exclusions / Exclusões do MVP

- AWS account administration, real-AWS console replacement, multitenancy, and internet exposure.
- Other service modules, persistent editable connection profiles, arbitrary terminal/shell, and native image as a release requirement.
- Complex S3 policy/CORS/encryption/lifecycle/replication editing, object metadata/content editing, object rename, and full version recovery.
- Automatic empty-and-delete bucket behavior; destructive/version-aware flows are post-MVP and need dedicated tests.

## Compatibility limits / Limites de compatibilidade

MiniStack is the proposed primary reference, while LocalStack and Moto are later targets. No emulator is currently certified compatible. Different S3 semantics, capability detection, health endpoints, persistence paths, and licensing/token requirements must be recorded by tested version.

## Legal and identity / Legal e identidade

The StackEmu name remains legally unvalidated. The project repository is `ERF88/StackEmu`. Validate package registries, Docker Hub, domain, and INPI before public release. Do not imply endorsement or affiliation with third-party marks.
