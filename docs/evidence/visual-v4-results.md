# V4 motion and feedback / Movimento e feedback V4

Date / Data: 2026-10-17 (America/Sao_Paulo). Maintainer authorized continuation after [V3](visual-v3-results.md) and the [transport correction](preview-transport-correction.md). This does not invent a human accessibility/V3 review report or authorize final-image tests/publication. / Continuação autorizada após checks registrados, sem inventar revisão humana nem autorizar validação final/publicação.

## Implemented scope / Escopo implementado

Authored semantic CSS adds optional card entry using starting-style: summary/resource/metadata cards translate 4px to their normal position over the existing 180ms token, with full text opacity, no stagger, loop, hiding or focus delay. Unsupported at-rules leave visible final content. CSS-only cross-document same-origin navigation opt-in uses the browser's root snapshot animations at 180ms, zero delay. No navigation interception, JS View Transition calls, new dependency, cookie/storage, font or CSP change. Motion duration does not measure or bound HTTP/navigation latency.

CSS semântico autoral adiciona entrada opcional de cards por translação 4px/180ms, texto opaco e sem escalonamento/loop/ocultar/atrasar foco. Regras não suportadas deixam estado final visível. Transições CSS nativas entre documentos da mesma origem usam root/180ms sem atraso; sem interceptação/API JS/dependência/storage/fonte/CSP nova. Duração não mede nem limita latência HTTP/navegação.

Existing bucket/key clipboard now records pending/success/failure only around the real promise; success/failure retain localized live text and a 3px status border (120ms color transition). Denial still focuses/selects the original manual-copy field, with no success before resolution and no global duplicate toast. No new command-copy button. Motion reduction disables ordinary/card transitions, document opt-in and view-transition pseudo animations. Skeletons remain static and operation notices persistent; native forms/links, CSRF, original bytes and read/mutation boundaries are unchanged.

Clipboard existente ganha estado pendente/sucesso/falha ligado à promessa real, texto live/borda persistentes; recusa mantém seleção/foco sem sucesso antecipado/toast duplicado. Sem cópia nova de comandos. Movimento reduzido desliga efeitos/transições entre documentos; skeletons estáticos/avisos persistentes, fallback/segurança/anexos/limites de leitura e escrita preservados.

## Verification / Verificação

Fresh local checks completed on 2026-10-17; counts are separate runs, not sums of repeated methods. / Checks locais novos concluídos; não somar métodos repetidos entre rodadas.

- Java 25.0.4.1 / Maven verify passed at 06:45:48: 107 tests, zero failures/errors/skips, with `STACKEMU_INTEGRATION=true`, local MiniStack, Spotless, checked Qute templates, CSS generation and packaged fast-jar (`work/v4-maven.log`). Only MiniStack was running in Docker; no GUI container/image build or container suite. Generated SBOM is not a candidate vulnerability scan. / Verify passou com 107 testes sem falhas/erros/ignorados e integração apenas local; SBOM não equivale a scan da imagem candidata.
- First focused six-case browser run passed in 26.6s (`work/v4-motion-browser.log`). Subsequent coverage checks no-JS with reduced motion in dark and with normal motion in light; four enhanced language/theme combinations also test live preference changes. / Primeira rodada focada passou; cobertura subsequente inclui sem JS com/sem movimento e mudança de preferência nos quatro casos melhorados.
- Expanded host Chrome 155.0.8059.80 regression passed 50 cases in 2.8 minutes (`work/v4-browser-final.log`): V1–V4 visual/dashboard/density, drawer/clipboard races/fallback and operation notifications. This includes the six new V4 methods, not 50 plus six distinct tests. / Regressão ampliada passou 50 casos, incluindo os seis V4, sem somar repetições.
- Preview regression separately passed 13 cases in 45.0s (`work/v4-preview-browser.log`), seven image/six text, both languages/themes, refusals, original attachment bytes and no-JS. / Treze previews passaram, incluindo recusas/anexos originais/fallback.
- Initial style check failed six `no-undef` references inside the new browser test. Qualifying browser APIs with `window` fixed the test without changing lint rules or runtime behavior. Complete `npm run check` then passed (`work/v4-style-corrected.log`): Prettier, markdownlint (107 Markdown files), five documentation-checker tests, 457 local links/index, Stylelint, ESLint, two contrast tests and 44 palette comparisons. The final six-case test file was rerun and passed in 24.4s (`work/v4-motion-final.log`); repeated cases are not extra coverage totals. / Lint inicial detectou seis globais no teste; corrigidas com window sem relaxar regras/runtime. Checks completos passaram, e seis casos finais repetidos também passaram, sem inflar contagem.

