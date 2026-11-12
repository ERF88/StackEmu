# StackEmu local AWS sandbox v1

* **Author:** Eduardo Rodrigo Ferreira
* **Status:** Proposed
* **Date:** 2026-11-07

## Overview

StackEmu v1 is a platform-team-maintained single Docker image for local S3,
SQS, and SNS development. It contains MiniStack, the StackEmu console, process
supervision, diagnostics, lifecycle commands, and a versioned bootstrap. Each
project supplies a non-secret stackemu.yaml file and owns persistent state
derived from a stable project ID.

The product supports only the documented and tested AWS SDK/AWS CLI operation
matrix. It starts without networking after release images have been preloaded.

## The Problem

Projects and developers maintain separate integration environments. The
differences create duplicated setup scripts, inconsistent behavior, slow
onboarding, and a support burden the platform team cannot manage reliably.

## Goals

* One platform-owned image for S3, SQS, and SNS pilot workflows.
* A preloaded sandbox starts offline within ten minutes for a new developer.
* Public lifecycle commands: start, state-preserving restart, status, diagnose,
  and destructive reset.
* Validated configuration, isolated state, and a proved SNS-to-SQS path.
* Two pilot migrations in 30 days with rollback.

## Non-Goals

* Broad AWS emulation, production semantics, IAM, or full console behavior.
* Kafka, RabbitMQ, WireMock, shared/remote sandboxes, Kubernetes, and generic
  configuration or control-plane systems.
* YAML secrets, automatic old-volume reuse, partial reset, or SNS administration
  in the console.

## Architecture

### Current state

Today, compose.yaml starts two services: MiniStack exposes S3 and SQS on port
4566 and the Quarkus StackEmu console exposes port 8080. MiniStack stores
emulator and S3 data in two named volumes; the console keeps operation history
in a third. Developers start both with Compose, configure the console through
environment variables, and have no supported project identity, reset contract,
or SNS capability. V1 retains MiniStack and the console code, replaces the
two-service runtime with one image, and does not import the three old volumes.

MiniStack remains the emulator base and must pass the pilot SNS matrix. The
image packages MiniStack, the existing Quarkus console, s6-overlay, and a
separate Java lifecycle JAR. The lifecycle JAR validates configuration, state
ownership, and image/state compatibility before starting s6-overlay. The
supervisor runs emulator and console separately; failure of either exits the
container.

A small platform-distributed host command named stackemu is the only supported
host interface. It reads and validates project configuration, derives the
volume name, creates and labels the volume when needed, then invokes Docker to
mount configuration and volume and start the image. It has no persistent
process and is released for Windows, macOS, and Linux beside the image. The
image repeats preflight against the mounted marker before state mutation.

The schema accepts only project ID, enabled services, initial resources,
namespace, persistence policy, and resource limits. It rejects unknown,
duplicate, incompatible, unsupported-version, and secret-like fields before
any state change. The project ID is lower-case, letter-led, and made only of
letters, digits, and hyphens. It resolves to one labelled volume. An ownership
marker records the project ID, state schema, compatible image range, and a
redacted configuration hash.

Start and reset use an exclusive volume lock. Bootstrap runs only for new state
or after reset. Restart never deletes or reconciles current state. Reset shows
its exact target, requires acknowledgement or CI-only --yes, erases all project
state, recreates declared resources, and audits its result outside erased state.
JSON status and diagnostics report redacted configuration, health, digest,
versions, effective public endpoints, usage, outcome, and log pointers.

Reset outcomes are retained as bounded structured records in a platform-managed
host audit directory keyed by project ID, not in erased project state. The
console reuses SmallRye Health endpoints; readiness adds marker, emulator,
enabled-service, and bootstrap checks.

The image uses a read-only root filesystem, dropped capabilities,
no-new-privileges, bounded temporary storage, documented resource limits, and
loopback ports by default. A digest preload manifest supports network-disabled
startup without pulls, builds, or external calls.

## Critical User Journeys

### Connectivity

The image listens internally on 127.0.0.1:4566 for AWS APIs and 127.0.0.1:8080
for the console. By default, the host publishes these on the same loopback
ports. Projects running on one machine may select distinct public ports in
stackemu.yaml; internal clients, bootstrap, and health checks always use 4566
and 8080 inside the container. SDK and AWS CLI examples use the selected public
AWS endpoint, region us-east-1, and explicit non-production test credentials.
The compatibility document contains reproducible requests, expected
responses, subscription policy setup, and a 10-second SNS-to-SQS deadline.

## Public contracts

The platform stackemu command is the only supported user entrypoint. It finds
stackemu.yaml in the current directory unless --config PATH is supplied.
Compose is an internal development and test harness only.

Commands are start, restart, status --json, diagnose --json, and reset. Exit
codes are: 0 success; 2 configuration; 3 Docker/image prerequisite; 4 ownership
or compatibility; 5 busy lock; 6 unready service; 7 incomplete reset. JSON
reports include status, cause, project ID, image digest, schema version,
configuration hash, effective public endpoints, volume usage, and log paths.

