# V2 — dashboard hierarchy / Hierarquia do dashboard

Date / Data: 2026-10-16. Maintainer reported V1 reviewed and authorized V2; [ADR-0013](../decisions/ADR-0013-visual-ux-evolution.md) records that source acceptance separately from final Linux/container evidence. / Mantenedor informou revisão V1 e autorizou V2; ADR separa aceite de fonte das evidências finais Linux/contêiner.

## Scope / Escopo

Connection status has greater desktop hierarchy with unchanged mobile stacking. Dashboard “Test connection” is a native link to the existing diagnostic page; “Refresh” retains the existing htmx/native listing refresh. The diagnostic tests connectivity through the existing S3 list operation, not a new health API. The count skeleton is static and aria-hidden, shown only while the existing panel is busy; the old count is hidden during loading and restored on transport failure with existing stale-result feedback. Unknown remains an em dash, never a fabricated zero. Native links, both languages/themes, CSP, CSRF and S3 boundaries remain intact. No new JavaScript, animation, dependency or route.

Status dominante no desktop, empilhamento mobile preservado. Diagnóstico é link nativo à página existente; Atualizar mantém refresh htmx/nativo. Conectividade é resultado da listagem S3, não API nova. Skeleton estático decorativo só durante carga real; contador anterior oculto na carga e restaurado com aviso em falha. Desconhecido permanece travessão, não zero. Idiomas/temas/fallback/segurança/limites preservados; sem JS, animação, dependência ou rota nova.

## Verification / Verificação

Executed locally with Java 25.0.4.1 and MiniStack already healthy; only the emulator was running in Docker. / Executado localmente com Java 25.0.4.1 e MiniStack saudável; somente emulador em Docker.

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
$env:PATH="$PWD\target\frontend\node;$PWD\target\frontend\node\node_modules\npm\bin;$env:PATH"
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
node node_modules/@playwright/test/cli.js test -c playwright.visual-local.config.mjs
```

The first Maven attempt finished at 17:27:19 -03:00 with 102 tests, zero assertion failures and one error: the pre-existing `RasterPreviewProcessTest.interruptionTerminatesWorkerAndStartupFailureIsReported` parsed an empty PID file. Inspection shows it waits for file existence, then interrupts the child, without waiting for nonempty content; this indicates a test synchronization race, not a proven V2 regression. No test, timeout or worker code was changed. The full command was repeated with the old GUI JVM stopped; it returned BUILD SUCCESS at 17:32:55 -03:00, 102 tests with zero failures/errors/skips, including integration, translation parity, Spotless and Qute/CSS packaging. The intermittent test condition remains unresolved and must not be hidden by the successful repeat.

Primeira tentativa Maven: 102 testes, uma exceção ao ler PID vazio no teste preexistente de interrupção do worker. O teste aguarda existência do arquivo e interrompe o filho sem aguardar conteúdo: indício de corrida de sincronização, não regressão V2 comprovada. Sem alterar testes, timeouts ou worker. Repetição completa com GUI anterior parada passou às 17:32:55, 102 testes sem falhas/erros/ignorados, incluindo integração, tradução, Spotless e build Qute/CSS. Condição intermitente continua não resolvida, sem ocultá-la pelo sucesso posterior.

`npm run check` passed: Prettier, markdownlint (104 documents), five documentation-checker tests, local link/index validation, Stylelint, ESLint, two contrast tests and 44 palette-pair checks. No lint rule, snapshot threshold or comparison tolerance was relaxed. / Checks passaram com essas contagens, sem relaxar regras/tolerâncias.

Host Chrome passed 20 tests in 50.0 seconds, zero failures/skips: ten V2 cases and ten fresh V1 regression cases, not a sum with prior runs. V2 covers four language/theme refresh cases with held-response loading, duplicate suppression, HTTP/network failure and recovery, keyboard focus, two viewport widths (390/1440), axe and connection-diagnostic navigation; four real Qute unavailable-response fixtures; two no-JavaScript navigation cases. The unavailable fixture and held transport are labelled presentation/network simulations, not a claim of a live outage or emulator latency. Live connected counts are compared with returned table rows, not a fixed number.

Chrome local passou 20 testes em 50,0 s, sem falhas/ignorados: dez V2 e dez de regressão V1 executados novamente, sem somar rodadas anteriores. V2 cobre quatro combinações idioma/tema com carga retida, duplicidade, falhas HTTP/rede/recuperação, foco, larguras/axe/diagnóstico; quatro fixtures Qute indisponíveis e dois casos sem JS. Fixtures/transporte retido são simulações identificadas, não queda real/latência do emulador. Contagem real comparada às linhas retornadas, não número fixo.

Thirty-two diagnostic PNGs were generated under ignored `work/browser-results/`, with no baseline comparison or update. Agent visual inspection covered English dark desktop loading and Portuguese light mobile dashboard. That inspection is not maintainer V2 acceptance or assistive-technology review. Logs remain in ignored `work/v2-maven.log`, `work/v2-maven-repeat.log`, `work/v2-style.log` and `work/v2-browser.log`; the updated GUI is a host JVM on loopback port 8080. No image build, full container suite, commit or push was performed in this slice.

Trinta e duas capturas diagnósticas em work ignorado, sem comparar/atualizar baselines. Agente inspecionou carga desktop escura em inglês e dashboard mobile claro em português; não é aceite V2 pelo mantenedor nem revisão de tecnologia assistiva. Logs locais indicados; GUI em JVM no host/loopback 8080. Sem imagem, suíte completa em contêiner, commit ou push nesta fatia.

## Remaining acceptance / Aceite restante

Maintainer V2 review, final new-image/container/full Linux browser checks, deliberate baseline reconciliation and human assistive-technology review remain pending. No baselines or tolerances are changed here. V3 is not started. StackEmu legal/name validation and D2–D6 remain pending. / Revisão V2 pelo mantenedor, imagem nova/contêiner/navegador Linux completo, baselines e tecnologia assistiva pendentes. Sem mudar referências/tolerâncias. V3 não iniciada; nome/jurídico e D2–D6 pendentes.
