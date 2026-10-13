# Emulator compatibility / Compatibilidade de emuladores

## Compatibility policy / Política

Connecting is not compatibility. A compatibility statement requires a pinned emulator version, documented operations, reproducible tests, known limitations, skipped tests and reasons, and any account/token/license requirement.

Conectar não é compatibilidade. Uma afirmação exige versão fixa do emulador, operações documentadas, testes reproduzíveis, limitações conhecidas, testes ignorados e motivo, além de requisitos de conta/token/licença.

## Current matrix / Matriz atual

| Emulator | Role in plan | Tested version | Verified operations | Status |
|---|---|---:|---|---|
| MiniStack | Proposed primary reference | Not recorded | None | Unverified |
| LocalStack | Progressive compatibility target | Not recorded | None | Unverified |
| Moto | Progressive compatibility target | Not recorded | None | Unverified |
| Generic S3-compatible endpoint | Fallback classification | N/A | None | Unverified |

## Capability model / Modelo de capacidades

Optional capabilities are `unknown`, `supported`, or `unsupported`. An HTTP 200 alone does not establish support. Automatic detection is best-effort: retain the evidence used, prefer explicit emulator-specific probes, and fall back to `generic` if no reliable probe succeeds.

## Planned common suite / Suíte comum planejada

Once implemented, test only shared semantics: endpoint connectivity, bucket list/create, object listing with prefix/pagination, upload, and streaming download. Emulator-specific cases may be conditional and must remain visible in the matrix.