Schema v1 requires project.id and permits s3, sqs, and sns services; named
buckets, queues, topics, and SNS-to-SQS subscriptions; optional namespace;
persistence enabled/disabled; and numeric CPU, memory, PID, and storage limits.
Persistence is enabled by default. Unsupported resources and duplicate names
fail validation. The optional `ports` mapping accepts required integer fields
`aws` and `console`, each from 1024 through 65535, and the values must differ.
Without the block, ports remain 4566 and 8080. Docker binds selected ports only
to 127.0.0.1. If a port is occupied, start or restart exits with code 3 and the
`host-port-in-use` cause; StackEmu does not choose a replacement port. Changing
only ports updates the container mapping on `restart`, preserves the same named
volume, and does not require reset. A failed rebind restores the previous
container and its existing mapping. The marker stores a full configuration
hash and a state hash that excludes only `ports`; legacy markers use their
original full hash as the state hash.

The marker is .stackemu/marker.json at volume root. It records project ID,
schema, compatible image range, and config hash. The lock is
.stackemu/lifecycle.lock; stale locks are removed only after the owner process
is absent. Preflight validates config, Docker, volume label, marker, and image
range in that order. A failed bootstrap rolls back only its new resources. A
failed reset retains the lock and blocks start until diagnose reports recovery.

Reset displays project ID and requires typing that exact ID. Noninteractive
execution requires --yes and never infers CI. The host retains bounded 30-day
audit JSON records in a user-only platform-data directory.

The initial compatibility matrix is S3 bucket/object create/list/delete; SQS
queue create/list/delete and send/receive/delete message; SNS topic
create/list/delete, subscription create/delete, publish, and SNS-to-SQS
delivery. Every entry has SDK and CLI contract tests; all other operations are
unsupported.

Persistence disabled uses a per-run tmpfs rather than a Docker volume; it has
no marker, survives no restart, and reset is a no-op after confirmation.
Persistent locks contain the host command and Docker container ID. A new start
or reset refuses an active owner; after a failed reset the user fixes the
reported cause and reruns reset. V1 has no separate repair command.

## Risks and mitigations

* Image provenance: only digest-pinned, signed release images are accepted.
* Hostile volumes: label and marker mismatch refuse startup without mutation.
* Audit privacy: host audit files are user-only, bounded to 1 MiB/100 records.
* Disk growth: storage limits emit warnings and stop writes at the documented
  Docker-enforced limit; unsupported engines report observation-only limits.
* Docker variance: support is limited to documented Docker Desktop and Linux
  versions tested in release CI.
* SNS gaps: any failed matrix operation blocks pilot rollout and retains the
  old setup as rollback.

### Minimal configuration

```yaml
version: 1
project:
  id: payments-api-dev
services: [s3, sqs, sns]
resources:
  buckets: [invoices]
  queues: [invoice-events]
  topics: [invoice-created]
  subscriptions:
    - topic: invoice-created
      queue: invoice-events
persistence: enabled
limits:
  memoryMiB: 512
  pids: 256
  storageMiB: 1024
ports:
  aws: 4566
  console: 8080
```

Omit `ports` to keep the defaults. A second project could use `aws: 14566`
and `console: 18080`; local SDK/AWS CLI clients use
`http://127.0.0.1:14566`, and its console opens at
`http://127.0.0.1:18080`.

Namespace is optional and prefixes every declared resource. A configuration
cannot combine a namespace with an explicitly prefixed resource name. Schema
version 1 supports only the listed fields. The host command evaluates the
marker range: image major version must equal the marker major version and image
minor version must be at least the marker minor version. A mismatch exits with
code 4 and requires a compatible image or a documented backup restore.

Unsupported SDK operations return the AWS-style InvalidAction error with HTTP
501 where the emulator can express it; otherwise the host matrix test records
the emulator-specific response as unsupported and the console links to the
matrix. Audit records are capped at 1 MiB each and 100 records per project in
addition to the 30-day retention; excess records discard the oldest.

1. A developer starts a preloaded valid sandbox offline; it creates verified
   state, bootstraps resources, and becomes ready within ten minutes.
2. A developer restarts it; all state remains and JSON status returns health and
   version details.
3. A developer or CI resets it; all state is erased and declared resources are
   recreated only after acknowledgement.
4. A pilot uses documented SDK/CLI operations, including SNS publish to a
   subscribed SQS queue; unsupported calls fail deterministically.

## Alternatives Considered

* Current two-service Compose was rejected because it cannot satisfy the
  confirmed single-image and project-state contract.
* A launcher with separate images was rejected because v1 requires one image.
* A different emulator is rejected unless MiniStack fails the pilot matrix.
* A custom emulator and generic control plane were rejected for their initial
  maintenance cost.

## Detailed Implementation Plan

### Host launcher module

