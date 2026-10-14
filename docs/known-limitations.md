# Known limitations / Limitações conhecidas

## Baseline status / Status do baseline

Phase 3 implements the read/write S3 slice against MiniStack 1.5.25. See [evidence](spikes/phase-3-results.md). Deletion, overwrite, range/resume and version selection are not implemented in the GUI; full compatibility remains unverified.

A Fase 3 implementa leitura e escritas limitadas contra MiniStack 1.5.25. Exclusão, sobrescrita, retomada e versões não existem na GUI; compatibilidade completa permanece não verificada.

## MVP exclusions / Exclusões do MVP

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
