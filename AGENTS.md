# StackEmu contributor instructions

Notification standard: ADR-0034 supersedes manual-only success toasts. Use the shared compact notice controller: confirmed success expires after five visible seconds, paused by hover/focus/hidden documents; pending and attention outcomes stay until dismissed. Preserve accessible close/focus and durable results. / Avisos compactos comuns; sucesso expira com pausas, demais estados persistem, sem ocultar erros ou cancelar trabalho.

Interaction standard: read ADR-0033 and `docs/design/interaction-feedback.md` for all current/future UI work. Reuse `beginInteractionFeedback` and checked read controllers: no empty loading dialogs, no minimum wait, delayed source feedback for slow reads, immediate real mutation state, explicit failures, valid focus and canonical fallback. Never invent progress/download completion, cache forms/tokens/files or cancel/retry sent mutations. / Padrão obrigatório para UI atual/futura: helper comum, conteúdo completo, feedback de leitura no acionador, estado real de mutações e segurança preservada.

Current navigation: read ADR-0032. Object read views use a bounded same-bucket inspector history; preserve checked fragments, canonical returns, listing origin and exact preference targets. Earlier no-history/drawer-reset rules are superseded only for these tested read views. Do not intercept mutations or serialize files, signed URLs or credentials. / Navegação atual: inspector de leitura com histórico delimitado, fragmentos verificados e origem/alvos preservados; sem interceptar mutações ou serializar segredos/arquivos.

Current approved direction: read ADR-0029. Minimal dialogs and durable operations supersede the historical restrictions below as tested replacements are delivered. Versioned cleanup, nonempty prefixes, explicit replacement, object transfer and signed downloads are authorized. Keep CSRF/CSP, target integrity, bounded resources and honest partial/uncertain outcomes. / Direção atual: ADR-0029 autoriza evolução; restrições históricas cedem aos substitutos testados, preservando segurança e resultados reais.

Explicit reviewed-cleanup exception: ADR-0028 authorizes only the separate `/s3/empty-bucket` workflow with immutable manifest, exact typed bucket name and independent one-use/CSRF gates, capped at 25 entries. Never turn ordinary empty-bucket/folder deletion into recursive cleanup. / Exceção explícita: somente fluxo separado/revisado da ADR-0028; preservar gates individuais e limites.

Empty-folder correction: read ADR-0027. Only exact zero-byte markers with bounded complete empty-prefix proof may be removed, with fresh rechecks and existing ticket/CSRF/version/ETag/no-retry gates. Never delete children or infer recursive cleanup. / Pasta vazia: remover só marker exato comprovado vazio, sem filhos/lote, com gates e nova verificação.

Current bucket workspace: ADR-0026 supersedes bucket-row text controls and the density selector with accessible icons/one spacing. Empty-bucket deletion has independent one-use confirmation, CSRF, never-versioned/empty checks and no retries. Empty and delete is mapped only, not enabled. / Buckets compactos e exclusão vazia com gates próprios; limpeza em lote apenas mapeada.

Read `docs/development/code-style.md`, `docs/development/naming-and-responsibilities.md` and applicable ADRs before changing code.

Current visual authority: ADR-0023 explicitly permits cohesive HIG-driven redesign and supersedes frozen V1 decoration/visual recipes; do not treat historical colors, spacing or timings as permanent blockers. Security, accessibility, licensing and build contracts remain. / ADR-0023 autoriza reformulação coesa e substitui receitas visuais congeladas, sem dispensar segurança/acessibilidade/licenças/build.

Current controls/navigation: read ADR-0025 and ADR-0024. Action dialogs replace generic consent checkboxes; diagnostics/density/local-only notice move into Preferences. The maintainer explicitly authorizes JavaScript; complete no-JS parity is no longer a UX blocker. Keep practical canonical forms, the service catalog and all security/accessibility/build gates. / Janelas de ação substituem checkboxes; diagnóstico/densidade/aviso local em Preferências. JavaScript autorizado, sem bloquear UX por paridade completa sem JS; preservar contratos reais.

For visual/UX work, use the maintainer-requested `apple-hig-designer` skill and read `docs/design/apple-hig-guidelines.md` plus ADR-0022. Install/report an unavailable skill rather than ignoring it. Adapt to Qute/CSS, retaining project security, accessibility and build rules; do not import React, a global reset, Apple font/symbol assets or glass effects automatically. / Para visual/UX, usar a skill solicitada e o guia/ADR; adaptar à stack e preservar contratos, sem importar exemplos/efeitos indiscriminadamente.

For visual/UX work also read `docs/product/visual-ux-evolution.md` and ADR-0013. UX stages are plans, not implemented capabilities. Keep current CSS conventions until UX-0 is accepted; preserve native fallbacks, CSP, CSRF, original-file policy and read/mutation boundaries. Preview, deletion and presigned URLs require their own documented gates; never infer working routes/bindings from the illustrative snippet. / Para UX, ler plano e ADR-0013; preservar fallback/segurança e não tratar exemplos como APIs implementadas.

- Use English identifiers, purpose-revealing names and the documented role suffixes.
- UX-1 CSS build ownership is in `docs/development/frontend-build.md`: literal shell utility tokens only, semantic component tokens retained, no Preflight/CDN or generated CSS edits. / UX-1: seguir guia do build CSS; utilitários literais no shell, componentes semânticos preservados e sem editar gerados.
- Preserve `application`, `adapter.aws`, `adapter.web` boundaries. Do not introduce speculative modules or framework dependencies in application.
- Use constructor injection for collaborators. Keep presentation, SDK translation and HTTP security in their respective adapters.
- Preserve routes and checked-template bindings when renaming. Keep read and mutation contracts separate.
- Keep bilingual documentation synchronized. Do not rewrite historical test evidence as new results.
- Validate with Java 25 Maven verification and the established style checks. Integration requires `STACKEMU_INTEGRATION=true` and local MiniStack; never target production resources.
- Daily development/testing runs on the host JVM and local tooling. Docker runs only the S3 emulator (`docker compose up -d --no-deps ministack`); keep the GUI container stopped. Do not build application images or run container test suites during iterations. Consolidated validation with a new image is reserved for the final validation stage. / Desenvolvimento e testes diários locais via JVM/ferramentas do host; Docker somente para emulador S3, GUI em contêiner parada. Imagem nova e testes completos em contêiner apenas na validação final.
- Report only tests actually run. Do not claim naming tests prove cohesion, full style compliance or security.

Leia os guias e ADRs; preserve limites, contratos HTTP e docs bilíngues. Não crie arquitetura especulativa nem declare verificações não executadas.
