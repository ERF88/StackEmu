# Release process / Processo de release

## Preconditions / Pré-condições

Do not release until the README has been followed by someone without prior context; Phase 0 evidence is available; tests run on a clean environment; bilingual documentation is synchronized; known limitations and tested emulator versions are published; legal-name validation is complete; and security review covers CSRF, escaping, secrets, port exposure, limits, and non-root container execution.

## Planned CI for pull requests / CI planejado

Build Maven/format check, unit and integration tests, ArchUnit, web/CSRF tests, essential E2E, i18n validation, dependency vulnerability review, image build, and container smoke test. Exact workflows/commands await implementation.

## Planned tagged release / Release por tag

For `vX.Y.Z`, build amd64/arm64 images, publish to the intended GHCR namespace `ghcr.io/erf88/stackemu` once the repository exists and publishing is configured (Docker Hub only after process stabilizes), generate CycloneDX SBOM, scan vulnerabilities, publish changelog/release notes, apply minimum workflow permissions/provenance where feasible, and pull/run the published image. Planned tags are `X.Y.Z`, `X.Y`, and `latest`; prerelease policy is still to be written.

## JVM versus native / JVM versus nativa

JVM image is the initial default. Measure final size, idle/active memory, time to health, build time/complexity, library compatibility, and test behavior before making a native image the default. No target numbers are set before a baseline exists.
