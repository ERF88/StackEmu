# Stage 6: comprehension test

## What this proposes

StackEmu v1 replaces inconsistent local environments with a single,
platform-maintained Docker sandbox for a limited S3/SQS/SNS contract. A host
command validates project configuration, prepares labelled state, and starts
the image; restart preserves state and reset is explicit and destructive.

## How the existing system works, per this document

The document describes a two-service Compose setup built from MiniStack and a
Quarkus console, but does not describe its topology, state, startup/reset
behavior, or source ownership sufficiently to explain migration.

## Findings

* **Major — current-state migration:** Describe the existing services, three
  volumes, startup/reset behavior, and the retained/replaced files.
* **Major — compatibility matrix:** Embed or link the exact pilot S3/SQS/SNS
  operation matrix, expected results, exclusions, and pass criteria.
* **Major — public CLI contract:** Specify command syntax, configuration
  discovery, Docker prerequisites, error/exit codes, and JSON output schemas.
* **Major — configuration schema:** Specify fields, types, defaults, permitted
  resources/services, incompatibilities, and semantics for namespace,
  persistence, and limits.
* **Major — state/reset contract:** Define marker location/format, compatibility
  range, lock/stale-lock behavior, preflight ordering, failed reset/bootstrap,
  audit location/retention/permissions, and acknowledgement semantics.
* **Major — launcher versus Compose:** State that Compose is internal or define
  its equivalent supported behavior; the host command must remain the only user
  entrypoint.
* **Nit — component definitions:** Add short responsibility/version statements
  or links for MiniStack, the console, s6-overlay, and SmallRye Health.