Create the Maven submodule stackemu-cli. Its native Windows, macOS, and Linux
launchers invoke the installed Docker CLI, not the Docker socket. The module
contains command parsing, YAML validation, Docker volume/container invocation,
auditing, and lifecycle tests. It derives volume name stackemu-project-id,
creates and labels an absent volume, rejects marker/label mismatches, and mounts
the accepted volume at /data/stackemu.

Before start/reset it writes .stackemu/lifecycle.lock with launcher and
container identity; concurrent work fails and normal completion removes it.
Reset stops the container, validates marker/configuration, requires typing the
exact project ID or explicit --yes, clears state, recreates the marker, then
bootstraps. Bootstrap creates buckets, queues, topics, resolves ARNs, creates
SNS-to-SQS subscriptions, validates resources, and records the config hash only
after complete success.

1. **Modify pom.xml:** add SNS SDK test support, YAML parsing, and a Maven
   execution that emits a runnable lifecycle JAR beside the Quarkus JAR.
2. **Create lifecycle classes:** LifecycleMain, StackEmuConfig,
   StackEmuYamlReader, ProjectId, VolumeIdentity, OwnershipMarker, VolumeLock,
   BootstrapService, ResetService, and DiagnosticReport under the lifecycle
   package. Implement commands, schema validation, state safety, diagnostics,
   redaction, and error codes.
3. **Modify application.properties; create HealthResource:** consume
   lifecycle-provided values, store operations under project state, retain
   loopback binding, and expose distinct liveness/readiness.
4. **Create Dockerfile, docker/entrypoint.sh, and docker/s6-overlay services:**
   produce the single digest-pinned image, preflight before supervision, and
   manage service order, signals, logs, and failure exits.
5. **Modify compose.yaml:** run one image with one configuration mount and one
   labelled volume; preserve loopback ports and constraints.
6. **Create stackemu.example.yaml, onboarding/configuration, compatibility,
   preload, and rollback documentation, plus README.md.**
7. **Create lifecycle unit tests and SandboxLifecycleIntegrationTest,
   OfflineStartupIntegrationTest, and SnsContractIntegrationTest.** Cover
   parser/marker/lock/reset behavior, one-image lifecycle, isolation, failure,
   offline start, and the complete published matrix.
8. **Create preload and offline verification scripts** for release digests and
   network-disabled test execution.

## Verification Plan

Run formatting, architecture, unit, packaging, container, SDK/CLI contract, and
offline-start tests. Migrate two clean-machine pilots, measure startup, rehearse
rollback, and stop broader rollout on state loss, isolation, compatibility,
migration, or rollback failure.

## Pilot Evidence (2026-11-12)

The following local checks were completed against the merged application. They
are runtime evidence, not a clean-machine or release qualification.

| Check | Result |
| --- | --- |
| SNS-to-SQS console flow | Created a topic and queue, subscribed the queue, published a unique payload, and received the SNS notification envelope containing that payload. |
| SNS-to-SQS integration test | `SnsSqsIntegrationTest`: 1 test passed, 0 failures. The test verifies delivery within the ten-second contract window. |
| Two-project runtime isolation | Ran two StackEmu image instances concurrently with separate project IDs, configuration files, labeled volumes, and host ports. Both Docker health checks were `healthy`; both console readiness endpoints returned HTTP 200. |
| S3/SQS/SNS inventory isolation | Six console checks passed: each project showed its own declared bucket, queue, and topic, and did not show the corresponding resource from the other project. |
| Cleanup | Removed both temporary containers, their named volumes, and temporary configuration files. The pre-existing `stackemu-example-api` container remained healthy on 4566/8080. |

The CLI-managed two-project integration test now starts both projects through
`stackemu`, verifies the effective endpoints from `status --json` and
`diagnose --json`, checks console readiness and public endpoint examples, and
confirms that S3, SQS, and SNS inventories remain isolated. It also verifies
SNS-to-SQS delivery within the ten-second contract, rejects an occupied host
port without losing a sentinel in the existing volume, and changes one
project's ports with `restart` while preserving that sentinel and the other
project. A separate injected unhealthy-image case verifies that a failed
replacement restores the previous container, port bindings, and volume data.
The pre-existing `stackemu-example-api` sandbox remained healthy on the default
4566/8080 ports throughout the tests.

The previously published baseline also passed the offline `docker-lifecycle`
job in [GitHub Actions run 43245774553](https://github.com/ERF88/StackEmu/actions/runs/43245774553).
That job confirmed the image was preloaded, rejected Docker pull/build commands,
blocked container egress, and ran the sandbox contract. The run predates the
current local configurable-port changes, so that offline contract must be
repeated after those changes are integrated.

The repository now defines a native CLI matrix for Linux x64/arm64, Windows
x64, and macOS x64/arm64. It builds and smoke-tests the native launcher on
each runner and uploads one artifact per target. Its first run against these
local changes is still pending. Clean-machine startup time remains an open
pilot check.

