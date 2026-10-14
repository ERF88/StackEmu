# ADR-0005 — Paginated reads and bounded streaming

Status: Accepted for Phase 2 / Aceita para a Fase 2

## Decision / Decisão

Keep SDK types in the AWS adapter and pure Java contracts in application. Fetch one `ListObjectsV2` page with delimiter `/` and opaque tokens. Show optional missing metadata as unknown; translate required failures to safe categories. Stream with a fixed 64 KiB buffer, close/abort upstream on completion/cancellation, and serve attachment/octet-stream.

Manter SDK no adapter AWS e contratos Java puros na aplicação. Buscar uma página com delimitador `/` e token opaco. Dados opcionais ausentes são desconhecidos; falhas obrigatórias recebem categorias seguras. Usar buffer de 64 KiB, encerrar upstream e servir anexo/octet-stream.

## Consequences / Consequências

No full materialization, global counts or guessed previous cursors. Optional failures do not hide readable buckets. After headers, terminate failed downloads without HTML. Range/resume/versions remain deferred. Heap proof is functional evidence, not performance certification.

Sem materialização completa, contagem global ou cursor inventado. Falhas opcionais não ocultam buckets. Após cabeçalhos, interromper falhas sem HTML. Retomada/versões adiadas; a prova de heap não certifica desempenho.

Reference: [AWS pagination](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/pagination.html), [evidence](../spikes/phase-2-results.md).
