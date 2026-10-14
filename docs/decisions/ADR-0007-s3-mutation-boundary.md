# ADR-0007 — S3 mutation boundary and upload safety

- Status: Accepted / Aceita
- Date: 2026-10-14

## Decision / Decisão

Separate writes from read ports. Keep domain validation pure, SDK mutation/failure mapping in the AWS adapter and form/CSRF/temporary-file handling in web/framework. Use normal forms, explicit confirmation and safely quoted illustrative CLI text, not a JavaScript/shell execution feature. Limit file and request size separately and clean server-generated temporaries. Store uploads as octet-stream and request conditional creation rather than overwrite.

Separar escritas das leituras, validar sem framework, traduzir SDK no adapter e tratar HTTP/CSRF/temporários na borda web. Usar formulários, confirmação e CLI ilustrativa, sem execução de shell. Limitar arquivo e requisição, limpar temporários e solicitar criação condicional, sem sobrescrita.

## Consequences / Consequências

Single-file, non-resumable uploads only; no metadata editor, delete UI or version selection. Conservative bucket names exclude dotted/legacy write targets. Temporary files use disk, not unbounded object buffering; HTTP limits do not provide a total disk quota. Unsigned double-submit CSRF remains a local-only baseline, not authentication/public-hosting approval. Crash recovery, concurrent create semantics and other emulators remain unverified.

Uploads individuais sem retomada, edição, exclusão ou versões. Nomes de escrita conservadores; temporários usam disco sem garantia de quota total. CSRF padrão não autentica nem permite exposição pública. Crashes, concorrência e outros emuladores permanecem não verificados.
