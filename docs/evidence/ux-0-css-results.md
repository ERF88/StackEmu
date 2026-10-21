# UX-0 — CSS decision evidence / Evidências da decisão CSS

Date / Data: 2026-10-15. Scope: isolated development spike, not a production redesign / spike isolado, não redesenho de produção.

## Decision / Decisão

Select Tailwind CSS/CLI **4.3.3**, build-time only, without Preflight, for incremental UX-1 migration. Keep application reset, semantic tokens, native theme/URL behavior and component boundaries. Do not introduce DaisyUI: this prototype does not establish a need for another component layer. Do not adopt Pico **2.1.1** as a drop-in overlay: this tested default configuration overrides the layered system and uses SVG data URLs blocked by the current CSP. This is not a claim that Pico cannot be customized safely.

Selecionar Tailwind/CLI **4.3.3**, apenas no build e sem Preflight, para migração incremental UX-1. Preservar reset/tokens, tema/URL e limites. Não adicionar DaisyUI sem necessidade comprovada. Não adotar Pico **2.1.1** como sobreposição direta: a configuração padrão testada sobrescreve o sistema em camadas e usa SVGs `data:` bloqueados pela CSP. Isso não significa que Pico não possa ser customizado com segurança.

Dependencies are installed only under `tools/ux0/` with an exact manifest/lockfile. Production dependencies, CSS/JS bytes, templates, Java classes and routes are unchanged. The next step must wire real CSS generation/fingerprinting into product packaging before changing production templates. The spike Dockerfile proves a separate Maven → Qute fixtures → Node CSS chain; it is not the shipped GUI image. CI steps are configured but have not been executed remotely in this turn.

Dependências ficam somente em `tools/ux0/`, com lockfile. Runtime, templates, classes/rotas e bytes CSS/JS do produto não mudaram. UX-1 deve integrar geração/hash ao empacotamento real antes de migrar templates. Dockerfile prova cadeia isolada, não imagem da GUI. CI configurado, sem execução remota nesta etapa.

## Measured additive CSS / CSS aditivo medido

| Candidate / Candidata                             | Raw bytes / Bytes brutos | Gzip bytes | Delta raw / Diferença bruta | Delta gzip |
| ------------------------------------------------- | ------------------------ | ---------- | --------------------------- | ---------- |
| Current / Atual                                   | 23,869                   | 4,891      | 0                           | 0          |
| Current + Tailwind utilities / Atual + utilidades | 24,483                   | 5,124      | +614                        | +233       |
| Current + Pico / Atual + Pico                     | 107,189                  | 16,322     | +83,320                     | +11,431    |

These are exact bytes for this small gallery/table/form prototype, retaining the entire existing app CSS in every candidate. They are NOT standalone framework sizes, a completed CSS migration, network-transfer measurements or a promise about final application size. Raw-file hashing/minification/gzip use the scripts and pinned Linux environment below. Two independent CLI invocations produced the same hash; the separate Docker chain matched it too.

São bytes deste protótipo pequeno, mantendo CSS atual nas três candidatas. NÃO são tamanhos isolados dos frameworks, migração concluída, tráfego medido ou promessa de tamanho final. Duas compilações e a cadeia Docker independente produziram o mesmo hash.

Tailwind candidate SHA-256: `679cd42d29222094631b229eeed87786edf96846a7824a35f993e8e51e572d3c`. Current CSS SHA-256: `a8dd93421ba4cfe510761b5591b6c61e5d421c63f56b41f6206d4fc1ce8669ef`.

## Qute, browser and security observations / Observações Qute, navegador e segurança

- A real Qute engine renders 18 fixtures with HTML escaping, including an HTML-like Unicode key shown as text. Literal classes in conditional branches compile; fragmented `p-{spacing}` renders `p-37` but deliberately has no generated selector. This negative control is a prohibition on fragmented production classes, not a supported styling technique. / Qute real com escape; classes completas funcionam e classe fragmentada é controle negativo.
- The custom dark variant covers explicit dark and auto/system dark; explicit light overrides the OS. Semantic colors remain the existing variables. The prototype checks 360/390/1440/1920px, both languages and four theme modes. / Variante cobre sistema/override; cores mantêm tokens existentes e larguras/idiomas são verificados.
- Twenty-four candidate cases completed. Current/Tailwind each had eight cases with zero axe violations, zero captured CSP/runtime errors and no page overflow at the four widths. These are partial automated checks, not WCAG or security certification. / 24 casos; atual/Tailwind sem violações/erros/overflow nas amostras, sem certificação.
- Pico diagnostics: four dark/auto-dark language cases reported `color-contrast`; 80 console-error occurrences across eight cases were CSP-blocked SVG `data:` image loads. These are repeated occurrences, not 80 distinct vulnerabilities. No CSP relaxation was made. / Pico teve contraste em quatro casos e 80 ocorrências de bloqueio CSP, não 80 vulnerabilidades distintas; CSP preservada.
- Native table-region focus after Tab, checkbox Space and details Enter were exercised. Direct package MIT license texts/copyrights were read and copied with hashes; transitive tooling licenses and vulnerabilities were not comprehensively audited. / Teclado nativo exercitado; licenças diretas lidas, sem auditoria integral transitiva/CVE.

