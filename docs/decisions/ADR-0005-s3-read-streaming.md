# ADR-0005 — Paginated reads and bounded streaming

Status: Accepted for Phase 2 / Aceita para a Fase 2

File MIME/name policy is superseded by [ADR-0008](ADR-0008-original-file-metadata.md); this is the historical Phase 2 decision. / Política de MIME/nome substituída pela ADR-0008; decisão histórica preservada.

## Decision / Decisão

Keep SDK types in the AWS adapter and pure Java contracts in application. Fetch one `ListObjectsV2` page with delimiter `/` and opaque tokens. Show optional missing metadata as unknown; translate required failures to safe categories. Stream with a fixed 64 KiB buffer, close/abort upstream on completion/cancellation, and serve attachment/octet-stream.

Manter SDK no adapter AWS e contratos Java puros na aplicação. Buscar uma página com delimitador `/` e token opaco. Dados opcionais ausentes são desconhecidos; falhas obrigatórias recebem categorias seguras. Usar buffer de 64 KiB, encerrar upstream e servir anexo/octet-stream.

## Consequences / Consequências

UX-2B opaque-key correction (2026-10-15): explicitly request `encoding-type=url` for each bounded ListObjectsV2 page to protect keys/common prefixes from XML normalization. The SDK handles decoding; never manually decode again, trim keys or decode continuation tokens. Preserve all existing pagination/read/mutation boundaries. Verify CR/LF, Unicode, plus and literal percent values against the pinned local emulator before recording compatibility. / Correção de chaves opacas: solicitar codificação URL na resposta e deixar decodificação para SDK, sem segunda decodificação, trim ou alteração de tokens; preservar paginação/limites e verificar localmente.

Protocol reference / Referência do protocolo: [AWS ListObjectsV2 encoding-type](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html).

No full materialization, global counts or guessed previous cursors. Optional failures do not hide readable buckets. After headers, terminate failed downloads without HTML. Range/resume/versions remain deferred. Heap proof is functional evidence, not performance certification.

Sem materialização completa, contagem global ou cursor inventado. Falhas opcionais não ocultam buckets. Após cabeçalhos, interromper falhas sem HTML. Retomada/versões adiadas; a prova de heap não certifica desempenho.

Reference: [AWS pagination](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/pagination.html), [evidence](../evidence/phase-2-results.md).
