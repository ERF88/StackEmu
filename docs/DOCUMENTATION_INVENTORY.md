# Documentation inventory / Inventário de documentação

This inventory prioritizes the documentation required by plan section 14 and its execution plan. Status describes documentation maturity, not product implementation. **All technical claims remain unverified unless a future spike links reproducible evidence.**

| Priority | Document                                                                          | Status                    | Purpose                                                  |
| -------- | --------------------------------------------------------------------------------- | ------------------------- | -------------------------------------------------------- |
| P0       | `README.md`, `README.pt-BR.md`                                                    | Draft                     | Scope, safety boundary, future quick start               |
| P0       | `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `CHANGELOG.md` | Draft                     | Open-source governance                                   |
| P0       | `docs/product/user-research.md`                                                   | Hypotheses only           | Prevent invented user research                           |
| P0       | `docs/product/roadmap.md`                                                         | Draft                     | Phased delivery and gates                                |
| P0       | `docs/architecture/overview.md`, `modules.md`                                     | Draft                     | Logical boundaries and dependency direction              |
| P0       | `docs/architecture/emulator-compatibility.md`                                     | Baseline only             | Honest compatibility matrix                              |
| P0       | `docs/configuration.md`, `docs/security.md`, `docs/known-limitations.md`          | Draft                     | Safe operation and explicit non-goals                    |
| P0       | `docs/services/s3.md`                                                             | Draft                     | MVP S3 behavior and exclusions                           |
| P0       | `docs/design-system.md`                                                           | Draft                     | Original visual system informed by an external reference |
| P0       | `docs/development/testing.md`, `release.md`                                       | Draft                     | Quality and release expectations                         |
| P0       | `docs/decisions/ADR-0001`–`0003`                                                  | Accepted planning ADRs    | Record initial decisions                                 |
| P0       | `docs/spikes/`                                                                    | Placeholders              | Evidence required before implementation decisions        |
| P1       | Verified Compose quick start                                                      | Blocked by Phase 0        | Must be tested before being presented as working         |
| P1       | Published benchmark and compatibility results                                     | Blocked by implementation | Must identify versions and reproducible runs             |
| P2       | Module-authoring guide and guided labs                                            | Post-MVP                  | Requires real module contract and verified labs          |

## Bilingual policy

See [coding conventions](development/code-style.md) for the per-file standards and required contribution checks.

Implementation follow-ups: [Phase 1 guide](development/phase-1.md), [validation](spikes/phase-1-results.md) and [ADR-0004](decisions/ADR-0004-phase-1-boundaries.md) document the current layers, effective configuration, shared UI and remaining limitations.

The product and documentation target English and Brazilian Portuguese. Root documents have paired `pt-BR` editions where they are user-facing. Technical documents are written bilingually in the same file during this planning baseline to keep decisions synchronized; implementation documentation should preserve that practice or add paired files with a translation check.

## Legal-name validation

`StackEmu` was chosen on 13 October 2026. The project repository is `ERF88/StackEmu`. Legal, registry, domain, Docker Hub, package-registry, and INPI validation is pending. Do not announce trademark availability or affiliation with AWS, MiniStack, LocalStack, or Moto.
