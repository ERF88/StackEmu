# StackEmu Local AWS Sandbox v1

* **Author:** Eduardo Rodrigo Ferreira
* **Date:** 2026-11-07

## Overview

StackEmu v1 will give each project one supported local AWS sandbox managed by the platform team. It will be delivered as one minimal Docker image containing the S3, SQS, and SNS emulator, StackEmu web console, internal process supervisor, health and readiness checks, diagnostics, a versioned default bootstrap, and `start`, `status`, `diagnose`, and `reset` commands.

The image must start without network access after its immutable image has been preloaded. Project state is outside the image in one isolated persistent volume derived from a required stable `project.id`. Each project supplies one versioned `stackemu.yaml`, limited to project ID, services, initial resources, namespace, persistence policy, and resource limits. Secrets come only from environment variables or deployment secret facilities; they never enter that file or diagnostic output.

This replaces the proof of concept as the product contract. The repository currently runs two Compose services, MiniStack and a separately built Quarkus GUI. It enables S3 and SQS, uses three named volumes, binds ports to loopback, and has useful GUI hardening and S3/SQS coverage. It has no SNS support, single-image supervisor, project schema, or lifecycle command interface. The existing implementation is evidence and reusable code, not the v1 contract.

## Analysis

### Risks and counterarguments

* **One image creates a new failure and upgrade boundary.** Combining emulator, console, supervisor, bootstrap, and diagnostics adds signal forwarding, child reaping, log routing, readiness ordering, and joint image/state compatibility work. V1 will use the standard small `s6-overlay` supervisor rather than a custom supervisor. Each child has separate logs, and failure of the emulator or console ends the container with an error exit code. Image metadata defines a supported state-schema range; an incompatible image refuses to start.
* **State identity can cause data loss or cross-project reuse.** Current state spans `ministack-state`, `ministack-s3`, and `stackemu-operations`. V1 defines `project.id` as lower-case, letter-led, and containing only letters, digits, and hyphens. It deterministically maps this ID to one labelled volume. An in-volume ownership marker stores the project ID, state-schema version, compatible image range, and applied configuration hash. Start, diagnose, and reset refuse missing, foreign, or mismatched state before touching it.
* **Bootstrap, restart, and changed configuration can conflict.** Bootstrap runs only for new state and after reset. It creates declared resources if absent, never deletes or reconciles existing resources during a normal restart, records its schema/config digest only after success, and reports conflicts rather than guessing. Changing an immutable declaration requires explicit reset or a later migration feature.
* **A small YAML can become a control plane.** V1 has one schema version and discovery rule; rejects unknown fields, duplicates, invalid namespaces, incompatible service options, and secret-like content before container or volume mutation; and returns field-specific corrections. It excludes overlays, templating, dynamic reload, interpolation, remote configuration, generic reconciliation, and schema migration. Diagnostics show only redacted effective configuration.
* **SNS is a delivery contract, not a service checkbox.** The baseline has S3/SQS dependencies and tests but no SNS dependency, adapter, UI, or tests. SNS is included only through a published pilot operation matrix, including SNS-to-SQS delivery and persistence. Its console posture is explicit; SDK/CLI support need not imply SNS console administration.
* **AWS-compatible would be misleading.** V1 promises only exact AWS SDK and AWS CLI versions and operations in a versioned matrix, with known differences and deterministic unsupported-operation errors. Every claimed entry has a contract test. It makes no broad AWS emulation or production-equivalence claim.
* **Offline start is not offline build.** The current GUI build downloads Node, npm, and dependencies. The start route must never build, pull, or call outside endpoints. The platform team provides a digest-pinned preload manifest and proves network-disabled startup after preload. Missing-image errors name the required digest and preload action.
* **Reset must cover all state and fail safely.** Reset erases every state domain for the resolved project and recreates declared resources; partial reset is excluded. Start and reset hold an exclusive lock in the project volume. Reset validates config and ownership, displays project ID, exact target, scope, and volume size, then requires a distinctive confirmation. `--yes` is the CI bypass only. Partial failure is explicit and blocks normal start until resolution; the outcome is recorded in a lifecycle log outside the erased state.
* **Health must mean more than processes running.** Liveness reports the supervisor and every child. Readiness additionally requires valid config, verified state ownership, emulator health, bounded enabled-service checks, and complete bootstrap. `status --json` and `diagnose --json` distinguish ready, starting, degraded, and failed, and include cause, image digest, config/schema version, volume usage, log pointers, and no secrets.
* **Limits vary by Docker environment.** The GUI already has read-only filesystem, dropped capabilities, no-new-privileges, PID/memory limits, and bounded temporary storage; the emulator does not. V1 defines its supported Docker engine/version, enforces portable CPU, memory, and PID limits for each process, and reports disk usage and low-space warnings. It reports whether storage limits are enforced or merely observed.

### Why this is worth delivering

One platform-owned image, one non-secret project file, and one state identity remove per-project local setup drift. The current loopback bindings, S3/SQS GUI flows, health endpoint, operation history, and environment-based connection settings are useful starting points. The value remains bounded: a newcomer can preload, start, inspect, use a documented endpoint, and reset only their project's state without learning internal orchestration.

