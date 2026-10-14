# ADR-0009 — browser quality and response policy

Status: Accepted / Aceita

## Decision / Decisão

Keep response security in `adapter.web`: REST responses receive nosniff, no-referrer, frame denial and a self-only CSP without inline/eval permissions. Configure htmx to disable eval and injected indicator styles; existing local CSS handles the indicator. Do not add HSTS to the HTTP-only local development origin or imply that headers replace CSRF/authentication.

Use pinned Playwright/axe development dependencies, Chromium against the packaged GUI and isolated local MiniStack fixtures. Keep runtime dependencies unchanged. Add ESLint for first-party test JavaScript, preserve routes and checked Qute templates, and associate field validation text with its input.

Manter política HTTP em `adapter.web`, sem permissões inline/eval, sem HSTS para HTTP local e sem promessa de autenticação. Desabilitar eval e estilos injetados do htmx; manter CSS local. Usar ferramentas de desenvolvimento fixadas, Chromium com GUI empacotada e fixtures locais isoladas, sem alterar dependências de runtime. Preservar rotas/templates e associar erros aos campos.

## Consequences / Consequências

CSP intentionally disallows arbitrary external scripts/resources and inline handlers. Future UI changes must preserve or explicitly review this policy, not loosen it silently. The REST filter does not establish a policy for every static/management endpoint. Automated browser/axe checks are partial evidence; no claim of full WCAG conformance, penetration test, load capacity or production suitability.

A CSP bloqueia recursos externos e handlers inline. Evoluções devem revisar a política explicitamente. O filtro REST não garante a mesma política em todos os endpoints estáticos/de gerenciamento. Testes automáticos são evidência parcial, não certificação WCAG, pentest, capacidade de carga ou adequação a produção.
