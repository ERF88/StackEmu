# Emulator compatibility / Compatibilidade de emuladores

## Compatibility policy / Política

Connecting is not compatibility. A compatibility statement requires a pinned emulator version, documented operations, reproducible tests, known limitations, skipped tests and reasons, and any account/token/license requirement.

Conectar não é compatibilidade. Uma afirmação exige versão fixa do emulador, operações documentadas, testes reproduzíveis, limitações conhecidas, testes ignorados e motivo, além de requisitos de conta/token/licença.

## Current matrix / Matriz atual

Phase 2 locally verified MiniStack 1.5.25: bucket/head/tag reads, `ListObjectsV2` with common prefixes/continuation tokens, `HeadObject` and `GetObject` streaming. See [Phase 2 evidence](../spikes/phase-2-results.md). This extends the historical Phase 0 matrix below; it does not certify all S3 semantics or other emulators.

A Fase 2 verificou localmente essas leituras, paginação/prefixos e streaming em MiniStack 1.5.25. Não certifica todas as semânticas S3 nem outros emuladores.

| Emulator                       | Role in plan                     | Tested version | Verified operations                            | Status                                                                               |
| ------------------------------ | -------------------------------- | -------------: | ---------------------------------------------- | ------------------------------------------------------------------------------------ |
| MiniStack                      | Primary Phase 0 reference        |         1.5.25 | Create/list/delete empty test bucket; GUI list | Locally verified for this narrow scope; see [evidence](../spikes/phase-0-results.md) |
| LocalStack                     | Progressive compatibility target |   Not recorded | None                                           | Unverified                                                                           |
| Moto                           | Progressive compatibility target |   Not recorded | None                                           | Unverified                                                                           |
| Generic S3-compatible endpoint | Fallback classification          |            N/A | None                                           | Unverified                                                                           |

## Capability model / Modelo de capacidades

Optional capabilities are `unknown`, `supported`, or `unsupported`. An HTTP 200 alone does not establish support. Automatic detection is best-effort: retain the evidence used, prefer explicit emulator-specific probes, and fall back to `generic` if no reliable probe succeeds.

## Planned common suite / Suíte comum planejada

Once implemented, test only shared semantics: endpoint connectivity, bucket list/create, object listing with prefix/pagination, upload, and streaming download. Emulator-specific cases may be conditional and must remain visible in the matrix.
