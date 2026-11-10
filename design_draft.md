# StackEmu local AWS sandbox v1

* **Author:** Eduardo Rodrigo Ferreira
* **Status:** Proposed
* **Date:** 2026-11-07

--------------------------------------------------------------------------------

## Overview

StackEmu v1 will provide a platform-team-maintained local sandbox for software
development. A project will run one minimal Docker image containing an S3, SQS,
and SNS emulator, the StackEmu web console, lifecycle commands, diagnostics,
and health checks. Each project will supply a small `stackemu.yaml` file and
keep state in a persistent volume identified by `project.id`.

The platform will support only the S3, SQS, and SNS operations proved by a
published AWS SDK and AWS CLI compatibility matrix. It will start without
network access after its immutable image has been preloaded. The console will
help developers inspect local resources and diagnose failures; it will not aim
to reproduce the full AWS Console.

## The Problem

Teams currently create and maintain separate local environments to test service
integrations. These environments differ by developer and project, which creates
inconsistent configuration, uncertain behavior, duplicated setup scripts, and
slow onboarding. The platform team cannot reliably support or update these
independent environments.

StackEmu will establish one supported product contract: a single image, one
versioned non-secret project configuration file, one isolated state volume, and
one lifecycle interface. This makes the local environment repeatable without
claiming production AWS equivalence.

## Goals

* Provide one minimal, platform-owned Docker image for local S3, SQS, and SNS
  sandbox workflows.
* Let a new developer preload the image and start a functional sandbox within
  ten minutes, without network access during startup.
* Replace pilot-project service scripts with a validated, versioned
  `stackemu.yaml` contract and lifecycle commands: `start`, `status`,
  `diagnose`, `restart`, and destructive `reset`.
* Isolate persistent state by stable `project.id`, prevent accidental volume
  reuse, and recreate declared resources predictably after reset.
* Expose only a tested AWS SDK and AWS CLI operation matrix, including
  SNS-to-SQS delivery required by the pilots.
* Migrate two pilot projects in 30 days while preserving a defined rollback
  path.

## Non-Goals

* Emulate all AWS APIs, IAM, production AWS semantics, or broad AWS
  compatibility.
* Add Kafka, RabbitMQ, WireMock, or other services to v1.
* Support remote, shared, multi-user, public, Kubernetes, multi-node, or
  highly available sandboxes.
* Build a generic plugin system, control plane, configuration language,
  reconciliation engine, migration engine, or full SNS administration console.
* Store secrets in `stackemu.yaml`, automatically reuse current global volumes,
  or allow partial resets.

## Technical Plan (Architecture)

The v1 image will extend the current MiniStack-based setup rather than replace
its emulator. The platform team will select a pinned MiniStack version only
after proving the pilot SNS operation matrix, including SNS-to-SQS delivery.
The image will embed the existing StackEmu Quarkus console and use
`s6-overlay` to supervise both components.

The image entrypoint will receive a mounted `stackemu.yaml`, project secrets
through the container environment, and one external project volume. Before
starting either service, it will validate the schema and preflight the volume.
The image will never build or pull dependencies during `start`.

```text
project repository
  ├── stackemu.yaml ───────────────┐
  └── secrets / environment ───────┼──> StackEmu single Docker image
                                  │      ├── s6-overlay supervisor
Docker project volume <───────────┼──────┤
  ├── ownership marker             │      ├── MiniStack: S3, SQS, SNS
  ├── state-schema version         │      ├── StackEmu web console
  └── applied config hash          │      └── lifecycle CLI and diagnostics
                                  │
developer SDK / AWS CLI ──────────┴──> loopback endpoint and console
```

### Configuration and lifecycle

`stackemu.yaml` will have one schema version. It contains only
`project.id`, enabled services, initial resources, namespace, persistence
policy, and resource limits. The runtime rejects unknown fields, duplicate
resources, invalid namespaces, incompatible service settings, and secret-like
values before it changes a volume or starts a service.

`project.id` is lower-case, begins with a letter, and contains only letters,
digits, and hyphens. The runtime deterministically derives one labelled Docker
volume name from it. Its ownership marker records the project ID, state-schema
version, compatible image range, and hash of the configuration applied during
bootstrap.

`start` takes an exclusive lock in the volume, validates image, schema, and
marker compatibility, then starts the supervised services. Bootstrap runs only
for a new volume or after `reset`; it creates declared resources without
reconciling or deleting existing state during a normal restart. `restart`
keeps all state. `status --json` and `diagnose --json` show non-secret
effective configuration, component readiness, image digest, config and schema
versions, volume usage, lifecycle outcome, and log pointers.

`reset` takes the same exclusive lock, verifies marker ownership, displays
the project ID and target volume, and requires a distinctive interactive
confirmation. CI may use `--yes`. Reset removes every state domain for that
project, recreates the declared resources, and records its result outside the
erased state. A partial reset or automatic volume import is not supported.

