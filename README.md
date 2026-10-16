# StackEmu

> **Phase 5: delivery preparation.** StackEmu provides the S3 read/write slice, browser quality checks, a hardened local JVM container and SBOM/scan tooling. Public release remains gated; the MVP is still in development.

[Português (Brasil)](README.pt-BR.md)

## Status

The first current functional delivery now targets [full operational S3 management](docs/product/s3-management-scope.md), including preview, deletion, copy/move, clipboard and presigned download links. This is confirmed scope, not a claim those capabilities are already implemented; advanced bucket configuration is excluded.

The product name was selected on 13 October 2026, but legal/name validation is **pending**. The project repository is [ERF88/StackEmu](https://github.com/ERF88/StackEmu). Before a public release, validate package registries, Docker Hub, domain availability, and INPI records. StackEmu is not affiliated with AWS, MiniStack, LocalStack, or Moto.

Phase 3 adds confirmed CSRF-protected forms and bounded uploads against MiniStack 1.5.25. See the [Phase 3 guide](docs/development/phase-3.md) and [executed evidence](docs/spikes/phase-3-results.md). There is no deletion, overwrite or resumable-upload UI; use only in trusted local development.

## Implemented stack

UX-1 implements Slate/blue themes and a responsive shell with locally compiled Tailwind CSS, without Preflight; Node is build-only. See [CSS build rules](docs/development/frontend-build.md) and [verification and acceptance status](docs/spikes/ux-1-results.md). Pending UX stages are not implemented S3 capabilities.

Interface SVGs use [coolicons v4.1](https://coolicons.cool/) by Kryston Schwarze, under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), not the application's MIT license. Selected icons are adapted into local Qute fragments; attribution, source revision and changes are recorded in [third-party notices](THIRD_PARTY_NOTICES.md).

Java 25, Quarkus 3.40.1, Qute, local htmx, CSS, Maven and AWS SDK for Java 2.x via the platform BOM. MiniStack 1.5.25 on amd64 is the tested baseline; other platforms/emulators are not certified.

## Quick start — local development

Requires Docker Compose v2. The GUI connects to MiniStack through `http://ministack:4566`, published only at `127.0.0.1:8080`. The following was executed locally on Windows/amd64:

```sh
docker compose up --build -d --wait
```

Open <http://127.0.0.1:8080/>. See [Phase 0 commands and tests](docs/development/phase-0.md) and [executed evidence and limitations](docs/spikes/phase-0-results.md). Stop using `docker compose down`; named volumes are retained.

## Documentation

- [Phase 4 quality evidence](docs/spikes/phase-4-results.md) and [human review checklist](docs/security-review.md)
- [Phase 5 container/SBOM/scan](docs/development/phase-5.md) and [release gates](docs/development/release.md)

- [Naming and responsibility guidelines](docs/development/naming-and-responsibilities.md) and [class review](docs/architecture/naming-review.md)

- [Phase 1 guide](docs/development/phase-1.md) and [validation](docs/spikes/phase-1-results.md)

- [Documentation inventory](docs/DOCUMENTATION_INVENTORY.md)
- [Product roadmap](docs/product/roadmap.md) and [research hypotheses](docs/product/user-research.md)
- [Architecture overview](docs/architecture/overview.md), [modules](docs/architecture/modules.md), and [emulator compatibility](docs/architecture/emulator-compatibility.md)
- [Configuration](docs/configuration.md), [security](docs/security.md), [known limitations](docs/known-limitations.md), and [S3 scope](docs/services/s3.md)
- [Design-system direction](docs/design-system.md)
- [Development/testing](docs/development/testing.md), [release](docs/development/release.md), [ADRs](docs/decisions/)

## Safety

The planned MVP has no user authentication and is intended only for a trusted local development environment. Bind published ports to loopback by default; never expose it to the internet. Read [SECURITY.md](SECURITY.md) before reporting a vulnerability.

## Contributing and license

See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [LICENSE](LICENSE). The license is MIT; dependency licenses remain separate.