The four enhanced V4 cases observe an actual 180ms card-entry transition on a test-inserted card, with full opacity/final no translation, and a real native same-origin page transition with every observed animation duration below 300ms. Test-only pagereveal telemetry is not shipped. Reduced motion yields no document transition or active animation, card/clipboard transition duration zero. Clipboard promise fixtures hold pending state (no success, disabled button), then resolve or reject; exact key, persistent localized status, manual selection/focus, Escape/return focus, mobile 390px/desktop 1440px overflow and axe assertions pass. Test-only promise simulation is not proof of OS clipboard permissions, screen-reader announcements or old-browser compatibility.

Casos V4 observam transição real de entrada 180ms em card inserido pelo teste, opacidade plena/estado final sem translação e navegação nativa com durações observadas abaixo de 300ms. Telemetria não vai ao produto. Redução resulta em ausência de transição/animação e duração zero nos cards/clipboard. Promessa simulada fica pendente sem sucesso e depois resolve/recusa; chave exata/texto/seleção/foco/Escape, axe e overflow em 390/1440px passaram. Não comprova permissões reais, anúncios de leitor de tela ou browsers antigos.

Diagnostic captures are under ignored `work/v4-motion-final` and `work/v4-browser-final`; they are not pinned-Linux baseline comparisons. Agent visual inspection covered the Portuguese light mobile clipboard-failure capture from the first focused run, not human acceptance or an image-by-image final baseline review. No tolerance, assertion deadline or check rule was relaxed. The verified package is running on host loopback port 8080, PID 41600. / Capturas diagnósticas não equivalem a baselines Linux. Agente revisou recusa clipboard pt-BR/claro/mobile da primeira rodada, sem inferir aceite humano/revisão final completa. Regras/prazos/tolerâncias preservados; GUI JVM verificada em loopback 8080.

Commands used / Comandos usados:

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
node node_modules/@playwright/test/cli.js test -c playwright.visual-local.config.mjs --output work/v4-browser-final
node node_modules/@playwright/test/cli.js test -c playwright.preview-local.config.mjs --output work/v4-preview-browser
node node_modules/@playwright/test/cli.js test -c playwright.visual-local.config.mjs --grep V4 --output work/v4-motion-final
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
```

## External references and limits / Referências e limites

[ADR-0013](../decisions/ADR-0013-visual-ux-evolution.md#v4-continuation-authorized--2026-10-17--continuação-v4-autorizada) records the scoped contract and primary-reference review. MDN labels starting-style Baseline 2024 newly available; that is not old-browser coverage. The Chrome cross-document guide documents same-origin opt-in, skipped/slow navigation and browser-specific support. The attempted cross-document web-features raw record and MDN at-view-transition retrieval failed; no cross-browser Baseline conclusion is inferred from those attempts. Cross-document support is not assumed equivalent to same-document View Transitions. / ADR registra contrato/fontes; Baseline starting-style não comprova browsers antigos. Consultas de registro bruto interdocumento e MDN at-view-transition falharam, sem inferir Baseline amplo ou equivalência intra/interdocumento.

Pinned Linux baseline reconciliation, full browser/container verification with a new image, human assistive-technology/V4 visual review, D2–D6 and StackEmu legal/name validation remain pending. V5 self-hosted fonts is deferred; V6 is not executed here. No baseline tolerance is relaxed; no image build, commit or push. / Baselines Linux, validação final com imagem nova, revisões humanas, D2–D6 e nome/jurídico pendentes. V5 adiada/V6 não executada; sem relaxar tolerância, imagem, commit/push.
