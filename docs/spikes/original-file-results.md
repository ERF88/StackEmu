# Original file metadata — evidence / Evidências de arquivos originais

Local Windows/amd64 verification with Java 25 and MiniStack 1.5.25 passed 49 tests with zero failures/errors/skips. Coverage includes automatic prefix/basename keys, ignored forged manual keys, Unicode encoded-word names, unsafe-name rejection, UTF-8 combined limits, MIME preservation/fallback, long filename extensions, attachment/nosniff and unchanged downloaded bytes. Cases named PDF, PNG, TXT and an unknown extension verify metadata and byte preservation; they do not validate those formats. The packaged GUI write smoke also passed with a Unicode original filename and identical SHA-256. CI evidence is recorded separately.

Verificação local aprovada: 49 testes sem falhas/erros/ignorados. Cobertura de chave automática, nome Unicode, nomes inválidos, limites, MIME/fallback, extensão longa, anexo/nosniff e bytes preservados. Casos nomeados PDF, PNG, TXT e extensão desconhecida verificam metadados, não a validade desses formatos. O fluxo da imagem passou com nome Unicode e SHA-256 idêntico. CI é registrado separadamente.

Existing objects are not migrated. MIME is metadata, not detection of actual content or malware. Other clients/encodings, operating-system filename restrictions, LocalStack and Moto are not comprehensively verified. See [ADR-0008](../decisions/ADR-0008-original-file-metadata.md).

Objetos existentes não são migrados. MIME não valida conteúdo nem malware. Outros clientes/codificações, restrições de nomes locais e emuladores permanecem sem validação completa.
