# Redesign E1 — token foundation / Redesenho E1 — tokens

Date / Data: 2026-10-14. Local Java 25 / JVM amd64, MiniStack 1.5.25 and pinned Chromium container. No release, remote deployment or GitHub publication performed. / Validação local; sem release, deploy remoto ou publicação no GitHub.

## Scope / Escopo

Semantic OKLCH palettes with hexadecimal fallbacks, legacy aliases, CSS layers, system font stacks, auto/dark/light theme resolution, more-contrast/forced-colors/reduced-motion support and solid surfaces under reduced transparency. Removed global nav/header/section/form styling; existing content-only card/form selectors are a documented bridge until E4/E5. No template, route, CSP, SDK or runtime-base changes. The theme link cycles auto → dark → light → auto without cookies. New contrast checker uses only Node built-ins; no third-party assets were added, so notices remain unchanged.

Paletas OKLCH/fallback, aliases, camadas CSS, fontes de sistema, temas automático/claro/escuro e preferências de acessibilidade. Estilos globais de nav/header/section/form foram removidos; seletores restritos ao conteúdo são temporários até E4/E5. Sem alterações em templates, rotas, CSP, SDK ou base da imagem. Alternância auto → dark → light → auto sem cookies. Verificador usa apenas módulos nativos Node, sem novos assets ou licenças externas.

## Commands / Comandos

Executed from the repository root; the integration flag targeted only the existing local MiniStack. / Executados na raiz, com integração restrita ao MiniStack local.

```powershell
mvn -B -ntp spotless:apply
$env:STACKEMU_INTEGRATION='true'
mvn -B -ntp verify
docker compose up -d --build gui
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run format
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run check
./scripts/check-style.ps1
git diff --check
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test
```

## Evidence / Evidência

- Maven verify: 52 tests passed, zero failed/skipped; includes the new UiContext default/override/link test and real MiniStack integration.
- `npm run check`: Prettier, markdownlint, Stylelint, ESLint and contrast gate passed. The gate checks 44 rows across actual OKLCH and hex palettes; two Node reference/failure tests passed. Minimum measured text ratio is 6.24:1 and minimum UI border ratio 3.32:1. Full per-role table is in [design system](../design-system.md).
- PowerShell analysis and diff whitespace check passed. Compose GUI rebuild succeeded, retaining emulator volumes.
- Final browser run: 11 tests passed in 55.8 seconds. The suite covers the existing eight tests plus three theme/preference tests: auto follows light/dark OS preference; explicit URL theme wins; locale links retain auto; widths 360/390/1440/1920 do not overflow the page and use the intended navigation layout; high-contrast tokens, reduced-motion duration and forced-color focus/current-page border are checked. Captures are under ignored `work/browser-results/`, with `work/browser-report.json` retaining results. Final auto-dark desktop and auto-light mobile captures were visually inspected.
- Initial local checks caught formatting/notation issues, an out-of-gamut hover and browser-test globals. These were corrected. Hover now mixes in sRGB; the reference tests intentionally detect unsupported/out-of-gamut colors and low contrast. Responsive overrides were moved to the component layer and the menu display is asserted at each viewport.

Maven: 52 testes aprovados, incluindo integração. Check de estilo, gate de 44 pares de contraste e dois testes Node aprovados. Análise PowerShell/diff aprovados. Execução final: 11 testes de navegador aprovados em 55,8 segundos, cobrindo sistema/URL/idioma, quatro larguras, alto contraste, movimento reduzido e foco/estado em cores forçadas. Capturas finais auto-dark desktop e auto-light mobile inspecionadas. Capturas/relatório ficam em `work/`, sem versionamento. Os problemas iniciais de formatação, gamut, globais de teste e precedência responsiva foram corrigidos, sem enfraquecer verificações existentes.

## Limits / Limites

No claim of WCAG conformance, comprehensive keyboard/screen-reader testing, actual Windows high-contrast user review, legacy-browser fallback rendering or Safari/Firefox compatibility. Reduced-transparency treatment is CSS-only with already-solid surfaces; it is not a measured translucency audit. Rendered long object keys, unavailable-emulator scenarios and visual baselines remain for E4–E7. E1 is a foundation, not the complete futuristic redesign: shell/icons/titles/favicon/cache busting start in E2; bento in E3. P2 security/preferences/performance proposals remain unimplemented. StackEmu legal name validation remains pending. No new vulnerability scan was run.

Sem certificação WCAG, revisão completa manual de teclado/leitor de tela, revisão real do alto contraste Windows, renderização em browsers antigos ou garantia Safari/Firefox. Transparência reduzida mantém superfícies já sólidas; não é auditoria de vidro. Chaves longas, indisponibilidade e baselines visuais seguem nas etapas futuras. E1 não é o redesign completo; shell/ícones/títulos/favicon/cache busting ficam na E2 e bento na E3. P2 não foi implementada. Validação jurídica do nome e nova varredura de vulnerabilidades não foram realizadas.
