# Runtime remediation — evidence / Evidências de correção

Date: 2026-10-14. Baseline: [Phase 5 run](https://github.com/ERF88/StackEmu/actions/runs/38587395076). / Data e baseline acima.

## Findings and action / Achados e ação

The baseline had 49 package/CVE findings (45 MEDIUM, 4 LOW), 39 distinct CVEs across 18 Ubuntu packages, with no reported Java JAR findings. The [complete inventory](../security/runtime-remediation.md) preserves each original identifier. The runtime was replaced with the official Temurin 25.0.4.1 Alpine/musl image, pinned by digest; required PNG/zlib libraries were retained and updated to libpng 1.6.59-r0 and zlib 1.3.2-r1. Build/JVM/Quarkus/API decisions and MiniStack volumes were preserved. No package metadata, scanner database, severity or affected/unfixed status was hidden.

Baseline: 49 pares pacote/CVE, 39 CVEs, 18 pacotes Ubuntu. Inventário completo preservado. Runtime substituído por Temurin Alpine/musl oficial fixado; PNG/zlib mantidas e corrigidas, sem mudar Java/Quarkus/API ou volumes MiniStack. Sem ocultar metadados, base do scanner, severidades ou achados sem correção.

## Local executed checks / Verificações locais executadas

Windows/amd64, Docker Desktop Linux containers, MiniStack 1.5.25, Java 25: Maven clean verification passed 51 tests, zero failures/errors/skips; packaged container/smoke/write tests passed; final-image Chromium/axe passed six tests in 39.6 seconds. The write flow exercises Docker service-name DNS, original Unicode filename, content type, SHA-256 byte preservation and CSRF rejection. This is not full native/TLS/performance coverage.

Localmente: 51 testes Java sem falhas/erros/ignorados, checks de container/smoke/escritas e seis testes Chromium/axe aprovados. Fluxo verifica DNS, Unicode, MIME, SHA-256 e CSRF, sem certificar JNI/TLS/desempenho integral.

Trivy 0.76.0 `-RequireClean` passed on the final image at 2026-10-14T15:45:51Z: zero findings of any severity, with 44 OS and 152 Java packages detected. The first Alpine candidate had two MEDIUM findings (PNG/zlib); those were corrected, not suppressed. The scan's vulnerability database was updated on 2026-10-14. A zero report is date/database/coverage-specific, not proof of a vulnerability-free product. The Maven build image and MiniStack are not targets of this final-runtime scan.

Scan final passou com zero achados e inventários de 44 pacotes OS/152 Java. Os dois MEDIUM da candidata foram corrigidos. Resultado depende da data/base/cobertura; não prova produto sem vulnerabilidades. Imagem Maven de build e MiniStack não são alvos deste scan do runtime final.

## Pending gates / Gates pendentes

The final corrected image also passed a 128 MiB download with `-Xmx64m`, identical SHA-256, in 4.03 seconds locally. This functional timing is not a performance benchmark. / Streaming final de 128 MiB com heap de 64 MiB passou com SHA-256 idêntico; tempo funcional não é benchmark.

Legal-name validation, human accessibility, clean-user quick-start review, ARM64 and public image publication remain pending. No release/tag is created by this remediation. / Nome legal, revisão humana, novo usuário, ARM64 e publicação seguem pendentes; sem release/tag criada.