## Goals vs. Non-Goals

* **In-Scope Goals:**
  * Deliver one platform-managed minimal image and one lifecycle interface: `start`, state-preserving `restart`, `status`, `diagnose`, and destructive `reset`.
  * Use `s6-overlay` to supervise the emulator and console, keep their logs separate, and exit the image when either required component fails.
  * Support only S3, SQS, and SNS operations in the published SDK/AWS CLI matrix for the two pilots.
  * Start offline after a digest-pinned preload, proven on a network-disabled host.
  * Use stable `project.id` for one labelled, marker-verified external persistent volume.
  * Accept one small versioned `stackemu.yaml` containing only project ID, services, initial resources, namespace, persistence policy, and resource limits; validate before mutation.
  * Keep secrets outside YAML and redact them everywhere.
  * Bootstrap declared resources idempotently on first start and reset only. Restart retains state without reconciliation or deletion.
  * Require interactive reset confirmation or `--yes`, recreate declared resources, and report reset outcome.
  * Provide component-level readiness and machine-readable diagnostics with non-secret effective config, image/state identity, usage, and lifecycle outcome.
  * Migrate two named pilots in 30 days and measure a scripted offline newcomer path under 10 minutes.
* **Out-of-Scope Non-Goals:**
  * Services beyond S3, SQS, and SNS; broad AWS API, IAM, production-semantics, or general AWS compatibility claims.
  * Kubernetes, remote/multi-user sandboxes, shared state, public exposure by default, multi-node operation, or high availability.
  * Multiple config files, overlays, templating, YAML secrets, auto migration, continuous reconciliation, import/export, backup/restore as a user feature, or a generic plugin/control-plane framework.
  * Dashboard metrics backends, cloud telemetry, automatic updates, support uploads, or full SNS console administration.
  * Partial reset, silent migration from old global Compose volumes, or automatic reuse of unverified state.

## Alternatives Weighed

* **Retain current two-service Compose.** Rejected by the confirmed v1 direction. It remains the baseline and rollback reference but cannot provide the single owned image, strict project state contract, or lifecycle surface.
* **Use one launcher with separate images.** Rejected by the confirmed single-image decision. Its simplicity argument is addressed by the required supervisor and lifecycle safeguards inside the supported image.
* **Build a generic configuration platform now.** Rejected. V1 uses one small schema and first-initialization bootstrap, not a reconciliation or migration framework.
* **Promise ordinary AWS SDK/AWS CLI compatibility.** Rejected. The versioned operation matrix, known exclusions, and contract tests are the sole compatibility promise.
* **Persist in the image or reuse global volumes.** Rejected. State must survive image replacement but be attributable to one stable project identity.
* **Allow reset without explicit destructive acknowledgement.** Rejected. Interactive confirmation or CI-only `--yes` is mandatory.

## Key Decisions & Staging for Next Steps

1. **Freeze contracts.** Select emulator and base image; write the image lock/preload manifest; define command grammar and exit codes, the `s6-overlay` process tree, signal/log behavior, readiness deadlines, the `project.id` grammar, volume-name algorithm, labels, ownership marker, schema v1, image compatibility range, and console capability for each service.
2. **Prove lifecycle and state safety.** Build a thin single-image spike with `s6-overlay`, per-component liveness/readiness, deterministic external volume mapping, exclusive volume locks for start/reset, strict preflight, and first-init bootstrap. Test bad/unknown/secret config, collision/foreign-volume refusal, concurrent starts, restart preservation, reset confirmation/failure, lifecycle audit logging, and project isolation.
3. **Define upgrade and migration policy.** Do not silently import the current three global volumes. An image outside the marker's compatible range refuses to start. Rollback is allowed only within that range or by restoring a backup. If migration is supported, require backup/snapshot, versioned state metadata, interruption recovery, and image/state rollback rehearsal. Otherwise require clean v1 state and preserve the old setup for rollback.
4. **Add service contracts, not broad features.** The initial SNS matrix covers creating, listing, and deleting topics and queues; creating and deleting subscriptions; publishing messages; and SNS-to-SQS delivery. Each has SDK and CLI contract coverage. Run every matrix operation after cold start, restart, reset/rebootstrap, and offline start. Unsupported calls must point to the matrix.
5. **Rehearse offline release.** Preload every digest, disconnect networking, then verify readiness without pull, build, or external call. Exercise killed child, failed health, low-space state, bootstrap conflict, and malformed config through diagnose.
6. **Run pilots with rollback.** Preserve each pilot's old setup, set rollback owner and target time, then time the clean-machine journey: prerequisite check, preload, offline start, SDK/CLI matrix actions, console access, restart preservation, and controlled reset. Pilot two begins only after pilot-one gaps are fixed or explicitly excluded.
7. **Review at day 30.** Success requires two completed pilot migrations and measured newcomer startup below 10 minutes. Any isolation failure, data loss, failed required operation, failed migration, or failed rollback stops wider rollout.
