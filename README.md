# StackEmu

> **Planning documentation — not an implemented product.** StackEmu is a proposed open-source web interface for managing AWS-compatible local emulators, starting with S3. It is not an AWS console, does not manage AWS accounts, and is not yet verified against an emulator.

[Português (Brasil)](README.pt-BR.md)

## Status

The product name was selected on 13 October 2026, but legal/name validation is **pending**. The project repository is [ERF88/StackEmu](https://github.com/ERF88/StackEmu). Before a public release, validate package registries, Docker Hub, domain availability, and INPI records. StackEmu is not affiliated with AWS, MiniStack, LocalStack, or Moto.

The MVP target is a usable S3 vertical slice: connection status, bucket listing/creation, object navigation by prefix, upload, streaming download, understandable failures, CLI equivalents, and English/pt-BR UI. Nothing in this repository demonstrates that these capabilities work yet.

## Planned stack

Java 25 LTS (Java 21 considered as an alternative), Quarkus 3.40 LTS, Qute, htmx, CSS, Maven, and AWS SDK for Java 2.x. Exact extension, BOM, JDK, and emulator versions require an implementation spike and official-source validation.

## Quick start — planned, unverified

The reference topology is a GUI container connected to MiniStack through `http://ministack:4566`, with the GUI published only at `127.0.0.1:8080`. A Compose file and application do not exist in this documentation-only baseline. Do not treat the following as a tested command:

```sh
docker compose up
```

When implemented and verified, the quick-start guide will cover starting the environment, creating a bucket, uploading/downloading an object, stopping it, and safely removing local data.

## Documentation

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
