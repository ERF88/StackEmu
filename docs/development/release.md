# Release process / Processo de release

## Current state / Estado atual

Reviewed 2026-10-17: development snapshot, not an approved release. [V6](../evidence/visual-v6-results.md) passed automated checks on the current uncommitted local tree/new Linux-amd64 image: clean host Java, full Linux browser with reviewed references, container/S3 controls, constrained streaming and zero reported scan findings (database-age caveat recorded). This is not exact immutable-candidate-commit verification, human accessibility, independent quick start or legal/name approval. [Current status](../status.md) retains those gates. Phase 5's [commands](phase-5.md) and earlier CI remain historical, not a new GitHub CI run. Daily host JVM/only S3 in Docker was restored. No version/tag/registry publication is authorized.

Revisado em 2026-10-17: checks automáticos V6 passaram na árvore local não commitada/imagem Linux-amd64 nova, com referência visual revisada, controles, streaming e scan sem achados reportados/limite da base registrado. Não equivale a verificar commit candidato imutável nem concluir revisão humana, início rápido independente ou nome legal. Status preserva gates; CI anterior continua histórico. Fluxo JVM local/somente S3 no Docker restaurado. Sem versão/tag/publicação autorizada.

## Required gates / Gates obrigatórios

- [ ] Complete legal/name validation (including INPI and intended registry/domain names). / Validar nome legal/registros.
- [ ] Complete the human accessibility checklist in [security review](../security-review.md). / Concluir revisão humana.
- [ ] Have a person without prior project context follow both documented quick-start paths and record issues. / Registrar início rápido por pessoa sem contexto.
- [ ] Run clean Java 25/real MiniStack, browser and container verification for the candidate commit. / Validar candidato em ambiente limpo.
- [ ] Review current SBOM and image scan; run `scan-image.ps1 -RequireClean` and resolve all reported findings before publication. / Revisar SBOM/scan e resolver achados de qualquer severidade.
- [ ] Synchronize bilingual changelog, limitations, tested versions and release notes. / Sincronizar documentos.
- [ ] Explicitly approve version and publication destination. / Aprovar versão e destino.

These boxes are release gates, not automatically completed by existing historical tests. Keep the candidate at `0.1.0-SNAPSHOT` until publication is approved. No automatic tag-triggered publish workflow is enabled while gates remain open.

Os itens são gates, não evidências automaticamente concluídas. Manter `0.1.0-SNAPSHOT` até aprovação. Nenhum workflow automático de publicação por tag foi habilitado.

## Future publication sequence / Sequência de publicação

Once approved: choose `vX.Y.Z` (or `vX.Y.Z-rc.N` for a prerelease), synchronize Maven version/changelog, validate the exact commit, build and scan each supported platform, publish only to `ghcr.io/erf88/stackemu` with narrowly scoped permissions, preserve immutable digests, attach SBOM/scan/checksums and bilingual notes, then pull/run the published digest with local Compose. Stable aliases `X.Y`/`latest` must never point to an RC. ARM64 must be built and functionally tested before it is advertised; amd64 is the current tested baseline. Provenance/signing is a future publication requirement to implement and verify, not an existing attestation.

Após aprovação: definir versão/tag, sincronizar Maven/changelog, validar commit, compilar/escanear cada plataforma, publicar GHCR com permissões mínimas e digests imutáveis, anexar evidências/notas e executar imagem publicada por digest. Aliases estáveis nunca apontam para RC. ARM64 e assinatura/proveniência ainda precisam de implementação/verificação.

## JVM versus native / JVM versus nativa

JVM remains the default. Measure size, memory, startup/build complexity and compatibility before a native-image decision; no native or multi-platform success is claimed by JVM tests.

JVM permanece padrão. Medir antes de decidir nativa; testes JVM não comprovam sucesso nativo/multiplataforma.