### Component health, offline use, and compatibility

`s6-overlay` forwards signals, reaps child processes, and keeps component
logs separate. A required emulator or console failure makes the image exit with
an error. Liveness covers the supervisor and children. Readiness additionally
requires valid configuration, verified state ownership, emulator health,
enabled-service checks, and completed bootstrap.

Release metadata will pin the image and MiniStack digests and publish a preload
manifest. A network-disabled startup test will prove that the lifecycle path
does not pull, build, or call external services. The image exposes endpoints
only on loopback by default.

The compatibility matrix is the only AWS compatibility claim. For S3 and SQS,
it begins with operations the two pilots require. For SNS, it includes creating,
listing, and deleting topics and queues; creating and deleting subscriptions;
publishing; and SNS-to-SQS delivery. Each entry names exact SDK and AWS CLI
versions, known differences, and an automated contract test. Unsupported calls
must fail deterministically and point to the matrix.

## Alternatives Considered

### Retain the current two-service Compose setup

* **Pros**: Reuses the current MiniStack and Quarkus GUI boundary with little
  packaging change.
* **Cons**: Leaves two independently managed images, three global volumes, and
  no strict project identity or lifecycle contract.
* **Reason for Rejection**: Rejected by author: v1 must be one platform-owned
  Docker image with an explicit per-project state contract.

### Use a launcher with separate emulator and console images

* **Pros**: Keeps process lifecycles independent and simplifies individual
  upgrades.
* **Cons**: Requires a launcher or Compose contract that the developer must
  understand and support; it does not meet the confirmed single-image boundary.
* **Reason for Rejection**: Rejected by author: a single image is required for
  the first version. The design contains supervisor, health, and compatibility
  safeguards for that boundary.

### Replace MiniStack with another AWS emulator

* **Pros**: Another emulator might offer broader SNS support.
* **Cons**: Changes the existing base, may increase image size and resource
  use, and could introduce behavior differences unrelated to the pilots.
* **Reason for Rejection**: Rejected by author: retain MiniStack unless it
  fails the pilot operation matrix, including SNS-to-SQS delivery.

### Build a custom emulator

* **Pros**: Allows the exact pilot behavior to be controlled.
* **Cons**: Transfers AWS-protocol compatibility, persistence, and long-term
  maintenance responsibility to the platform team.
* **Reason for Rejection**: Rejected by author: this cost is not justified for
  two pilots or the initial S3, SQS, and SNS scope.

### Build a generic control plane and configuration platform

* **Pros**: Could support future services, templates, and reconciliation.
* **Cons**: Adds schema migration, dynamic configuration, reconciliation, and
  support work before the pilots establish that need.
* **Reason for Rejection**: Rejected by author: v1 has one small
  `stackemu.yaml` schema and bootstrap-on-first-init only.

## Non-Functional Requirements

* Startup works with networking disabled after all digest-pinned images are preloaded.
* Configuration, project ID, and volume ownership are validated before state mutation.
* The runtime uses a read-only root filesystem, no added capabilities, bounded temporary storage, and documented CPU, memory, PID, and storage limits.
* Ports bind to loopback by default, and all status, diagnostics, logs, and markers redact secrets.

## Critical User Journeys (CUJs)

### CUJ 1: start a clean sandbox offline

* **Given**: A project has valid `stackemu.yaml` and the release images are preloaded.
* **When**: A developer runs `stackemu start` with networking disabled.
* **Then**: One labelled volume is created, declared resources are bootstrapped, the services become ready, and the path completes within ten minutes.

### CUJ 2: restart without losing state

* **Given**: A ready sandbox owns a marker-verified project volume.
* **When**: A developer runs `stackemu restart` and `stackemu status --json`.
* **Then**: The same state remains and status reports readiness, image digest, configuration hash, schema version, and volume usage.

### CUJ 3: reset deterministically

* **Given**: A valid project owns the resolved volume.
* **When**: A developer confirms `stackemu reset`, or CI passes `--yes`.
* **Then**: All project state is erased, declared resources are recreated, and the lifecycle result is audited outside erased state.

## Detailed Implementation Plan

### Target Files to Create/Modify

#### 1. `pom.xml` (Modify)

* **Type / Scope**: Build dependencies and packaging.
* **Proposed Changes**: Add the AWS SDK SNS client for contract tests, a YAML parser, and a Maven packaging execution that emits a runnable lifecycle JAR with `LifecycleMain` as manifest entry point, alongside the Quarkus console JAR.

#### 2. `src/main/java/io/github/erf88/stackemu/lifecycle/LifecycleMain.java`, `StackEmuConfig.java`, and `StackEmuYamlReader.java` (Create)

