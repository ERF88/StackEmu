# Known limitations / Limitações conhecidas

## Baseline status / Status do baseline

Preview supports complete UTF-8 text/JSON-as-text up to 64 KiB and separate static PNG/JPEG re-encoding within [ADR-0019 limits](decisions/ADR-0019-raster-preview-isolation.md). No JSON syntax validation, HTML/SVG/PDF/GIF/WebP/APNG, compressed encoding or arbitrary active documents. Images are refused rather than downscaled/truncated; no exact EXIF orientation/color-fidelity promise or version-consistent page/download snapshot. Shell 200 does not establish object/image availability; no-JS retains generic refusal guidance, not the failed image response's exact reason. Original attachments remain unchanged. [Local raster evidence](spikes/ux-5b-results.md) does not replace final new-image/runtime validation. / Preview de texto até 64 KiB e PNG/JPEG estáticos reencodados dentro dos limites; sem validar sintaxe JSON/renderizar formatos ativos/animação/encoding comprimido. Sem reduzir/truncar imagens, prometer EXIF/fidelidade ou snapshot consistente. Shell 200 não confirma imagem; sem JS há orientação genérica, não motivo HTTP exato. Anexos preservados e validação final com imagem nova pendente.

Phase 3 implements the read/write S3 slice against MiniStack 1.5.25. See [evidence](spikes/phase-3-results.md). Deletion, overwrite, range/resume and version selection are not implemented in the GUI; full compatibility remains unverified.

A Fase 3 implementa leitura e escritas limitadas contra MiniStack 1.5.25. Exclusão, sobrescrita, retomada e versões não existem na GUI; compatibilidade completa permanece não verificada.

## Historical MVP exclusions and current limits / Exclusões históricas e limites atuais

The user-confirmed [expanded operational delivery](product/s3-management-scope.md), recorded in ADR-0014, supersedes earlier operational rename/destructive-cleanup scheduling exclusions below. These features still require implementation and fresh evidence; advanced configuration and other exclusions remain. / O escopo operacional ampliado substitui o agendamento antigo de rename/limpeza destrutiva, sem declarar implementação. Configuração avançada e demais exclusões permanecem.

Phase 5's reference Compose adds a 64 MiB aggregate tmpfs quota and container limits. Standalone host execution has no equivalent aggregate quota by default. Quota exhaustion/concurrency capacity, ARM64, registry publication and provenance/signing remain unverified. / Compose limita tmpfs e container; execução no host não tem quota equivalente. Concorrência, ARM64, publicação e assinatura seguem pendentes.

Phase 4 adds a Chromium/axe automated baseline and REST browser policy, not full WCAG/security certification. Human assistive-technology/zoom review and cross-browser checks remain pending. See [evidence](spikes/phase-4-results.md) and [review](security-review.md). / A Fase 4 adiciona baseline automático, não certificação; revisão humana e outros browsers seguem pendentes.

Phase 3 adds bucket creation, bounded single-file upload and markers; see [guide](development/phase-3.md). Write targets use a conservative name policy. Authentication, signed-CSRF policy, crash recovery, concurrency stress, versions and resumability remain unverified/deferred. / A Fase 3 adiciona escritas limitadas; autenticação, assinatura CSRF, crashes, concorrência, versões e retomada continuam pendentes.

- AWS account administration, real-AWS console replacement, multitenancy, and internet exposure.
- Other service modules, persistent editable connection profiles, arbitrary terminal/shell, and native image as a release requirement.
- Complex S3 policy/CORS/encryption/lifecycle/replication editing, object metadata/content editing, object rename, and full version recovery.
- Automatic empty-and-delete bucket behavior; destructive/version-aware flows are post-MVP and need dedicated tests.

## Compatibility limits / Limites de compatibilidade

MiniStack is the proposed primary reference, while LocalStack and Moto are later targets. No emulator is currently certified compatible. Different S3 semantics, capability detection, health endpoints, persistence paths, and licensing/token requirements must be recorded by tested version.

## Legal and identity / Legal e identidade

The StackEmu name remains legally unvalidated. The project repository is `ERF88/StackEmu`. Validate package registries, Docker Hub, domain, and INPI before public release. Do not imply endorsement or affiliation with third-party marks.
