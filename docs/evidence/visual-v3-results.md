# V3 — native table density / Densidade nativa de tabelas

Date / Data: 2026-10-16. Maintainer reported testing V2 and authorized V3; [ADR-0013](../decisions/ADR-0013-visual-ux-evolution.md) records scope. / Mantenedor informou teste V2 e autorizou V3; ADR registra escopo.

## Contract / Contrato

Native labelled preference links render `data-density=comfortable|compact`. Query/form values are explicitly normalized in the web view context; absent, malformed and unknown values choose comfortable. Compact URLs retain `density=compact`; ordinary comfortable navigation omits the default parameter. Density switches preserve bucket/prefix/opaque cursor, language and theme. Links, fragments, previews and mutation forms/results/errors propagate the value; raw downloads/image bytes do not depend on presentation density. No cookie, local storage, runtime JS, SDK/application contract or CSP change.

Links nativos rotulados definem data-density. Valores de URL/form são normalizados no contexto web; ausente/desconhecido/malformado usa confortável. Compacta acompanha URL; navegação confortável comum omite parâmetro padrão. Troca mantém bucket/prefixo/cursor opaco/idioma/tema, com propagação em fragmentos, previews e formulários/resultados/erros. Downloads/bytes de imagem não dependem da densidade. Sem cookie, armazenamento, JS runtime, contrato SDK/aplicação ou CSP novos.

Cell padding is 12px vertically/horizontally in comfortable mode and 4px/8px in compact mode (default 16px root scale); object-panel padding is 20px/16px. Fonts and 44px object-link/download targets remain unchanged. Existing sticky headers, folder/file icons, filename-type hint badges and native scroll regions are retained. Actions remain visible at rest and gain border/underline emphasis on row focus. Long names remain escaped/verbatim and wrap, not ellipsize; horizontal overflow is table-local. This does not add a file-validation or preview allowlist based on suffixes.

Espaçamento de células confortável 12px/12px e compacto 4px/8px na escala raiz padrão; painel de objetos 20px/16px. Fonte e alvos 44px preservados. Cabeçalho fixo, ícones/badges indicativos e scroll nativo mantidos. Ações sempre visíveis; foco acrescenta borda/sublinhado. Nomes escapados/verbatim quebram, sem reticências; rolagem só na tabela. Sufixos não validam formato nem ampliam allowlist.

## Verification / Verificação

Agent inspection of the first passing browser run identified an 8px row strip above sticky headers caused by scroll-container padding. The final CSS removes that padding and uses a compact object-link focus offset of 1px (3px outline plus 1px offset fits 4px cell padding). The sticky-header test was tightened from a 10px allowance to 1px, and explicit comfortable/compact long-key captures and a pre-wrap check were added. No baseline tolerance was relaxed. / Inspeção da primeira rodada aprovada encontrou faixa 8px de linha acima do cabeçalho devido ao padding do scroll. CSS final remove padding e usa offset 1px no foco compacto; outline 3px cabe na célula. Teste de sticky foi apertado de 10px para 1px; capturas de nome longo nos dois modos e check pre-wrap adicionados. Sem relaxar tolerância de baseline.

Initial V3 Maven verification passed at 17:52:11 -03:00 with 104 tests and zero failures/errors/skips (two new `UiContextTest` cases). The first focused browser run passed all 26 tests before the sticky-padding correction. After that correction, full Maven verification at 18:00:44 -03:00 had 104 tests with one failure, no errors/skips: `AwsS3PreviewAdapterTest.imageTransportBoundsStalledResponseBody` exceeded its unchanged 7-second deadline. Its stack waited in JDK `MeteredStream`/`KeepAliveStream.close` through SDK response cleanup. No transport/worker test, timeout or implementation was altered. This may involve transport cleanup concurrency, but cause/repeatability are not established; do not label it harmless or infer that a repeat erases it. A full repeat and final browser/style checks are tracked below. / Primeira verificação V3 passou com 104 testes (dois novos), e navegador inicial passou 26 antes do ajuste de sticky. Após ajuste, Maven teve uma falha de prazo inalterado de 7 s na limpeza JDK/SDK do corpo de imagem parado. Sem alterar transporte/testes/timeouts. Possível concorrência de cleanup, causa/reprodutibilidade não comprovadas; não descartar nem apagar com repetição. Repetição completa e checks finais abaixo.

## Remaining acceptance / Aceite restante

### Final execution checkpoint / Checkpoint final de execução

The full repeat at 18:03:06 -03:00 reproduced the same stalled-image-body deadline failure: 104 tests, one failure, no errors/skips. Do not report Maven verification as passing for the final V3 state. Initial V3 success and the two subsequent failures are separate runs, not additive totals. The transport and worker code/tests remain unchanged; root cause is unresolved and the finding is also recorded in current security/limitations docs.

