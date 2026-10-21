# Redesign E2 — shell and assets / Redesenho E2 — shell e assets

Date / Data: 2026-10-14. Local Windows/amd64 Java 25.0.4.1, MiniStack 1.5.25 and Chromium in `mcr.microsoft.com/playwright:v1.64.1-noble`. No remote publication or release. / Validação local, sem publicação remota ou release.

## Changes / Alterações

- Refined shared sidebar/header with opaque semantic surfaces, local brand mark, service navigation and explicitly labelled system/light/dark controls. Existing paths and locale/theme query propagation retained; no preference cookies or first-party runtime JavaScript added. Existing CSRF cookies remain unchanged.
- Twenty used coolicons v4.1 Line SVG fragments, pinned to upstream commit `42d70477683a0c44cd1732b32a9def8a94469c5a`. Decorative aria-hidden/focusable attributes added; duplicate group IDs removed; original path geometry/currentColor retained. Author/license links verified against official sources before incorporation. [Source inventory](../design/icon-sources.md), notices and both READMEs retain CC BY 4.0 attribution. Copy/search icons remain deferred until the actual controls exist.
- Original first-party stack-shaped favicon and inline brand SVG. Full-page Qute title slot overridden on overview, connection, gallery, bucket, object, error, mutation form and result. Names remain escaped; form titles identify the selected action.
- Local color-scheme/theme-color metadata. WebAssetCatalog provides SHA-256 URLs for fixed bundled CSS/favicon resources, read once per bean lifecycle; no Maven plugin, runtime dependency, HTTP route or CSP changes.
- Existing workspace text assertion now expects `Visão geral` rather than `◫Visão geral`, because the icon is decorative SVG. It still checks the exact active navigation item; no test was removed.

Shell refinado com superfícies opacas, marca local, navegação por serviço e controles explícitos de tema. Rotas/parâmetros preservados, sem cookies de preferência ou JS próprio; CSRF permanece. Vinte SVGs usados com origem/licença verificadas, IDs removidos e atribuição bilíngue. Favicon e marca próprios separados dos ícones CC BY. Títulos por página e metadados de cor; hashes SHA-256 dos assets locais, sem dependências/plugins/CSP novos. O teste de navegação mantém a comparação exata do texto, retirando apenas o glifo substituído pelo SVG decorativo.

## Commands actually run / Comandos executados

```powershell
mvn -B -ntp spotless:apply
mvn -B -ntp spotless:check
$env:STACKEMU_INTEGRATION='true'
mvn -B -ntp verify
docker compose up -d --build gui
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run format
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run check
./scripts/check-style.ps1
git diff --check
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test
```

## Results / Resultados

- Final Maven verify: 57 passed, zero failures/errors/skips. Includes integration, architecture, translations, localized titles, theme links, SHA-256 reference/content-change checks and all twenty SVG fragments checked for decorative attributes, duplicate IDs and prohibited executable/external constructs.
- Initial Maven run had two failures because a new first-party `<img>` logo violated existing strict no-img assertions in untrusted-key read/write screens. The logo was changed to inline SVG; both original security assertions remained intact and the complete final suite passed. No escaping/security test was weakened to permit the logo.
- Browser suite: 13 passed. Existing axe/navigation/theme/S3 flow coverage retained. New checks cover six screen/action titles in both languages and themes, current preference markers, decorative navigation SVGs, absence of inline styles/scripts, CSS/favicon HTTP 200 and equality between URL hash and actual served bytes, plus explicit theme links retaining page/language.
- `npm run check`: Prettier, markdownlint, Stylelint, ESLint, two Node contrast tests and 44 contrast checks passed. PowerShell analysis and diff whitespace checks passed. No token palette changes from E1; surfaces remain opaque.
- Final Compose image build succeeded and GUI container was recreated without deleting emulator volumes. SBOM generation remained part of Maven packaging; no new image vulnerability scan or separate 128 MiB/64 MiB streaming proof was run in E2.
- Final Portuguese dark desktop and light mobile captures were visually inspected. Both language/theme sets at 1440/390 are generated under ignored `work/browser-results/`; `work/browser-report.json` contains the run result. These captures are evidence, not approved visual regression baselines.

Verificação final: 57 testes Java e 13 de navegador aprovados, sem falhas ou skips Java. A primeira execução encontrou duas falhas pelo logo img; a marca virou SVG inline, preservando as verificações originais de leitura/escrita com chaves não confiáveis. Checks de estilo, contraste e PowerShell aprovados. Assets retornaram 200 e hashes corresponderam aos bytes servidos. Build local aprovado, volumes preservados. Capturas finais pt-BR desktop escuro e mobile claro inspecionadas; não são baselines visuais aprovados. Não houve novo scan da imagem ou prova isolada de streaming com heap limitado.

## Remaining work / Trabalho pendente

E3 introduces the bento dashboard and effective connection summary using real data only. E4–E6 cover table/form/gallery migration, accessible loading feedback, copy/search controls where justified and visual baselines. Full manual keyboard/screen-reader/zoom/high-contrast review remains pending; automated axe and token contrast do not certify WCAG compliance. Safari/Firefox, old-browser CSS fallback rendering and production exposure are not certified. P2 proposals remain unimplemented. StackEmu legal name validation is still pending. Changes are local, not committed/pushed by this stage.

E3 fará o bento e resumo de conexão com dados reais. E4–E6 cobrem tabelas/formulários/galeria, carregamento acessível, copiar/buscar quando justificados e baselines visuais. Revisão manual completa segue pendente; axe/contraste não certificam WCAG. Outros browsers, fallback antigo e exposição em produção não são certificados. P2 não foi implementada e a validação jurídica do nome continua pendente. Esta etapa não criou commit/push.
