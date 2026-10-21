# Original file metadata — evidence / Evidências de arquivos originais

Local Windows/amd64 verification with Java 25 and MiniStack 1.5.25 passed 49 tests with zero failures/errors/skips. Coverage includes automatic prefix/basename keys, ignored forged manual keys, Unicode encoded-word names, unsafe-name rejection, UTF-8 combined limits, MIME preservation/fallback, long filename extensions, attachment/nosniff and unchanged downloaded bytes. Cases named PDF, PNG, TXT and an unknown extension verify metadata and byte preservation; they do not validate those formats. The packaged GUI write smoke also passed with a Unicode original filename and identical SHA-256. CI evidence is recorded separately.

Verificação local aprovada: 49 testes sem falhas/erros/ignorados. Cobertura de chave automática, nome Unicode, nomes inválidos, limites, MIME/fallback, extensão longa, anexo/nosniff e bytes preservados. Casos nomeados PDF, PNG, TXT e extensão desconhecida verificam metadados, não a validade desses formatos. O fluxo da imagem passou com nome Unicode e SHA-256 idêntico. CI é registrado separadamente.

Existing objects are not migrated. MIME is metadata, not detection of actual content or malware. Other clients/encodings, operating-system filename restrictions, LocalStack and Moto are not comprehensively verified. See [ADR-0008](../decisions/ADR-0008-original-file-metadata.md).

Objetos existentes não são migrados. MIME não valida conteúdo nem malware. Outros clientes/codificações, restrições de nomes locais e emuladores permanecem sem validação completa.

## Published CI / CI publicado

Commit `fef5568` passed [GitHub CI run 38569223238](https://github.com/ERF88/StackEmu/actions/runs/38569223238): style checks, Java 25 verification with MiniStack integration, packaged GUI smoke, Unicode upload/download flow and 128 MiB streaming download with a 64 MiB heap. This evidence does not establish content-format validity or comprehensive security.

O commit `fef5568` passou no CI acima: verificações de estilo, Java 25 com integração MiniStack, smoke da GUI empacotada, upload/download Unicode e download de 128 MiB com heap de 64 MiB. Essas evidências não comprovam validade dos formatos nem segurança abrangente.
