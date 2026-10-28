# ADR-0033 — Consistent interaction feedback / Feedback consistente das interações

- Date / Data: 2026-10-27.
- Status / Estado: maintainer-authorized platform standard / padrão da plataforma autorizado pelo mantenedor.

Notification refinement: [ADR-0034](ADR-0034-compact-notifications.md) supersedes the manual-only success-notice rule; other interaction contracts remain. / Refinamento substitui persistência manual de avisos de sucesso; demais contratos preservados.

- Local verification / Verificação local: [recorded results / resultados registrados](../evidence/interaction-standard-results.md).

The maintainer requests the same anti-flicker behavior across the platform and future implementations. Adopt the [interaction feedback standard](../design/interaction-feedback.md), using the shared frontend helper and existing checked-fragment controllers. This extends ADR-0025/0032 and supersedes initial empty loading dialogs and immediate transient read-loading text. It does not remove real preparation/progress, errors or security checks; no framework migration, generic service discovery or application-layer dependency.

O mantenedor solicita o mesmo comportamento sem lampejos na plataforma e nas próximas implementações. Adotar o padrão vinculado, helper frontend compartilhado e controladores existentes de fragmentos verificados. Estende ADR-0025/0032 e substitui janelas iniciais vazias/texto transitório imediato em leituras; sem remover trabalho real, erros, gates, migrar framework ou criar descoberta genérica/dependências na aplicação.

Fast reads open complete content with no minimum wait. Slow reads use source feedback after 200 ms. Existing content remains during a checked replacement; failures remain explicit. Writes/long operations expose actual state immediately and retain no-retry, CSRF/CSP, exact-target, resource limits and honest uncertain outcomes. Native downloads remain native; no fake completion. Preferences, history, focus, accessibility and practical canonical fallback remain.

Leituras rápidas abrem conteúdo completo sem espera mínima; lentas sinalizam no acionador após 200 ms. Preservar contexto durante substituição verificada e falhas explícitas. Escritas/operações longas mostram estado real imediatamente e preservam no-retry, CSRF/CSP, alvos, limites e incerteza honesta. Downloads nativos sem conclusão simulada; manter preferências, histórico, foco, acessibilidade e fallback.
