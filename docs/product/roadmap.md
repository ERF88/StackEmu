# Product roadmap / Roadmap do produto

## Product direction / Direção

StackEmu is a proposed lightweight, server-rendered GUI for AWS-compatible local emulators, beginning with S3. Its differentiation is a hypothesis: simple S3 tasks, JVM-friendly modularity, honest compatibility documentation, CLI equivalents, and later guided labs. “Built in Java” is not itself validated user value.

StackEmu é uma GUI proposta, leve e renderizada no servidor para emuladores locais compatíveis com APIs AWS, começando por S3. Seu diferencial ainda é hipótese: tarefas S3 simples, modularidade JVM, documentação honesta de compatibilidade, comandos CLI e futuramente laboratórios guiados.

## MVP / MVP

The MVP is a usable S3 vertical slice: connection dashboard, bucket list/create, paginated prefix navigation, upload, backend-streamed download, basic metadata, understandable unavailable-emulator states, CLI equivalents, keyboard/responsive/light-dark support, and en/pt-BR. It is not a generic plugin platform or a full AWS console.

O MVP é um vertical slice S3 utilizável com dashboard de conexão, listar/criar bucket, navegação paginada por prefixo, upload, download por streaming no backend, metadados básicos, estados claros para emulador indisponível, comandos CLI, suporte a teclado/responsividade/tema e en/pt-BR. Não é uma plataforma genérica de plugins nem um console AWS completo.

## Phases and gates / Fases e gates

| Phase | Focus                                                                                                            | Gate                                                                                     |
| ----- | ---------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| 0     | Discovery: toolchain, S3 endpoint override, Compose hostname, Qute error state, htmx/CSRF/assets, emulator image | A real screen connects to a real emulator with reproducible build and one automated test |
| 1     | Skeleton and first connection screen                                                                             | A person understands connection status and next action                                   |
| 2     | Read-only S3                                                                                                     | Real MiniStack reading flow is automated                                                 |
| 3     | MVP S3 writes                                                                                                    | Create/upload/navigate/download works end to end                                         |
| 4     | Quality and security                                                                                             | E2E, accessibility, limits, review, and measurements are complete                        |
| 5     | Container/release                                                                                                | Non-root image, verified Compose, CI, SBOM/scan/changelog, bilingual docs                |
| 6     | MVP+ S3                                                                                                          | Safe deletion/versioning/preview only with dedicated behavior and tests                  |
| 7     | Compatibility/differentiation                                                                                    | Reproducible matrix, LocalStack/Moto evidence, first verified lab                        |
| 8     | Growth                                                                                                           | Consider SQS/DynamoDB, native image, profiles, documentation site only when justified    |

No phase is evidence of completion merely because its document exists. Gates require implementation and reproducible test evidence.

## Future modules / Módulos futuros

Suggested order: SQS, DynamoDB, SNS, SSM Parameter Store/Secrets Manager, Lambda, and CloudWatch Logs. A module enters executable planning only with prioritized use cases, target-emulator support, security criteria, reproducible tests, documented limitations, and a maintainer able to review contributions.
