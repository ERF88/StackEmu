# Goldfish annotated review report

**Status:** resolved

**Target:** docs/designs/stackemu-local-aws-sandbox-v1.md (feature scope)

## Prioritized findings and AI suggestions

### Connectivity contract — Major

* **AI Suggestion:** PROPOSE FIX
* **Proposed Edit:** Specify loopback ports, endpoint URLs, region, test
  credentials, and SDK/CLI examples.

### Persistence and reset recovery — Major

* **AI Suggestion:** PROPOSE FIX
* **Proposed Edit:** Define disabled persistence as an ephemeral tmpfs mode;
  add recovery status and guarded repair after failed reset; define lock fencing
  metadata and cross-platform timeout behavior.

### Risks and mitigation — Major

* **AI Suggestion:** PROPOSE FIX
* **Proposed Edit:** Add a risk register for image provenance, hostile local
  volumes, audit privacy, disk growth, Docker platform variance, and SNS gaps.

### Document structure and health reuse — Major

* **AI Suggestion:** PROPOSE FIX
* **Proposed Edit:** Nest current-state content beneath architecture, separate
  public contracts from user journeys, and explicitly reuse SmallRye Health.

### Offline and matrix acceptance — Major

* **AI Suggestion:** PROPOSE FIX
* **Proposed Edit:** Define the reference machine, measurement window, matrix
  request/response expectations, and SNS-to-SQS authorization/timing.

### Reset wording — Nit

* **AI Suggestion:** PROPOSE FIX
* **Proposed Edit:** State consistently that --yes is permitted only as an
  explicit noninteractive override and never inferred from CI.
