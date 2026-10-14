# Phase 5 — executed evidence / Evidências executadas

## Local verification / Verificação local

On 2026-10-14, Windows/amd64 with Docker Desktop Linux containers, Java 25 and MiniStack 1.5.25: `mvn -B -ntp spotless:apply clean verify` passed 51 tests with zero failures/errors/skips and generated CycloneDX 1.6 JSON/XML with 152 Maven components. The inspected JSON contained no JUnit/ArchUnit/REST Assured components. The plugin reported schema-keyword warnings (`meta:enum`, `deprecated`), not build failure; no broader SBOM completeness/certification is claimed.

Em 2026-10-14, Java 25/MiniStack local aprovou 51 testes sem falhas/erros/ignorados e gerou SBOM JSON/XML com 152 componentes. Inspeção JSON não encontrou JUnit/ArchUnit/REST Assured. O plugin emitiu warnings de keywords de schema, sem falhar o build; não há certificação de completude.

The hardened GUI passed container-policy checks (UID 10001, read-only root, tmpfs/memory/PID limits, capability/privilege restrictions, loopback and license files), deployed smoke and S3 write/download SHA-256/CSRF checks. Six Chromium/axe tests passed in 34.3 seconds under the new reference restrictions. MiniStack's existing persistent volumes were retained.

A GUI reforçada passou nos checks de container, smoke, fluxo S3/SHA-256/CSRF e seis testes Chromium/axe em 34,3 s. Volumes persistentes do MiniStack foram preservados.

## Scan discipline / Disciplina do scan

The first scanner attempt failed because its own temporary download area was only 64 MiB; this was not a clean scan. Scanner scratch now uses its generated cache directory, without loosening GUI tmpfs limits or mounting the Docker socket. Scan results and blocking severities must be recorded only after a successful invocation. Current JSON/summary are generated in `work/release`; CI retains selected reports separately.

Primeiro scan falhou por falta de espaço no scratch do scanner; não é resultado limpo. Scratch foi direcionado ao cache gerado, sem alterar a GUI ou montar socket. Registrar achados somente após execução aprovada. JSON/resumo em `work/release`; CI retém relatórios separados.

## Release status / Status de release

Final commit `b1ca5f8` passed [Linux/amd64 CI run 38587395076](https://github.com/ERF88/StackEmu/actions/runs/38587395076). The downloaded artifact was inspected: CycloneDX JSON/XML, browser JSON, scan JSON and summary are present; the CI scan also records 49 findings (45 MEDIUM, 4 LOW), zero HIGH/CRITICAL. Artifact retention expires on 2026-10-28; regenerate evidence rather than assuming availability after expiry. Container, S3, browser and constrained-stream checks all passed. This is verification evidence, not release approval or proof of no vulnerabilities.

O commit `b1ca5f8` passou no CI Linux/amd64 acima. Artefato baixado e inspecionado contém SBOM JSON/XML, relatório de navegador e scan/resumo: 49 achados (45 MEDIUM, 4 LOW), zero HIGH/CRITICAL. Retenção até 2026-10-28; após expirar, regenerar evidências. Checks de container, S3, navegador e streaming aprovados. Não aprova release nem prova ausência de vulnerabilidades.

The initial completed scan reported 62 findings, including three HIGH Go stdlib findings (`CVE-2026-78667`, `CVE-2026-78669`, `CVE-2026-97031`) in `/usr/bin/pebble`, built with Go 1.26.7. StackEmu's explicit Java entrypoint does not use this inherited supervisor; its exact runtime binary was removed, not ignored. The rebuilt image passed container/smoke/write checks and `scan-image.ps1 -Enforce`: 49 remaining findings, zero HIGH/CRITICAL. No finding is suppressed; remaining severities still need review. The vulnerability DB used was updated at 2026-10-14T13:10:32Z. Scanner results are time-/database-specific, not proof that the image is vulnerability-free. Existing legacy scanner-generated cache/report ownership was migrated locally to the new scanner UID; no user resources were removed.

Scan inicial completo: 62 achados e três HIGH na stdlib Go do supervisor herdado. O binário sem uso foi removido da imagem, sem ignore list. Imagem reconstruída passou em container/smoke/escritas e no modo estrito: 49 achados restantes, zero HIGH/CRITICAL. Achados remanescentes exigem revisão. Base atualizada em 2026-10-14T13:10:32Z; resultado depende da data/base e não prova ausência de vulnerabilidades. A propriedade do cache/relatório legado gerado foi migrada localmente; nenhum recurso do usuário foi removido.

The first Linux CI scan failed on cache permissions after capabilities were dropped. The scanner now uses the host UID/GID on POSIX hosts (UID/GID 10001 on Windows bind mounts), without broad chmod or restored capabilities. That failed invocation is not vulnerability evidence. The hardened local 128 MiB streaming check separately passed with a 64 MiB Java heap and identical SHA-256.

O primeiro scan Linux falhou por permissões no cache após remover capabilities. O scanner agora usa UID/GID do host POSIX (10001 no bind Windows), sem chmod amplo ou restaurar privilégios. Falha não é evidência de vulnerabilidades. Streaming local de 128 MiB com heap de 64 MiB passou com SHA-256 idêntico.

No tag, GitHub Release or GHCR image is published by this phase. Legal-name validation, human accessibility/clean-user review, vulnerability approval, ARM64 and published-image pull/run remain pending. See [release gates](../development/release.md).

Sem tag/release/imagem GHCR publicada. Nome legal, revisão humana/início rápido, aprovação de vulnerabilidades, ARM64 e execução de imagem publicada continuam pendentes.