Repetição completa às 18:03:06 reproduziu a mesma falha: 104 testes, uma falha, sem erros/ignorados. Não declarar Maven final aprovado. Sucesso inicial e duas falhas posteriores são rodadas separadas, sem somar contagens. Transporte/worker/testes intactos; causa não resolvida e limitação registrada.

For host-only UI diagnosis, existing generated CSS was packaged with the following explicit skip command, which returned BUILD SUCCESS at 18:05:20 -03:00. It is not verification success, a release, a new image or a waiver of the failed test. An earlier unquoted PowerShell argument attempt failed before the lifecycle and was corrected with quoted `-D` arguments. / Para diagnóstico local, CSS já gerado foi empacotado pelo comando abaixo, sem confundir sucesso de pacote com verificação/release ou dispensa. Primeira chamada sem aspas falhou no parsing PowerShell antes do lifecycle; corrigida com aspas.

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
# verify above failed as recorded; diagnostic packaging only / acima reprovou; pacote diagnóstico:
mvn -B -ntp package '-DskipTests' '-Dskip.frontend=true'
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
node node_modules/@playwright/test/cli.js test -c playwright.visual-local.config.mjs
```

Final host Chrome run passed all 26 cases in 1.4 minutes, no failures/skips: six V3 cases plus 20 fresh V1/V2 regressions. Four V3 language/theme cases compare comfortable/compact row geometry, verify 44px actions, exact escaped long-key text/pre-wrap, focus emphasis, sticky-header top within 1px after scrolling, mobile/desktop axe/overflow, preserved opaque pagination, drawer/full-detail/text-preview density, original attachment bytes and native folder-result density. Two no-JS cases verify defaulting, refresh/language links and invalid native POST density, with no preference cookie. UUID-owned local S3 fixtures were cleaned up by asserted hooks. These are specific checks, not full browser, security or WCAG certification; image-preview density and every upload-error variant were not independently browser-exercised in compact mode.

Chrome final passou 26 casos em 1,4 min, sem falhas/ignorados: seis V3 e vinte regressões V1/V2 novas. Quatro combinações idioma/tema cobrem geometria, alvos, nomes longos, foco/sticky (1px), axe/overflow, cursor, drawer/detalhe/preview texto, bytes originais e resultado de pasta; dois casos sem JS cobrem fallback/refresh/idioma/erro POST/ausência de cookie de preferência. Fixtures locais próprias removidas com asserts. Não é certificação/suíte completa; preview de imagem e cada variante de erro de upload não foram testados independentemente em compacto no navegador.

Forty-eight final diagnostic PNGs are under ignored `work/browser-results/`, never compared with or substituted for Linux baselines. Agent inspection covered final English dark desktop long-key wrapping and English light mobile sticky-header behavior; no maintainer V3 or human assistive-technology acceptance is inferred. `npm run check` passed Prettier, markdownlint (105 documents), five documentation-checker tests, local link/index validation, Stylelint, ESLint, two contrast tests and 44 palette pairs. A direct npm-script-file invocation produced no check output and is not counted; checks were rerun via the explicit Node CLI above. No rule/tolerance was relaxed.

Quarenta e oito capturas diagnósticas finais em work ignorado, sem substituir/comparar baselines. Agente revisou nome longo escuro desktop e sticky claro mobile em inglês, sem inferir aceite humano. Checks explícitos Node passaram com as contagens acima; chamada direta do arquivo CLI sem saída não foi contada. Sem relaxar regras/tolerâncias.

Logs: `work/v3-maven.log` (initial pass), `work/v3-maven-final.log` and `work/v3-maven-repeat.log` (failures), `work/v3-package-diagnostic-final.log`, `work/v3-style-final.log`, `work/v3-browser-final.log`. The loopback GUI is diagnostic-only; only MiniStack runs in Docker. No application image, container test suite, commit or push was produced. V3 general verification remains blocked; V4 is not authorized by browser success. / Logs preservam sequência; GUI de diagnóstico e somente MiniStack no Docker. Sem imagem/suíte em contêiner/commit/push. Verificação geral V3 bloqueada; navegador aprovado não autoriza V4.

Maintainer V3 review, final new-image/full Linux/browser/baseline reconciliation and human assistive-technology review remain pending; no baselines or tolerances are changed. V4 is not started. Existing PID-file race observed in V2 is not silently fixed by this visual slice. D2–D6 and StackEmu legal/name validation remain pending. / Revisão V3, validação final/imagem nova/Linux/baselines e tecnologia assistiva pendentes; referências/tolerâncias intactas. V4 não iniciada. Corrida de PID observada em V2 não é corrigida implicitamente. D2–D6/nome/jurídico pendentes.
