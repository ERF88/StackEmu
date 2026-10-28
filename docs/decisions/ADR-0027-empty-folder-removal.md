# ADR-0027: Empty folder removal / Exclusão de pasta vazia

Date / Data: 2026-10-22. Status: accepted scoped correction / correção delimitada aceita.

## Context / Contexto

The maintainer reported that creating a folder leaves an undeletable marker and prevents empty-bucket deletion. S3 folders are prefixes; creation writes a zero-byte object ending in `/`. This decision supersedes ADR-0021's marker exclusion only for this exact empty-folder entry. It does not authorize recursive deletion or Empty and delete.

O mantenedor encontrou uma pasta criada sem ação de exclusão, impedindo excluir o bucket vazio. Pastas S3 são prefixos; criar grava um objeto de zero bytes terminado em `/`. Esta decisão substitui a exclusão de markers da ADR-0021 apenas para essa entrada vazia exata, sem autorizar exclusão recursiva ou Esvaziar e excluir.

## Contract / Contrato

- Label creation as Create folder / Criar pasta. Expose a labelled review action beside folder rows and zero-byte marker objects. No per-row metadata queries. / Criação com nome simples e revisão ao lado das pastas/objetos markers vazios, sem consulta de metadados por linha.
- Reuse the exact-object route, server-held target, five-minute one-use ticket, CSRF, explicit confirmation, never-versioned bucket check and conditional ETag deletion. / Reutilizar rota, alvo no servidor, ticket único de cinco minutos, CSRF, confirmação explícita, bucket nunca versionado e ETag condicional.
- During review and again before submission, list the exact prefix without delimiter, at most two entries. Require an explicit complete response containing only the exact zero-byte marker, consistent count, no common prefixes or continuation. Missing, nonempty, truncated or unknown states fail closed. HEAD must also prove zero bytes and no object version. Recheck the reviewed ETag before submitting DELETE. / Na revisão e antes do envio, listar o prefixo exato sem delimiter, no máximo duas entradas. Exigir resposta completa contendo só o marker exato de zero bytes, contagem coerente, sem prefixos agrupados/continuação. Ausente, ocupado, truncado ou desconhecido bloqueia. HEAD comprova zero bytes/ausência de versão; rever ETag antes do DELETE.
- Submit one exact-key DELETE, never enumerate/delete children, never retry automatically. Preserve uncertainty after an ambiguous response. Return to the parent prefix. Bucket deletion keeps its independent empty/version gates. / Um DELETE exato, sem percorrer/excluir filhos ou retry automático; resposta ambígua mantém incerteza. Retornar ao pai; exclusão do bucket mantém gates próprios.
- Prefix proof and DELETE are not atomic against external writers. A concurrent child may survive a marker deletion; no child can be deleted by this action. An external versioning change retains the existing uncertainty risk. / Prova e DELETE não são atômicos diante de escritores externos; um filho concorrente pode permanecer, mas esta ação nunca o exclui. Mudança externa de versionamento mantém o risco de incerteza existente.
- Center action headings over a fixed icon rail, reserve the trailing delete slot for folder rows and retain accessible target sizes and names. / Centralizar cabeçalhos sobre o grupo fixo de ícones e reservar a última posição para excluir pasta, preservando alvos/rótulos acessíveis.

## References and verification / Referências e verificação

The isolated folder-list response is capped at 64 KiB before SDK conversion; a hardened XML parser rejects DTD/external resources and requires one explicit literal IsTruncated boolean. This is necessary because the installed SDK coerces empty boolean text to false. / Resposta isolada limitada a 64 KiB antes da conversão; parser XML endurecido recusa DTD/recursos externos e exige booleano IsTruncated literal único, pois o SDK converte texto vazio em false.

Primary references consulted: [AWS object keys](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html) and [ListObjectsV2](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html). They describe S3 semantics, not MiniStack compatibility evidence. Record actual local Java/protocol/browser results in [workspace evidence](../evidence/bucket-workspace-results.md). Keep Docker MiniStack-only. Human accessibility/visual acceptance, final new-image checks and StackEmu legal/name validation remain pending.

Referências primárias descrevem semântica S3, não comprovam compatibilidade MiniStack. Registrar execuções locais reais no registro vinculado. Docker somente MiniStack; aceite humano, imagem final e validação jurídica/do nome continuam pendentes.
