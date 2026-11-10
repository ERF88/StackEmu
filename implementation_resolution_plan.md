# Implementation readiness resolution plan

Before implementation, the design must define:

1. Exact source, packaging, release, and Docker invocation contract for the
   cross-platform host stackemu command.
2. Exact Dockerfile stages, pinned images, s6 service scripts, ports, users,
   writable paths, probes, and lifecycle-JAR launch boundary.
3. YAML, marker, lock, labels, audit, command/exit-code, bootstrap, reset, and
   recovery schemas and algorithms.
4. Existing JdbcOperationStoreAdapter integration and H2 state migration.
5. Repository-relative Create/Modify/Delete file allowlist, dependencies, and
   reproducible integration fixtures/assertions.

## Accepted decisions

* Create a Maven submodule named stackemu-cli in this repository.
* Build native launchers for Windows, macOS, and Linux with the image release.
* Invoke the installed Docker CLI; do not implement the Docker socket protocol.
* Source layout: stackemu-cli/src/main/java for CLI/config/Docker services and
  stackemu-cli/src/test/java for contract tests.
* Volume algorithm: validate project ID, derive stackemu-project-id, create it
  with labels when absent, reject label/marker mismatch, and mount it at
  /data/stackemu.
* Start/reset create .stackemu/lifecycle.lock with launcher/container identity;
  concurrent operations fail and normal completion removes the lock.
* Reset stops the container, validates marker/configuration, requires the exact
  project ID or explicit --yes, clears state, recreates marker, then bootstraps.
* Bootstrap creates buckets, queues, and topics, resolves ARNs, creates SNS to
  SQS subscriptions, validates resources, then records the configuration hash.
