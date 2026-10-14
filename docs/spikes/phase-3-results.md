# Phase 3 evidence / Evidências da Fase 3

Local validation completed on Windows/amd64 with Java 25, Quarkus 3.40.1 and MiniStack 1.5.25: 42 tests, zero failures/errors/skips. PowerShell analysis, Markdown/CSS formatting/lint, packaged GUI smoke and `verify-writes.ps1` passed. The deployed flow created a bucket/marker, uploaded a Unicode key, navigated and downloaded with identical SHA-256, and rejected missing CSRF. Fixtures were removed. CI status is recorded separately; this does not certify full S3 compatibility or security.

Validação local concluída: 42 testes sem falhas/erros/ignorados, estilo e smoke aprovados. A imagem empacotada completou criação/upload/navegação/download com SHA-256 idêntico e recusou falta de CSRF; fixtures foram removidas. CI é registrado separadamente. Isso não certifica compatibilidade completa nem segurança.

## Test design / Escopo dos testes

Create through the GUI, upload an exact-limit file with a Unicode/special/XSS-style key, navigate and compare downloaded bytes. Test duplicate bucket/object/marker conflicts, missing confirmation, per-field errors, invalid bucket escaping, missing/mismatched CSRF on form and multipart, file over-limit, request over-limit, missing bucket and temporary-file cleanup. Application tests check UTF-8 byte limits and reserved names; CLI tests check POSIX quoting and absence of credential flags.

Criar pela GUI, enviar arquivo no limite com chave Unicode/especial/XSS, navegar e comparar download; verificar conflitos, confirmação, erros por campo, escaping, CSRF, limites, falhas e limpeza. Testes de aplicação e CLI cobrem validação e quoting. Não são benchmark nem teste de caos.

## Published CI / CI publicado

Commit `dda00dd` passed [GitHub run 38562764657](https://github.com/ERF88/StackEmu/actions/runs/38562764657) on Linux/amd64: style checks, Java integration, packaged Compose smoke, deployed write flow and 128 MiB streaming with a 64 MiB Java heap. Heap is not total process memory. The first Linux run exposed root-owned workspace fixture directories; test uploads now use the system temporary directory instead. No release tag was created.

O commit passou no CI Linux/amd64 vinculado, incluindo o fluxo de escrita e a regressão de streaming. A primeira execução encontrou conflito de permissões nas fixtures; uploads de teste passaram para o temporário do sistema. Heap não representa memória total do processo. Nenhuma tag de release foi criada.

## Limitations / Limitações

No LocalStack/Moto/arm64 certification, public exposure, auth, crash cleanup, resumability, version semantics or concurrent-creation stress evidence. Legal name validation remains pending. See [guide](../development/phase-3.md).

Sem certificação de outros emuladores/arm64, exposição pública, autenticação, recuperação de crashes, retomada, versões ou concorrência. Validação jurídica do nome continua pendente.
