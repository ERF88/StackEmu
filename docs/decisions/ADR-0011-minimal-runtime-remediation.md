# ADR-0011 — minimal runtime vulnerability remediation

Status: Accepted for the verified JVM/amd64 slice / Aceita para JVM/amd64 verificado

## Context / Contexto

The Phase 5 runtime scan reported 49 package/CVE findings (45 MEDIUM, 4 LOW), all in Ubuntu 26.04 OS packages; Java JAR findings were zero. Only two findings had a fixed version in that scan. Removing libc or required libraries from an Ubuntu runtime is not an acceptable fix, and hiding package metadata/ignoring unfixed findings would conceal rather than resolve the problem.

O scan registrou 49 pares pacote/CVE, todos no Ubuntu, e zero em JARs. Somente dois indicavam versão corrigida. Não remover libc necessária nem ocultar metadados/achados sem correção.

## Decision / Decisão

Evaluate the official Temurin 25.0.4.1 JRE on Alpine 3.24, pinned by verified manifest digest. Keep the Maven build base, Java 25, Quarkus version, application boundaries, HTTP contracts and hardened Compose controls unchanged. Replace the Ubuntu runtime packages with Alpine's supported musl runtime, not a glibc compatibility shim or a custom copied JRE. Preserve OS package metadata and scan the complete final filesystem/JARs with the existing scanner, without severity/status suppression.

Avaliar Temurin oficial Alpine/musl, fixado por digest. Manter build Maven, Java/Quarkus, limites, rotas e controles Compose. Substituir componentes Ubuntu pela distribuição musl suportada, sem shim glibc ou JRE copiada manualmente. Preservar metadados e escanear toda a imagem sem supressão.

## Acceptance and consequences / Aceitação e consequências

The first Alpine candidate retained two MEDIUM findings: libpng CVE-2026-46675 and zlib CVE-2026-85091. Install explicitly pinned corrected Alpine package versions (libpng 1.6.59-r0, zlib 1.3.2-r1), retaining the libraries instead of removing needed functionality. CI uses `-RequireClean` to block every reported finding, including LOW/UNKNOWN; `-Enforce` remains the backward-compatible HIGH/CRITICAL policy. / Corrigir PNG/zlib com versões fixadas, preservando bibliotecas; CI passa a bloquear qualquer achado, sem supressões.

Require Java 25 verification, packaged GUI/container/S3 tests, Chromium/axe and the 128 MiB/64 MiB streaming test before adoption. Verify Unicode filename/MIME handling and Docker service-name DNS through the real write/read flow. musl changes native-library compatibility; only the tested JVM/amd64 slice is supported by this evidence. ARM64, arbitrary JNI additions, external HTTPS trust-store customization and load capacity remain unverified. Future native libraries require renewed compatibility review.

Exigir suíte Java, GUI/S3/container, Chromium/axe e streaming, incluindo Unicode/MIME e DNS real. musl altera compatibilidade nativa; evidência cobre apenas JVM/amd64 testado. ARM64, JNI futura, customização HTTPS/trust-store e carga seguem não verificados. Zero achados no scan não certifica ausência de vulnerabilidades.

Reference: [official Temurin container variants](https://adoptium.net/installation/containers).

Adoption evidence: [executed remediation results](../evidence/runtime-remediation-results.md), including final-image browser/streaming checks and a zero-finding scan retaining OS/Java inventories. / Adoção sustentada pelas evidências vinculadas, sem certificação de segurança completa.
