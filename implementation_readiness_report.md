# Stage 9: Implementation Readiness Report

**Target:** `docs/designs/stackemu-local-aws-sandbox-v1.md`, Detailed
Implementation Plan (lines 207–238)

## Findings

### Blocker — The plan has no enforceable file boundary

**Affected scope:** implementation tasks 1–8.

The plan names only `pom.xml`, `application.properties`, `compose.yaml`, and a
few new top-level artifacts. It does not give repository-relative paths for the
host `stackemu` command, lifecycle package, test sources/resources, Docker/s6
files, documentation files, or scripts; nor does it mark every path Create,
Modify, or Delete. It also does not identify the current console classes that
must consume lifecycle state. This fails the required Phase 5 write allowlist
and leaves the implementation scope open to adjacent refactors.

**Required change:** replace each broad task with a complete, Create/Modify/Delete
file inventory and file-level responsibilities, including every test fixture and
generated-output glob.

### Blocker — The supported host entrypoint is not planned

**Affected scope:** task 2; architecture and Public contracts.

The design makes `stackemu` the only supported host interface: it locates and
validates configuration, creates/labels the volume, chooses mounts, invokes
Docker, performs reset acknowledgement, and writes host audits. Task 2 instead
creates a lifecycle JAR inside the image and does not create or package a host
binary at all. A process inside the target container cannot select that
container's mounts or create the volume before it starts.

**Required change:** name the host-command module/files, language and packaging
strategy for Windows/macOS/Linux, Docker invocation/API, exact host data/audit
directory resolution and permissions, command parsing/output implementation,
and its handoff contract to the in-image lifecycle JAR.

### Blocker — Lifecycle state models and destructive-state coordination are unspecified

**Affected scope:** tasks 1–5.

“Implement commands, schema validation, state safety” does not specify
executable behavior per file. The plan leaves the implementer to choose the YAML
object model, canonical/redacted configuration-hash representation, marker and
lock JSON schemas, volume-label values, command-to-exit-code mapping, lock
acquisition/release/crash behavior, stale-owner verification, the host versus
container authority, and failed-reset recovery. Those choices govern the
destructive state contract and cannot be delegated to an implementer.

**Required change:** make these interfaces and algorithms explicit in the
relevant file steps, including exact config/marker/lock/audit JSON fields,
validation and recovery decision tables, command mappings, and an atomic
cross-process protocol for every start/reset race and failure path.

### Blocker — Bootstrap/reset algorithms are not selected

**Affected scope:** tasks 2 and 7.

The plan does not specify resource-creation ordering and resolved identifiers
(including topic/queue ARNs before subscriptions), idempotency rules, the
definition of a resource newly created by a failed bootstrap, rollback cleanup
order, reset deletion/recreation sequencing, or individual recovery-state
assertions. “State safety” and “complete published matrix” leave the core
mutation behavior to discovery.

**Required change:** state the precise algorithm, decisions, and expected
results for bootstrap, reset, partial bootstrap failure, and reset failure.

### Blocker — The single-image runtime is not executable from the plan

**Affected scope:** tasks 1, 4, and 5.

The plan omits the pinned MiniStack base digest; build stages and artifact-copy
contract; lifecycle-JAR launch contract; s6 service names, run/finish scripts,
dependencies, environment and probes; user/UID ownership; writable mount/tmpfs
paths; and port wiring. The current Compose runtime instead uses
`ministackorg/ministack:1.5.25` alongside a separately built console and has no
current Dockerfile from which those choices can be inferred.

**Required change:** provide the full Docker/s6 file list and their exact image,
build, filesystem, process, and health definitions.

### Blocker — Existing console integration is not described

**Affected scope:** task 3.

The existing application uses `JdbcOperationStoreAdapter` with
`stackemu.operations.path`, and already includes SmallRye Health. The plan does
not identify the adapter/resources that consume lifecycle-provided values, how
the existing H2 operation store is located/recovered in project state, or the
concrete SmallRye health checks. It instead names a proposed `HealthResource`
without deciding its role or need.

**Required change:** name the existing production files, their exact changes,
the operation-store migration/configuration, and the CDI health check classes
and checks.

### Major — Build dependencies and artifact assembly are underspecified

**Affected scope:** task 1.

“Add SNS SDK test support, YAML parsing, and a Maven execution” does not name
the dependency coordinates/versions/scopes, lifecycle source-set/module,
main-class manifest, shading/assembly method, output names, or the Docker copy
paths. It also does not decide whether the lifecycle program uses the existing
Quarkus build or a separate plain-Java executable. These are material build and
runtime choices.

**Required change:** specify the exact `pom.xml` changes and resulting artifact
names/locations consumed by Docker and release packaging.

### Major — Compose and the test harness are not reconciled with the public CLI contract

**Affected scope:** tasks 5 and 7.

The current `compose.yaml` is a two-service development harness with three
named volumes. The plan says to modify it to run one image with one mounted
configuration and labelled volume, but does not say whether it remains an
internal fixture, how it receives the dynamically derived volume and config,
or how developers invoke it without bypassing the only supported `stackemu`
interface. It does not state which current integration-test harnesses are
retained or replaced.

**Required change:** define Compose's internal role, invocation, generated
inputs, and explicit relationship to the host command; list the precise test
harness changes.

### Major — Reset audit persistence has no implemented destination

**Affected scope:** tasks 2, 3, and 5.

The design requires bounded reset records in a platform-managed host directory
outside erased state. No task creates the host audit writer or defines its
directory/mount relationship; task 5 instead limits the container harness to a
configuration mount and one labelled state volume. A reset that erases that
volume cannot satisfy the retention contract.

**Required change:** assign the host audit writer, repository files, directory
resolution, permissions, record format, retention cleanup, and tests.

### Major — Test and verification instructions cannot prove the contracts

**Affected scope:** tasks 6–8 and Verification Plan.

The three proposed test class names have no paths, framework/profile, image
build/preload fixture, host-platform coverage, Docker cleanup/isolation rules,
or assertions tied to the public exit-code/JSON schemas and failure states.
“Complete published matrix” does not name the matrix file, version, requests,
expected responses, queue policy, 10-second delivery observation, or expected
unsupported-operation responses. The Verification Plan similarly does not give
the commands, prerequisites, network-disabling mechanism, timing measurement,
or pass/fail thresholds.

**Required change:** define each test's location, setup, command, mocks versus
Docker requirement, exact assertions, cleanup, and platform matrix; define the
versioned compatibility artifact and scripts with concrete invocations and
acceptance thresholds.

### Nit — Task 6 is an imprecise document bundle

**Affected scope:** task 6.

“Onboarding/configuration, compatibility, preload, and rollback documentation”
does not identify filenames, section ownership, generated-versus-handwritten
content, or the source of the documented matrix and image digests.

**Required change:** list each document path and its authoritative inputs.

## Severity Summary

| Severity | Count |
| --- | ---: |
| Blocker | 6 |
| Major | 4 |
| Nit | 1 |
