# Emulator compatibility / Compatibilidade de emuladores

## Compatibility policy / Política

Connecting is not compatibility. A compatibility statement requires a pinned emulator version, documented operations, reproducible tests, known limitations, skipped tests and reasons, and any account/token/license requirement.

Conectar não é compatibilidade. Uma afirmação exige versão fixa do emulador, operações documentadas, testes reproduzíveis, limitações conhecidas, testes ignorados e motivo, além de requisitos de conta/token/licença.

## Current matrix / Matriz atual

V6 follow-up, 2026-10-17: MiniStack **1.5.25** was the sole emulator for clean host Java integration and the fresh Linux/amd64 GUI image's full browser/write/streaming checks. [V6 execution](../evidence/visual-v6-results.md) records 107 Java/157 browser cases, original attachment/hash/CSRF checks, bounded text/static-image previews and 128 MiB streaming with 64 MiB heap. This extends the dated evidence below, not all S3/version/deletion/signing semantics or other emulator compatibility. No production resources were targeted. / MiniStack foi o único emulador; V6 amplia evidência local, não certifica semânticas pendentes/outros emuladores. Sem recursos de produção.

UX-2B local integration additionally verifies URL-encoded ListObjectsV2 responses with MiniStack 1.5.25/AWS SDK 2.54.13: exact CR/LF, Unicode, plus and literal percent values in object keys and common prefixes, without decoding pagination tokens. This narrowly corrects XML normalization observed during drawer testing; it is not certification of other emulators, all control characters or mutation/version semantics. [Fresh execution record](../evidence/ux-2b-results.md). / Integração local UX-2B verifica preservação desses caracteres/chaves/prefixos com resposta codificada URL, sem alterar tokens. Correção delimitada, não certificação ampla de emuladores, controles, mutações ou versões.

Phase 2 locally verified MiniStack 1.5.25: bucket/head/tag reads, `ListObjectsV2` with common prefixes/continuation tokens, `HeadObject` and `GetObject` streaming. See [Phase 2 evidence](../evidence/phase-2-results.md). This extends the historical Phase 0 matrix below; it does not certify all S3 semantics or other emulators.

A Fase 2 verificou localmente essas leituras, paginação/prefixos e streaming em MiniStack 1.5.25. Não certifica todas as semânticas S3 nem outros emuladores.

| Emulator                       | Role in plan                     | Tested version | Verified operations                            | Status                                                                                 |
| ------------------------------ | -------------------------------- | -------------: | ---------------------------------------------- | -------------------------------------------------------------------------------------- |
| MiniStack                      | Primary Phase 0 reference        |         1.5.25 | Create/list/delete empty test bucket; GUI list | Locally verified for this narrow scope; see [evidence](../evidence/phase-0-results.md) |
| LocalStack                     | Progressive compatibility target |   Not recorded | None                                           | Unverified                                                                             |
| Moto                           | Progressive compatibility target |   Not recorded | None                                           | Unverified                                                                             |
| Generic S3-compatible endpoint | Fallback classification          |            N/A | None                                           | Unverified                                                                             |

## Capability model / Modelo de capacidades

Optional capabilities are `unknown`, `supported`, or `unsupported`. An HTTP 200 alone does not establish support. Automatic detection is best-effort: retain the evidence used, prefer explicit emulator-specific probes, and fall back to `generic` if no reliable probe succeeds.

## Planned common suite / Suíte comum planejada

Once implemented, test only shared semantics: endpoint connectivity, bucket list/create, object listing with prefix/pagination, upload, and streaming download. Emulator-specific cases may be conditional and must remain visible in the matrix.