* **Type / Scope**: Lifecycle executable and schema-v1 parser.
* **Proposed Changes**: Implement `start`, `restart`, `status`, `diagnose`, and `reset`. Accept only project ID, enabled services, initial resources, namespace, persistence policy, and limits; reject unknown, duplicate, invalid, incompatible, secret-like, and unsupported-version values before mutation. Define documented non-zero error codes and produce a canonical redacted configuration hash.

#### 3. `src/main/java/io/github/erf88/stackemu/lifecycle/ProjectId.java`, `VolumeIdentity.java`, `OwnershipMarker.java`, `VolumeLock.java`, `BootstrapService.java`, `ResetService.java`, and `DiagnosticReport.java` (Create)

* **Type / Scope**: State safety and lifecycle operations.
* **Proposed Changes**: Enforce lower-case, letter-led letter/digit/hyphen IDs; derive one deterministic labelled volume; verify marker project ID, schema, compatible image range, and configuration hash; lock start/reset; bootstrap only new/reset state; preserve restart state; reset all state after confirmation; and emit redacted text/JSON diagnostics.

#### 4. `src/main/resources/application.properties` and `src/main/java/io/github/erf88/stackemu/adapter/web/HealthResource.java` (Modify/Create)

* **Type / Scope**: Console runtime wiring.
* **Proposed Changes**: Receive lifecycle-provided endpoint, region, identity, metadata, and state path; retain loopback binding; move operation storage under project state; add separate console liveness and readiness endpoints; never log credentials.

#### 5. `Dockerfile`, `docker/entrypoint.sh`, and `docker/s6-overlay/` service definitions (Create)

* **Type / Scope**: Single-image build and supervision.
* **Proposed Changes**: Build the two JARs, assemble a digest-pinned runtime with MiniStack and s6-overlay but no build tools, run preflight before supervisor handoff, and define separate emulator/console services with readiness order, signal handling, child-failure exit, separate logs, and writable state/temp paths only.

#### 6. `compose.yaml` (Modify)

* **Type / Scope**: Development and integration-test launcher.
* **Proposed Changes**: Replace MiniStack and GUI services with one image, one mounted configuration file, one labelled project volume, loopback ports, resource limits, and lifecycle readiness checks. Remove automatic use of existing global volumes.

#### 7. `stackemu.example.yaml`, `docs/stackemu-yaml.md`, `docs/compatibility-matrix.md`, `docs/offline-preload.md`, `docs/rollback.md`, and `README.md` (Create)

* **Type / Scope**: Configuration and support documentation.
* **Proposed Changes**: Document schema v1, project ID and secrets rules, bootstrap/reset semantics, preload and offline startup, compatibility bounds, image/state rollback, one-command onboarding, endpoint discovery, diagnostics, and pilot rollout.

#### 8. `src/test/java/io/github/erf88/stackemu/lifecycle/StackEmuYamlReaderTest.java`, `ProjectIdTest.java`, `VolumeIdentityTest.java`, `LifecycleMainTest.java`, and `ResetServiceTest.java` (Create)

* **Type / Scope**: Lifecycle unit tests.
* **Proposed Changes**: Cover parser failures, configuration hashes, identity, marker mismatches, image compatibility refusal, locking, command exit codes, confirmation versus `--yes`, reset audit, and no mutation after failed preflight.

#### 9. `src/test/java/io/github/erf88/stackemu/SandboxLifecycleIntegrationTest.java`, `OfflineStartupIntegrationTest.java`, and `SnsContractIntegrationTest.java` (Create)

* **Type / Scope**: Container and AWS contract tests.
* **Proposed Changes**: Verify the single image, readiness, restart, reset, isolation, foreign-volume refusal, child failure, offline startup, and every published S3/SQS/SNS SDK/CLI operation, including SNS-to-SQS after cold start, restart, and reset.

#### 10. `scripts/preload-stackemu.ps1` and `scripts/verify-offline-stackemu.ps1` (Create)

* **Type / Scope**: Release verification.
* **Proposed Changes**: Preload release digests, report missing images without pulling them, launch with networking disabled, wait for readiness, execute contract tests, and fail on any build, pull, or external call.

### Verification Plan

* Package both Java artifacts and build the digest-pinned final image.
* Run existing checks plus lifecycle parser, marker, lock, reset, and diagnostics unit tests.
* Run container tests for limits, loopback exposure, separate logs, health transitions, isolation, and destructive reset.
* Run every published SDK/CLI matrix case, including SNS-to-SQS after cold start, restart, reset, and offline startup.
* Preload images, disable networking, and prove startup makes no pull, build, or external call.
* Migrate two pilots, measure clean-machine startup below ten minutes, rehearse rollback, and stop broader rollout on state loss, isolation, compatibility, migration, or rollback failure.

