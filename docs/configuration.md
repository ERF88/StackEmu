# Configuration / Configuração

## Source of truth / Fonte de verdade

Phase 3 implements `STACKEMU_MAX_UPLOAD` (positive bytes, default 10485760) and `STACKEMU_HTTP_MAX_BODY` (Quarkus size syntax, default 11M). Both limits apply independently; allow multipart overhead. Preview remains planned. See [Phase 3](development/phase-3.md).

A Fase 3 implementa limites de arquivo e corpo HTTP independentes; ajuste ambos com margem para multipart. Preview continua planejado.

Phase 1 implements the effective connection view. Phase 2 adds `STACKEMU_PAGE_SIZE` mapped to `stackemu.page-size`: default 50, clamped to 1–1000. Preview remains planned. See [Phase 1](development/phase-1.md) and [Phase 2](development/phase-2.md).

A Fase 1 mostra configuração efetiva; a Fase 2 implementa paginação, padrão 50 e limite defensivo 1–1000. Preview continua planejado. A tabela distingue configurações atuais de planejamento futuro.

For the MVP, environment variables are the only persistent connection configuration. The UI should display the effective endpoint and region, mask secrets, and test a safe read operation. It must not imply that edits persist across restart. A temporary session-only override is deferred unless a real need is demonstrated.

No MVP, variáveis de ambiente são a única configuração persistente. A UI deve mostrar endpoint/região efetivos, mascarar segredos e testar uma operação segura de leitura. Não deve sugerir que edições persistem após reinício.

## Variables and status / Variáveis e status

| Variable                     | Proposed default        | Rule/status                                                      |
| ---------------------------- | ----------------------- | ---------------------------------------------------------------- |
| `STACKEMU_ENDPOINT`          | `http://localhost:4566` | Address visible to GUI process; unverified format/config mapping |
| `STACKEMU_REGION`            | `us-east-1`             | Region sent by client                                            |
| `STACKEMU_ACCESS_KEY`        | `test`                  | Development value only; not security                             |
| `STACKEMU_SECRET_KEY`        | `test`                  | Never log or display                                             |
| `STACKEMU_EMULATOR`          | `auto`                  | `ministack`, `localstack`, `moto`, or `generic`                  |
| `STACKEMU_MAX_UPLOAD`        | `10485760`              | Implemented file-byte limit; positive integer                    |
| `STACKEMU_HTTP_MAX_BODY`     | `11M`                   | Implemented whole-request limit; include multipart overhead      |
| `STACKEMU_PREVIEW_MAX_BYTES` | `1048576`               | 1 MiB preview cap                                                |
| `STACKEMU_PAGE_SIZE`         | `50`                    | Configurable with defensive maximum                              |

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