The browser harness obtains the actual GUI `/components` CSP and fulfills same-origin test-only documents/assets. It does not add application endpoints or use real S3 mutations. Intercepted DOMContentLoaded samples are retained as diagnostics only, not comparable production timings; concurrent machine load was not controlled for benchmarking. No performance-budget/P2 policy is activated.

O harness lê CSP real e entrega documentos/assets de teste na mesma origem, sem endpoints ou mutações novas. Timings interceptados são diagnósticos, não benchmark; carga da máquina não foi controlada para tal. Sem orçamento/P2.

## Executed commands / Comandos executados

- Registry inspection: `npm view` for Tailwind/CLI, Pico and DaisyUI; direct MIT texts read from installed exact packages. DaisyUI was not installed. / Registro consultado e licenças diretas lidas.
- `mvn -B -ntp spotless:apply`, then `docker compose up -d ministack --wait` and `$env:STACKEMU_INTEGRATION='true'; mvn -B -ntp verify`: BUILD SUCCESS; **59 tests, zero failures/errors/skips**, Java 25, MiniStack 1.5.25 only. XML/JSON SBOM regenerated; not a vulnerability scan. / 59 testes aprovados e SBOM, apenas emulador local.
- `docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run spike:css`: locked install, two matching compilations, selector/negative/reset checks, 18 fingerprinted fixture pages, byte/license report. / Instalação fixada e verificações do build aprovadas.
- `docker build -f tools/ux0/Dockerfile --target css-spike -t stackemu-css-spike:local .`: succeeded with its own selected Qute test; Docker output hash matched the local-container output. Nested dependency exclusion was added to the Docker context; final context/build recheck is recorded below. / Build isolado aprovado e hash idêntico; exclusão de dependências aninhadas adicionada.
- `docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 mcr.microsoft.com/playwright:v1.64.1-noble node tools/ux0/check.mjs`: **24 cases completed**, including candidate diagnostics; 48 PNG captures. / 24 casos concluídos e 48 capturas.
- Scoped source checks: Prettier, Stylelint for `tools/ux0/input.css`, ESLint for both scripts and `git diff --check`: passed after correcting the Tailwind directive handling. / Checks das fontes aprovados após ajuste de diretivas.

The first full `npm run check` stopped at six input-CSS lint errors. Fixes: blank lines plus input-only string-import notation and macro-nesting exceptions; production selectors/rules were not loosened. Generated files remain under ignored `work/`; nested dependencies are excluded from Markdown lint. / Primeira execução parou em seis erros CSS; correções restritas à entrada, sem relaxar regras de produção.

The first ordinary 82-test browser run returned **79 passed / 3 failed**: desktop gallery full-page captures alternated by three pixels in document height and could not stabilize. Concurrent builds were running; causality is not established. Four isolated desktop gallery cases then passed without modifying code, baselines, retries, timeout or tolerance. A full normal rerun is recorded in the completion note below; the initial failure is not erased. / Primeira suíte teve 79 aprovações/3 falhas de estabilidade de altura; builds eram concorrentes, sem comprovar causa. Quatro casos isolados passaram sem mudanças; repetição completa fica registrada abaixo.

## Artifacts and review / Artefatos e revisão

Sources/reproduction: [spike guide](../../tools/ux0/README.md). Local reports/48 captures/direct license copies: ignored `work/ux0/`; raw Qute fixtures: ignored `work/ux0-fixtures/`. Representative inspected captures: [Tailwind dark desktop](../../work/ux0/tailwind-pt-BR-dark-1440.png), [Tailwind light mobile](../../work/ux0/tailwind-pt-BR-light-390.png) and [Pico dark desktop](../../work/ux0/pico-pt-BR-dark-1440.png). These are diagnostic prototypes, not a new approved brand or final UX. / Fontes/artefatos vinculados; amostras inspecionadas não aprovam estética final.

