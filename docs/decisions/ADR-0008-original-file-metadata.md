# ADR-0008 — Original file names and content types

Status: Accepted / Aceita

## Decision / Decisão

Upload keys are derived on the server from the current prefix plus the selected file's basename. Preserve extension, case and Unicode; strip client path components (including Windows fake paths), reject empty/dot/control names and validate the combined UTF-8 key length. Ignore legacy/manual `key` fields on upload. Never use this logical name as the temporary filesystem path.

Preserve a syntactically valid reported MIME type; use the filename extension when metadata is missing/generic, then octet-stream for unknown types. This is metadata handling, not byte-format validation or malware detection. Download uses the stored safe MIME, the basename in UTF-8 `filename*` and an ASCII filename fallback retaining the extension. All downloads remain attachments with `nosniff` and bounded streaming.

A chave é calculada pelo servidor: prefixo atual mais basename original, preservando extensão, case e Unicode. Componentes de caminho enviados pelo cliente são removidos; nomes inválidos e chaves acima do limite são recusados. O nome lógico nunca define o temporário.

Preservar MIME informado válido; recorrer à extensão e depois octet-stream quando desconhecido. Não é validação de formato nem antivírus. Baixar com MIME armazenado seguro e nome/extensão preservados, sempre como anexo com `nosniff`.

## Consequences / Consequências

Supersedes only the forced-octet-stream metadata policy of ADR-0005/0007, not streaming, CSRF, limits, cleanup or no-overwrite behavior. Existing objects are not migrated; their stored metadata remains authoritative. Browser/OS filename sanitization can still occur for names that are not valid locally.

Substitui apenas a política de MIME forçado, preservando demais controles. Objetos existentes não são migrados; metadados armazenados continuam sendo a referência. Navegadores/sistemas podem sanitizar nomes incompatíveis com o filesystem local.

Reference: [Content-Disposition, RFC 6266](https://www.rfc-editor.org/rfc/rfc6266).
