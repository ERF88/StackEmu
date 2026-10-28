# Configuration / Configuração

Durable operation storage, retention and transfer settings / Persistência, retenção e limites de transferência: [S3 operations](development/s3-operations.md).

## Source of truth / Fonte de verdade

Reviewed / Revisado: 2026-10-16. Effective mappings are defined by `src/main/resources/application.properties`; [current status](status.md) supersedes historical phase labels below. Host execution defaults to `http://localhost:4566`; the separate reference GUI container uses `http://ministack:4566`. Daily workflow starts only the emulator in Docker. / Mapeamentos efetivos vêm das properties; status atual substitui rótulos históricos. Endpoints do host/contêiner diferem; fluxo diário usa somente emulador no Docker.

Phase 5's reference GUI container has a read-only root, a 64 MiB `/tmp` tmpfs, 512 MiB memory and 256 PIDs. These Compose controls are not `STACKEMU_` variables and do not automatically apply to standalone host execution. See [container guide](development/phase-5.md). / Limites do Compose não são variáveis `STACKEMU_` nem se aplicam automaticamente ao host.

`STACKEMU_MAX_UPLOAD` (positive bytes, default 10485760) and `STACKEMU_HTTP_MAX_BODY` (Quarkus size syntax, default 11M) apply independently; allow multipart overhead. Text preview is fixed at 64 KiB and raster preview has fixed [ADR-0019 limits](decisions/ADR-0019-raster-preview-isolation.md), not an implemented preview environment switch. See [upload guide](development/phase-3.md).

Limites de arquivo e corpo independentes, com margem para multipart. Preview implementado tem limites fixos nas políticas, não variável de ambiente de preview implementada.

The effective connection view and `STACKEMU_PAGE_SIZE` mapping to `stackemu.page-size` are implemented: default 50, clamped to 1–1000. See [connection guide](development/phase-1.md) and [read guide](development/phase-2.md).

A conexão efetiva e paginação estão implementadas, com padrão 50 e limite defensivo 1–1000. A tabela distingue variáveis reais de propostas não implementadas.

For the MVP, environment variables are the only persistent connection configuration. The UI should display the effective endpoint and region, mask secrets, and test a safe read operation. It must not imply that edits persist across restart. A temporary session-only override is deferred unless a real need is demonstrated.

No MVP, variáveis de ambiente são a única configuração persistente. A UI deve mostrar endpoint/região efetivos, mascarar segredos e testar uma operação segura de leitura. Não deve sugerir que edições persistem após reinício.

## Variables and status / Variáveis e status

| Variable                     | Proposed default        | Rule/status                                                                     |
| ---------------------------- | ----------------------- | ------------------------------------------------------------------------------- |
| `STACKEMU_ENDPOINT`          | `http://localhost:4566` | Implemented endpoint override; host defaults to loopback                        |
| `STACKEMU_REGION`            | `us-east-1`             | Region sent by client                                                           |
| `STACKEMU_ACCESS_KEY`        | `test`                  | Development value only; not security                                            |
| `STACKEMU_SECRET_KEY`        | `test`                  | Never log or display                                                            |
| `STACKEMU_EMULATOR`          | Proposed `auto`         | Not implemented; no environment mapping / Não implementada                      |
| `STACKEMU_MAX_UPLOAD`        | `10485760`              | Implemented file-byte limit; positive integer                                   |
| `STACKEMU_HTTP_MAX_BODY`     | `11M`                   | Implemented whole-request limit; include multipart overhead                     |
| `STACKEMU_PREVIEW_MAX_BYTES` | Former proposal only    | Not implemented; fixed text/raster policies / Não implementada, políticas fixas |
| `STACKEMU_PAGE_SIZE`         | `50`                    | Configurable with defensive maximum                                             |

Defaults and exact Quarkus property mappings must be verified alongside code; this table is planning guidance, not a runtime contract.

## Topologies / Topologias

| GUI/emulator placement          | Endpoint seen by GUI                                                    |
| ------------------------------- | ----------------------------------------------------------------------- |
| Same Compose network            | `http://ministack:4566`                                                 |
| GUI container, emulator on host | `http://host.docker.internal:4566` (Linux needs explicit configuration) |
| Both on host                    | `http://localhost:4566`                                                 |

The browser should not need the container-internal hostname. The backend mediates object operations when that avoids invalid internal URLs or endpoint exposure.

## Connection test / Teste de conexão

“Test connection” must validate connectivity plus a safe read action, show an understandable result, and never reveal a secret, stack trace, or unnecessary internal response.
