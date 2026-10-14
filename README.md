# StackEmu

> **Phase 2: S3 reading.** StackEmu provides bucket details, paginated objects, prefixes/breadcrumbs, metadata and streaming downloads, with English/pt-BR and light/dark themes. The MVP is still in development.

[Português (Brasil)](README.pt-BR.md)

## Status

The product name was selected on 13 October 2026, but legal/name validation is **pending**. The project repository is [ERF88/StackEmu](https://github.com/ERF88/StackEmu). Before a public release, validate package registries, Docker Hub, domain availability, and INPI records. StackEmu is not affiliated with AWS, MiniStack, LocalStack, or Moto.

Phase 2 reads and downloads objects against MiniStack 1.5.25. GUI bucket creation/upload remain pending. See the [Phase 2 guide](docs/development/phase-2.md) and [executed evidence](docs/spikes/phase-2-results.md).

## Planned stack

Java 25 LTS (Java 21 considered as an alternative), Quarkus 3.40 LTS, Qute, htmx, CSS, Maven, and AWS SDK for Java 2.x. Exact extension, BOM, JDK, and emulator versions require an implementation spike and official-source validation.

## Quick start — Phase 0

Requires Docker Compose v2. The GUI connects to MiniStack through `http://ministack:4566`, published only at `127.0.0.1:8080`. The following was executed locally on Windows/amd64:

```sh
docker compose up --build -d
```

Open <http://127.0.0.1:8080/>. See [Phase 0 commands and tests](docs/development/phase-0.md) and [executed evidence and limitations](docs/spikes/phase-0-results.md). Stop using `docker compose down`; named volumes are retained.

## Documentation

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

See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [LICENSE](LICENSE). The planned license is MIT.
