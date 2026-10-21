# Preview transport correction / Correção de transporte do preview

Date / Data: 2026-10-16. Host Java 25, AWS SDK 2.54.13. Scoped follow-up to [V3 verification](visual-v3-results.md); not V4, release approval or final-image validation. / Correção delimitada, sem iniciar V4 ou aprovar release/imagem final.

## Diagnosis and change / Diagnóstico e alteração

The unchanged 7s stalled-body assertions exposed JDK/SDK stream-close contention. Socket 1s plus `Connection: close` with the original managed body transformer was insufficient: the six original tests passed once, then the amplified eight-test class failed once; full verification had 106 tests and one failure at 18:42:46. Do not discard these failed runs or treat the header as proof of non-reuse. / Assert de 7s expôs disputa de fechamento. Primeira tentativa passou seis casos, falhou na regressão ampliada e no verify (106/uma falha); histórico preservado, header não prova ausência de reuso.

The owned preview client now hands successful streams to the adapter's bounded body reader before consuming them, with explicit abort on every reader exit. No SDK timer competes with that successful body read; caller-owned socket/monotonic limits remain essential. Shared download client and credentials configuration are retained, and adapter close must not close the shared client. Limits remain text 64 KiB + 1 detection byte, image 1 MiB + 1, body deadline 3s, request API/attempt 3s/2s, connect/socket 1s/1s. Original attachments, worker, routes, CSP and CSRF are unchanged. See [ADR-0018](../decisions/ADR-0018-bounded-text-preview.md).

Stream bem-sucedido passa ao leitor limitado do adaptador antes de consumir corpo, com abort em todas as saídas. Timers SDK não concorrem nessa leitura; limites próprios de socket/prazo são necessários. Cliente comum/provider preservados; fechamento próprio não pode fechar cliente comum. Limites de bytes/prazo mantidos, socket reduzido a 1s. Sem alterar anexos/worker/rotas/CSP/CSRF.

## Executed checks / Verificações executadas

- Focused eight-test class passed at 18:45:43 (`work/preview-stream-owner.log`), including eight internal repeated stalled/trickling cases, metadata refusal without draining the object and shared-client usability after owned-client close. Internal cases are not extra JUnit test methods. / Oito testes passaram, incluindo oito casos internos repetidos, recusa sem consumir objeto e cliente comum utilizável após fechar o próprio.
- Initial error-body check failed because it expected unavailable but received the legitimate forbidden mapping, not because of a deadline breach. The corrected fixture sends incomplete XML and accepts only sanitized forbidden/unavailable outcomes for HTTP 403; original HTTP 200 assertions still require unavailable. That focused test passed at 18:48:06 (`work/preview-error-xml.log`), covering stalled text and trickling image error paths under the unchanged 7s deadline. / Primeiro assert de erro confundiu 403 legítimo com transporte; fixture XML corrigida passou, sem relaxar prazo nem asserts originais de sucesso.

- Full host verification passed at 18:50:56: `STACKEMU_INTEGRATION=true`, Java 25.0.4.1, `mvn -B -ntp verify` with heap/processor limits; 107 tests, zero failures/errors/skips (`work/preview-owner-verify.log`). Spotless validation, checked templates, local MiniStack integrations and packaged fast-jar build completed. Generated SBOM is not a vulnerability scan or final-image evidence. / Verify completo no host passou 107 testes sem falhas/erros/ignorados, com integração MiniStack, templates e pacote; SBOM não equivale a scan/aceite da imagem.
- A separate repeat of the final nine-test transport class passed at 18:52:08, zero failures/errors/skips (`work/preview-owner-repeat.log`). Do not add repeated runs to the full-suite test count. / Repetição independente dos nove testes passou; não somar contagem ao total da suíte.
- The verified fast-jar started on the host JVM, loopback port 8080, PID 16992. Chrome 155.0.8059.80 passed all 13 preview cases in 41.7s (`work/preview-owner-browser.log`): seven image/six text, both languages/themes, original attachments, refusals and no-JS fallback. A separate 26-case V1–V3 interface regression passed in 1.9 minutes (`work/preview-owner-visual.log`), including density, native fallback, geometry/focus and axe/overflow assertions. No screenshot baselines were updated or compared against Linux here; diagnostic captures are not baseline approval. Test-owned S3 fixtures were cleaned by the suites. / Pacote verificado via JVM local; 13 casos de preview e 26 de interface passaram no Chrome, com fixtures próprias limpas. Sem comparar/atualizar baselines Linux ou inferir aceite humano.
- `npm run check` passed Prettier, markdownlint (106 Markdown files), five documentation-checker tests plus local links/index, Stylelint, ESLint, two contrast-checker tests and 44 palette comparisons (`work/preview-owner-style.log`). No check rule, timeout or tolerance was relaxed. / Checks de estilo/documentação/contraste passaram, sem relaxar regras, prazos ou tolerâncias.

Commands used / Comandos usados (PowerShell; host toolchain):

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
mvn -B -ntp test '-Dskip.frontend=true' '-Dtest=AwsS3PreviewAdapterTest' '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
node node_modules/@playwright/test/cli.js test -c playwright.preview-local.config.mjs --output work/preview-owner-browser
node node_modules/@playwright/test/cli.js test -c playwright.visual-local.config.mjs --output work/preview-owner-visual
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
```

Only MiniStack was running in Docker; the GUI container stayed stopped. The local transport verification blocker is resolved by this correction and fresh evidence, not by dismissing earlier failures. V3 maintainer review remains pending before advancing. / Somente MiniStack no Docker, GUI em contêiner parada. Bloqueio local resolvido por correção/evidência, não por descartar falhas. Revisão V3 do mantenedor pendente antes de avançar.

## Remaining limits / Limites restantes

Local repetitions do not prove universal wall-clock bounds, exhaustive cleanup-race coverage, advanced endpoint/proxy/TLS/provider compatibility, security certification or hostile-network isolation. Final new-image/runtime scan, full Linux browser/baseline reconciliation, V3 human review, assistive-technology review, D2–D6 and legal/name validation remain pending. No baseline/tolerance change, application image build, container suite, commit or push is included. / Repetições locais não comprovam prazo universal/cobertura exaustiva/configuração avançada/segurança. Gates finais, revisões e nome/jurídico pendentes; sem imagem, suíte em contêiner, commit/push ou mudar baselines.
