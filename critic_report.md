# Critic Report — StackEmu Local AWS Sandbox v1

## Scope

Reviewed `docs/designs/stackemu-local-aws-sandbox-v1.md`, limited to the feature range before `Detailed Implementation Plan`, against the EGM feature-spec standard. Findings below are synthesized from isolated design-critic and structural reviews.

## P1 — Must resolve before implementation

1. **Client connectivity contract is unspecified** (`Public contracts`, lines 96–127)
   
   The document does not define the SDK/CLI endpoint and port, region, credentials, protocol, or configuration path for clients using the combined image. The current-state reference to port `4566` is descriptive rather than a v1 contract.
   
   **Recommendation:** Define exact S3/SQS/SNS endpoint mappings, published ports, default region and credentials, configuration variables, and SDK/CLI examples.

2. **Persistence modes are contradictory and incomplete** (`Goals and constraints`; `Public contracts`, lines 60–73 and 106–110)
   
   `persistence: enabled/disabled` is accepted, yet the host command always derives and creates a labelled volume, while markers and locks presume a volume. The disabled lifecycle, storage location, cleanup/reset behavior, and diagnostics are undefined.
   
   **Recommendation:** Specify disabled persistence precisely (for example per-run tmpfs with no managed volume), including its marker, lock, bootstrap, status, cleanup, and reset rules.

3. **Failed reset can strand a project** (`Lifecycle/reset contract`, lines 112–121)
   
   A failed reset retains the lock and blocks start until `diagnose` reports recovery, but `diagnose` is report-only. There is no recovery action, validity condition, or safe procedure for lock release.
   
   **Recommendation:** Add a recovery state machine plus a guarded recovery/repair command, the evidence required before releasing a lock, and rollback/manual-remediation behavior.

4. **Stale-lock handling is unsafe across target hosts** (`Lifecycle/reset contract`, lines 112–117)
   
   “Owner process is absent” does not define stored identity, verification, PID namespace behavior, or Windows/macOS semantics. PID reuse can permit concurrent mutation; unavailable host inspection can deadlock the volume.
   
   **Recommendation:** Define lock metadata, liveness verification, fencing/timeout rules, and cross-platform behavior for Docker Desktop and Linux.

5. **Required risk treatment is absent**
   
   The feature-spec standard requires explicit failure modes, security implications, scalability limits, and mitigations. Existing controls do not cover image supply-chain trust, malicious or mislabelled volumes, host-audit privacy/permissions, volume growth, cross-platform Docker differences, or SNS-emulator incompatibility.
   
   **Recommendation:** Add a risk-and-mitigation section with triggers, owner, mitigation, rollback/stop criteria, and residual limits for each material risk.

6. **Architecture heading is structurally empty** (`Architecture` / `Current state`)
   
   `Current state` is a peer H2 directly under `Architecture`, leaving the latter without content, although its content is architectural.
   
   **Recommendation:** Make `Current state` an H3 under `Architecture`, or remove/rename the parent heading.

7. **Critical User Journeys heading is structurally empty** (`Critical User Journeys` / `Public contracts`)
   
   `Public contracts` is a peer H2, while the journeys are an unlabeled ordered list at its end.
   
   **Recommendation:** Put the four journeys under `Critical User Journeys`, preferably as consistently structured subsections. Place `Public contracts` separately or beneath its relevant parent.

## P2 — Resolve before approval

1. **Destructive reset rules conflict** (`Goals and constraints`, lines 75–78; `Lifecycle/reset contract`, lines 119–120)
   
   One section restricts `--yes` to CI, while another permits it for any noninteractive reset and says CI is never inferred.
   
   **Recommendation:** Select one rule and state interactive and noninteractive behavior consistently.

2. **Offline/startup target is not measurable** (`Goals`, `Success criteria`, lines 15–16, 26–31, 87–90)
   
   “Offline within ten minutes” does not identify machine/Docker baseline, timing boundary, preload verification, or failure budget.
   
   **Recommendation:** Specify the reference environment, start/end measurement points, image/preload preconditions, and threshold/percentile.

3. **Compatibility matrix is not reproducible** (`Compatibility matrix`, lines 123–127)
   
   Operation names do not define SDK/CLI versions, parameters, expected AWS/HTTP errors, ordering/eventual-consistency assumptions, or SNS-to-SQS policy behavior.
   
   **Recommendation:** Version the matrix and give concrete request/response/error expectations, including subscription authorization, queue-policy setup, and delivery timing.

4. **Public-contract section mixes unrelated contract types** (`Public contracts`)
   
   Architecture, client configuration, lifecycle semantics, compatibility, and operational/audit behavior are combined in one section, weakening scanability and feature-spec flow.
   
   **Recommendation:** Separate coherent sections such as Public Interface, Configuration and State, Lifecycle/Reset, Compatibility, and Operations/Audit.

## Severity summary

- P0: 0
- P1: 7
- P2: 4
- P3: 0