## Limits and next step / Limites e próximo passo

UX-1 must integrate the chosen build into the real Maven/Docker/CI package, preserve `WebAssetCatalog` hashing, migrate one component at a time and remove replaced CSS only after contrast/browser review. No utility adoption in production is implied by test markup. Keep native fallbacks, i18n, CSRF, attachment streaming and original basename/extension/MIME. Preview/delete/presigning and ADR-0012 remain gated separately. Human assistive-technology review, complete visual acceptance, other browsers/OS/ARM64, a tooling vulnerability scan and actual production performance remain unverified. No Git commit/push/release in this turn. StackEmu legal/name validation remains pending.

UX-1 deve integrar build real/hash e migrar/remover CSS por componente com revisão. Markup de teste não migra produção. Preservar fallback/idiomas/segurança e arquivo original; capacidades novas/P2 seguem separadas. Revisão humana, ambientes ampliados, scan de ferramentas e desempenho real permanecem não verificados. Sem commit/push/release; nome/jurídico pendentes.

## Completion note / Nota de conclusão

The second normal run had 81 passing cases and one unstable Portuguese dark desktop gallery capture. The diagnostic `tools/ux0/diagnose-gallery.mjs` measured the first full-page capture settling shell height from 2813.25px to 2816.25px. The next page capture was byte-identical to the existing 2816px-high baseline (SHA-256 `50f44d55f5fe0ba0b8d98862eaf2aa2d1dfbb297f90b398076416d7524373774`). No production CSS difference was present. The visual test now discards one gallery preparation capture before the full-page comparison; it does not mask/crop content, update baselines, increase deadlines/retries or relax pixel tolerances.

A segunda suíte teve 81 aprovações e uma falha de estabilidade na galeria PT escura desktop. O diagnóstico mediu ajuste de 3px após a primeira captura; a seguinte foi idêntica, byte a byte, à baseline existente. O teste descarta apenas uma captura preparatória da galeria antes da comparação integral, sem mascarar/cortar conteúdo, regenerar baselines ou alterar limites/retries/tolerâncias. CSS de produção não mudou.

After that preparation, `test visual.spec.mjs --grep "visual contracts: gallery,.*, 1440" --repeat-each 3 --output work/ux0-gallery-prepared` passed all **12 cases (50.4s)**. The final normal full invocation below passed **82 tests (4.6m)** with the existing **80 PNG comparisons**, zero test retries and no snapshot-update flag:

Após a preparação, 12 casos repetidos de galeria desktop e a suíte completa de 82 testes passaram, preservando as 80 comparações e sem atualizar snapshots:

```powershell
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test
```

Final `npm run check` in `node:24.10.0` passed: Prettier, Markdown (80 files), Stylelint, ESLint, two Node reference tests and 44 contrast rows. `./scripts/check-style.ps1`, Hadolint 2.12.0 on both Dockerfiles, actionlint 1.7.7 and `git diff --check` passed. Local link targets in all 12 changed Markdown files were checked. The final isolated Docker rebuild with pinned Node/Maven bases and nested dependency exclusion passed and retained the recorded CSS hash.

Checks finais aprovados: formatação, 80 Markdowns, CSS/JS, dois testes Node e 44 linhas de contraste, PowerShell, ambos Dockerfiles, workflow, diff e destinos de links dos 12 Markdowns alterados. Rebuild Docker isolado com bases fixadas e exclusão aninhada manteve o hash registrado.

Candidate axe/keyboard checks occur after the width loop at 1920px; overflow/theme checks cover all four widths, with captures at 390/1440px. The 48 candidate images plus ten diagnostic captures are not a new visual baseline or exhaustive human review. Initial/second regression JSON reports remain ignored under `work/ux0/`; ordinary final results are in `work/browser-report.json`. No new heap/load test, vulnerability scan, remote CI run or publication is claimed.

Axe/teclado das candidatas rodam após o loop em 1920px; overflow/tema cobrem quatro larguras e capturas usam 390/1440px. As 48 imagens e dez capturas diagnósticas não são nova baseline ou revisão humana integral. Relatórios iniciais/finais permanecem locais e ignorados. Sem novo teste de heap/carga, scan, CI remoto ou publicação.
