# Naming review / Revisão de nomenclatura

Scope: production Java types and their tests/templates after Phase 2. No public route, SDK timeout, page limit or download policy intentionally changed. This is an engineering review, not a compatibility or security certification.

## Applied mapping / Padronização aplicada

| Before                              | After                                       | Reason / Motivo                                                          |
| ----------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------ |
| `BucketReader.read`                 | `BucketListPort.listBuckets`                | Explicit output port/action / porta e ação explícitas                    |
| `AwsBucketReader`                   | `AwsBucketListAdapter`                      | SDK implementation role / papel do adapter                               |
| `BucketSnapshot`                    | `BucketListSnapshot`                        | Listing/connectivity data / resultado da listagem                        |
| `ReadFailure`                       | `S3ReadException`                           | S3-specific application exception / exceção específica                   |
| `Ui`                                | `UiContext`                                 | Language/theme/labels / contexto de apresentação                         |
| `ConnectionView`                    | `ConnectionSettingsView`                    | Effective redacted settings / configuração efetiva                       |
| `S3Views.bucket/object`             | `S3ViewMapper.toBucketPage/toObjectPage`    | Presentation mapping / mapeamento explícito                              |
| Mapper `BucketPage/ObjectPage`      | `BucketPageView/ObjectPageView`             | Separate formatted UI from port data / separar apresentação de dados     |
| `StreamCopy` in application         | `DownloadStreamCopier` in web               | Delivery concern / responsabilidade de entrega                           |
| Mapper `disposition`                | `DownloadHeaders.attachmentDisposition`     | HTTP policy outside page mapping / política HTTP separada                |
| Buckets resource connection/gallery | `ConnectionResource` / `ComponentsResource` | Independent screens/reasons to change / telas com mudanças independentes |

## Current class responsibilities / Responsabilidades atuais

| Class/type                      | Owns                                                              | Must not own                       |
| ------------------------------- | ----------------------------------------------------------------- | ---------------------------------- |
| `BucketListPort`                | Listing/connectivity contract                                     | SDK or HTTP                        |
| `BucketListSnapshot`            | Immutable bucket names/connectivity                               | Display formatting                 |
| `S3ReadPort` and nested records | Bucket/object reads and stream ownership contract                 | Mutations, templates               |
| `S3ReadException`               | Stable failure category                                           | SDK causes, secrets, locale        |
| `AwsBucketListAdapter`          | SDK bucket listing/failure snapshot                               | Page rendering                     |
| `AwsS3ReadAdapter`              | SDK details/pagination/download/failure translation               | HTTP status or UI                  |
| `BucketsResource`               | Dashboard/list fragment                                           | Connection/gallery routes          |
| `ConnectionResource`            | Effective-settings screen and safe listing probe                  | Config editing/client creation     |
| `ComponentsResource`            | Static component gallery                                          | Emulator I/O                       |
| `S3Resource`                    | Read request validation, port invocation, HTML/download responses | SDK/mutation rules                 |
| `ConnectionSettingsView`        | Redacted endpoint and configured region                           | Credentials or persistence editing |
| `UiContext`                     | Locale/theme/labels/context links                                 | Emulator I/O                       |
| `S3ViewMapper`                  | Formatted page records, navigation and URL encoding               | SDK/attachment policy              |
| `DownloadHeaders`               | Safe attachment filename                                          | Object rendering                   |
| `DownloadStreamCopier`          | Fixed-buffer transfer                                             | Upstream lifecycle ownership       |

Esses limites mantêm contratos puros, isolam SDK/HTTP e tornam dependências explícitas. Mappers não fazem I/O; recursos coordenam entrega; adapters traduzem operações. O caller encerra streams. Os nomes foram atualizados também nos testes e templates associados.

## Follow-up boundaries / Limites para a sequência

Retain one read adapter while operations share SDK translation/lifecycle; split only when a new responsibility warrants it. Introduce real mutation ports/use cases for Phase 3, not methods on read ports. Shared URL construction remains within the web adapter; extract a dedicated link type if routes gain independent consumers/policy. Add a domain model when actual cross-route business invariants appear. No count/size threshold mechanically forces a class split.

Manter adapter de leitura enquanto as operações compartilham responsabilidade. Na Fase 3, criar contratos/use cases de escrita conforme regras reais, não ampliar o port de leitura. Extrair links/modelo de domínio apenas quando houver motivo concreto; evitar limites artificiais por tamanho.
