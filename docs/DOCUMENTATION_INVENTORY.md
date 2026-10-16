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

## Current operational expansion / Ampliação operacional atual

The historical inventory above is not current implementation evidence. First-delivery requirements are tracked in [S3 operational scope](product/s3-management-scope.md) and [scope-only ADR-0014](decisions/ADR-0014-expanded-operational-s3-scope.md). [Explorer contracts](development/s3-explorer.md) and [UX-2A evidence](spikes/ux-2a-results.md) distinguish the native-table slice from pending drawer, preview, clipboard, signing, copy/move and deletion work.

O inventário histórico acima não comprova implementação atual. Os links registram requisitos ampliados, decisão de escopo, contratos e evidência, separando a tabela nativa das capacidades ainda pendentes.

UX-2B now adds metadata drawer and bucket/key clipboard, not the remaining operational capabilities. Consult [ADR-0015](decisions/ADR-0015-object-drawer-clipboard.md) and [its evidence](spikes/ux-2b-results.md), which supersede the drawer/clipboard pending labels in the preceding inventory summary only. / UX-2B adiciona drawer e clipboard de bucket/chave, não as demais capacidades; documentos vinculados substituem somente esses rótulos pendentes do resumo anterior.

UX-3 upload selection/progress contracts and fresh results: [development](development/s3-upload.md), [ADR-0016](decisions/ADR-0016-single-file-upload-progress.md), [execution](spikes/ux-3-results.md). This does not complete preview, deletion, signing or copy/move. / Upload UX-3 tem contratos/evidências próprios nos links, sem concluir demais capacidades.

UX-4 notice/confirmation contract and fresh evidence: [ADR-0017](decisions/ADR-0017-operation-notifications.md), [execution](spikes/ux-4-results.md). No destructive modal is implemented. / Contrato/evidência UX-4 vinculados, sem modal destrutivo implementado.

UX-5A bounded text preview: [contract](decisions/ADR-0018-bounded-text-preview.md), [fresh evidence](spikes/ux-5a-results.md). Raster remains UX-5B work. / Preview de texto limitado nos links; imagens continuam na UX-5B.

UX-5B static PNG/JPEG image integration: [ADR-0019](decisions/ADR-0019-raster-preview-isolation.md), [current local evidence](spikes/ux-5b-results.md). Earlier [worker-only evidence](spikes/ux-5b-worker-results.md) remains historical; consolidated new-image validation is deferred to the final stage. / Integração de imagens e evidência atual nos links; registro anterior do worker é histórico. Validação consolidada com imagem nova ao final.

## Legal-name validation / Validação jurídica do nome

`StackEmu` was chosen on 13 October 2026. The project repository is `ERF88/StackEmu`. Legal, registry, domain, Docker Hub, package-registry, and INPI validation is pending. Do not announce trademark availability or affiliation with AWS, MiniStack, LocalStack, or Moto.
