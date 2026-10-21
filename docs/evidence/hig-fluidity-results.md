# HIG fluidity iteration / Iteração de fluidez HIG

Date / Data: 2026-10-20. Contract / Contrato: [ADR-0023](../decisions/ADR-0023-hig-fluidity.md). This record is distinct from the previous 125-Java/110-browser explorer result; those counts are historical until freshly rerun. / Registro separado da rodada anterior; contagens históricas não comprovam esta alteração.

## Scope / Escopo

Replace the earlier Slate gradient/glow aesthetic with solid neutral surfaces, separate decorative/control borders, compact mobile navigation, continuous breadcrumbs, grouped metadata, tonal secondary controls and 100/200ms press/entry feedback. Reuse the skill's hierarchy/spacing/feedback patterns through authored Qute/CSS; no imported reset, JSX, Apple assets or framework. Existing Java and runtime JavaScript contracts remain unchanged. / Composição neutra coesa, navegação/metadados e feedback refinados, sem importar exemplos ou mudar contratos Java/JS.

## Verification / Verificação

### Build and checks / Build e checks

- Java 25/Maven `verify`, `STACKEMU_INTEGRATION=true`, local MiniStack: final run passed 125 tests, zero failures/errors/skips; BUILD SUCCESS in 3m07s. `work/hig-fluidity-maven-final.log`. Earlier 3m13s run also passed 125, before the final drawer-height refinement; do not sum repeats as 250 distinct tests. / Verificação final local aprovada; repetição anterior não aumenta casos distintos.
- First Stylelint inspection flagged excessive generated OKLCH precision and selector ordering. Rounded authored palette values to the existing four-decimal convention and corrected selector placement/specificity; no rule was disabled. Final `npm run check` passed formatting, Markdown, the 117-document/556-link index check, Stylelint, ESLint and 44 fallback/OKLCH contrast checks. Logs: `work/hig-fluidity-style-final.log`, `work/hig-fluidity-contrast.log`. / Inspeção inicial encontrou precisão/ordem; correções sem relaxar lint, checks finais aprovados.
- New light-theme minima across the four tested surfaces: blue accent/link text 5.29:1, muted text 6.83:1, danger 5.90:1 and control borders 3.87:1 in OKLCH. Decorative separators are deliberately separate from functional control/focus borders and are not presented as 3:1 control boundaries. / Separadores decorativos não são bordas funcionais; medições não certificam acessibilidade integral.
- Local PowerShell analysis, GUI/fragment/health/configuration/gallery smoke and `git diff --check` passed. All 80 versioned PNG hashes match the preceding V6 proposal; none regenerated. / Análise/smoke/whitespace aprovados, baselines preservadas.

### Browser execution / Execução no navegador

- First four-case preflight failed only the new focus-outline assertion: after pointer activation, programmatic `focus()` does not establish keyboard input modality. Corrected the test to use Tab/Shift+Tab and require actual focus plus the existing visible outline, without changing product CSS to force mouse focus rings. Logs: `work/hig-fluidity-preflight.log` and traces. / Primeiro preflight falhou no assert de modalidade; teste passou a usar teclado real, sem alterar produto para ocultar falha.
- Corrected preflight: 4/4 passed in 31.2s, en/pt-BR and light/dark. Checks cover 390/1440px overflow, compact single-row mobile brand/navigation, title reachability, 48px standalone control/pressed target above 44px, 0.98 press feedback, reduced-motion opt-out, actual keyboard outline and supplemental axe. `work/hig-fluidity-preflight-corrected.log`; screenshots under `work/hig-fluidity-preflight-corrected/`. / Preflight corrigido aprovado, sem somar repetição como novos casos.
- Broad selected host-browser run: 114/114 passed in 10.0m, zero failures/skips/flaky. Logs/report: `work/hig-fluidity-browser.log`, `work/hig-fluidity-browser-report.json`; captures under `work/hig-fluidity-browser/`. Includes four new fluidity cases, strengthened drawer target checks, updated neutral-surface/100–200ms contracts, and existing service refresh/recovery, density, metadata races/focus, clipboard, original upload/download, preview/refusal, conditional deletion, preferences and native fallbacks. Do not add the repeated four-case preflight as four more distinct cases. This selection excludes pinned-Linux visual comparisons and the container-dependent upload spec; it is not the consolidated container suite. / Execução ampla local: 114 aprovados, sem falhas/ignorados/flaky; preflight repetido não aumenta casos distintos. Não é a suíte final em contêiner.

### Visual review / Revisão visual

Reviewed the new en-light 390px and en-dark 1440px Home captures, en-dark 1440px drawer and compact explorer/long-key captures against the preceding composition. Grouped facts retain full values; attachment/preview/review actions stay ahead of auxiliary content. Reduced chrome and changed hierarchy are implementation observations, not user-research findings or measured latency gains. This is a representative comparison, not every-image, maintainer or assistive-technology acceptance. / Revisão representativa de Home, drawer e explorer compacto, com valores completos/ações preservados; não é pesquisa, ganho de latência medido ou aceite humano/integral.

## Remaining gates / Gates restantes

Maintainer visual and assistive-technology acceptance, final new-image/Linux baseline review and legal/name validation remain pending. No fresh vulnerability scan, production validation, commit/push/tag or release is claimed. Daily GUI stays on host JVM with only MiniStack in Docker. / Aceites humano/visual, imagem nova/Linux e validação jurídica/do nome pendentes; sem scan/publicação/release novos.
