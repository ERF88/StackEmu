# Operational workspace validation / Validação do workspace operacional

Date / Data: 2026-10-14. Local evidence only; not a release or production certification. / Evidência local, não release nem certificação de produção.

## Changes / Alterações

Shared Qute sidebar/header shell, active-page navigation, grouped bucket/upload actions, native collapsible bucket metadata, responsive layout and existing semantic light/dark tokens. No imported premium template, external runtime asset, framework migration or analytics data. English and Brazilian Portuguese catalogs remain synchronized. Existing HTTP routes, CSRF forms, original filename/MIME policy and download streaming remain intact.

Shell compartilhado com sidebar/cabeçalho Qute, navegação com página atual, ações agrupadas, metadados recolhíveis nativos, layout responsivo e tokens claro/escuro. Sem template premium importado, assets externos, migração de framework ou métricas fictícias. Catálogos bilíngues sincronizados; contratos HTTP, CSRF, nome/MIME original e streaming preservados.

## Executed checks / Verificações executadas

- Java 25 `mvn -B -ntp verify` with `STACKEMU_INTEGRATION=true`: 51 tests passed, none skipped, using local MiniStack. The initial sandboxed attempt failed because loopback sockets were denied; the permitted rerun succeeded.
- Local Compose GUI image rebuilt successfully; only the GUI container was recreated. MiniStack volumes were retained.
- Eight Chromium tests passed in the existing `mcr.microsoft.com/playwright:v1.64.1-noble` image against local GUI/MiniStack. This includes existing axe/navigation coverage across both languages/themes, S3 write/download/validation/security flow and two new desktop/mobile workspace checks. The Windows attempt could not launch because its required browser binary was absent; no product assertion ran in that attempt.
- Prettier, Stylelint, markdownlint, ESLint and `git diff --check` executed successfully.
- Dark/light desktop and dark mobile screenshots visually inspected; generated screenshots and browser report are under ignored `work/browser-results/` and `work/browser-report.json`.

Java: 51 testes aprovados com MiniStack local; build Compose aprovado, preservando volumes. Chromium: oito testes aprovados no container existente, incluindo os fluxos anteriores e duas verificações novas de layout. A tentativa Windows não iniciou o browser por ausência do binário; a tentativa Maven no sandbox falhou por bloqueio do loopback. Prettier/Stylelint/markdownlint/ESLint/diff executados. Capturas desktop claro/escuro e mobile escuro inspecionadas; artefatos gerados em `work/`, não versionados.

## Remaining review / Revisão pendente

Full manual keyboard and screen-reader review, zoom/high-contrast review, usability feedback, and broader long-key/table/error-state visual review remain pending. Automated axe checks are partial evidence, not WCAG certification. This work did not repeat image vulnerability scanning, publish an image or create a release. StackEmu legal name validation remains pending.

Continuam pendentes revisão manual completa de teclado/leitor de tela, zoom/alto contraste, feedback de usuários e revisão visual ampliada de chaves longas, tabelas e erros. Axe não certifica WCAG. Não houve novo scan de vulnerabilidades, publicação de imagem ou release. A validação jurídica do nome StackEmu segue pendente.
