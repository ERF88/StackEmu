# StackEmu

StackEmu is a local development sandbox for AWS-compatible S3, SQS, and SNS workflows. A single Docker image runs the emulator and the developer console; the `stackemu` CLI controls that image through the local Docker CLI.

## Repository modules

- `stackemu-console/` contains the Quarkus console, AWS adapters, bootstrap, and browser assets.
- `stackemu-cli/` contains the host-side launcher and Docker lifecycle commands.
- `docs/designs/stackemu-local-aws-sandbox-v1.md` describes the v1 product contract.

## Requirements

- Java 25 and Maven 3.9 or later.
- Docker Engine or Docker Desktop with the Docker CLI available on `PATH`.
- AWS CLI v2 is optional and is used only by the AWS CLI contract checks.

The console build installs the pinned Node.js/npm toolchain and Tailwind dependencies through Maven. The first build needs access to the Maven and npm package registries; subsequent npm dependency resolution uses the checked-in lockfile.

## Build and verify

Build and test both Maven modules from the repository root:

```sh
mvn -B clean verify
```

Build the native launcher on a GraalVM 25 host:

```sh
mvn -B -pl stackemu-cli -am -Pnative -DskipTests package
```

Start the development Compose harness:

```sh
docker compose up --build
```

The default local endpoints are `http://127.0.0.1:4566` for AWS APIs and `http://127.0.0.1:8080` for the console. The supported user-facing lifecycle interface is the `stackemu` executable; Compose is for repository development and integration tests.

## Release candidates

A manual GitHub Actions workflow builds versioned native launchers and a multi-platform container image, then creates a draft pre-release with an image digest manifest and SHA-256 checksums. See [docs/releasing.md](docs/releasing.md) for the pilot and review steps.

## AWS API contract checks

The Bash and PowerShell scripts exercise S3 bucket/object operations, SQS queue/message operations, SNS topic/subscription operations, and SNS-to-SQS delivery. They create uniquely named resources and remove only those resources during cleanup.

```sh
STACKEMU_ENDPOINT=http://127.0.0.1:4566 bash scripts/aws-contract.sh
```

```powershell
$env:STACKEMU_ENDPOINT = 'http://127.0.0.1:4566'
./scripts/aws-contract.ps1
```

The Maven reactor also runs the CLI's AWS SDK contract and console tests. Docker lifecycle tests are opt-in with `STACKEMU_DOCKER_INTEGRATION=true` and require the preloaded `stackemu:local` image.
