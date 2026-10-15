# E5 — forms and request feedback / Formulários e feedback

Date / Data: 2026-10-15.

## Scope / Escopo

Native mutation forms receive component styling, operation-specific headings, explicit invalid/read-only/disabled states, 16px field/CLI text, and associated help/errors. Folder errors cannot emit duplicate key-error IDs. Upload guidance explains original-name policy, size limit and file reselection after validation. Submit feedback disables only the submit button; payload inputs and native POST/CSRF/enctype remain unchanged. Page restoration resets the progressive busy state. This is not server idempotency, cancellation, resumable upload or measured upload progress.

Formulários nativos recebem classes, título da operação, estados explícitos, texto de campos/CLI em 16px e ajuda/erros associados. Erros de pasta não duplicam o ID key-error. Upload explica nome original, limite e nova seleção do arquivo após validação. O feedback desabilita somente o botão: payload, POST, CSRF e enctype permanecem. Restauração da página limpa o estado ocupado. Não é idempotência, cancelamento, retomada nem progresso medido de upload.

The small same-origin `app.js` handles DOM text/attributes and local htmx lifecycle events, with no inline/eval/HTML injection or runtime dependency. Its URL uses the existing content fingerprint catalog. Refresh keeps the trigger/focus outside the swapped fragment, drops duplicate in-flight requests, sets/clears `aria-busy`, and announces loading/failure outside the busy region. Failure explicitly says that the old result remains. Native forms and full-page refresh navigation remain available without JavaScript. CSP is unchanged. ESLint covers the new runtime script; localized strings come from escaped Qute data attributes.

`app.js` local altera apenas texto/atributos e acompanha o htmx, sem inline/eval, injeção HTML ou dependências. A URL usa hash do catálogo existente. O refresh mantém controle/foco fora do fragmento, descarta duplicatas em andamento, acompanha `aria-busy` e anuncia carregamento/falha fora da região ocupada. Falha identifica resultado anterior. Sem JS, formulários e navegação continuam nativos. CSP permanece; ESLint cobre o script e textos traduzidos chegam por atributos Qute escapados.

## Verification / Verificação

Executed locally / Executado localmente:

- `mvn -B -ntp spotless:apply`: passed / aprovado.
- `STACKEMU_INTEGRATION=true` with `mvn -B -ntp verify`, Java 25: BUILD SUCCESS, 57 tests, zero failures/errors/skips, MiniStack local only. / 57 testes, sem falhas, erros ou testes ignorados, apenas MiniStack local.
- `docker compose up -d --build gui`: passed; packaged E5 GUI running locally. / aprovado; GUI E5 empacotada em execução local.
- `npm run format` and `npm run check` through `node:24.10.0`: passed; Prettier, Markdown, CSS, ESLint, two contrast reference tests and 44 contrast rows. A final targeted ESLint run also passed for `feedback.spec.mjs` and `app.js`. / aprovados; lint final direcionado também aprovado.
- `./scripts/check-style.ps1` and `git diff --check`: passed / aprovados.
- Final full Playwright suite through `mcr.microsoft.com/playwright:v1.64.1-noble`: 18 passed, no retries. Covers both languages/themes, htmx busy/loading state and duplicate-request drop, HTTP/network failure with unchanged old results, recovery/retry, native form state, payload/CSRF preservation, server validation, back navigation, three-form axe/responsive checks, no-JavaScript native validation and existing S3/theme/assets regressions. The script URL hash is checked against its actual served bytes. / Suíte final completa: 18 aprovados, sem retries, cobrindo idiomas/temas, estados/falhas/recuperação, formulários, payload/CSRF, retorno, axe, ausência de JS e regressões existentes; hash do script comparado aos bytes servidos.

### Test-fixture correction / Correção da fixture

The first full run had 14 passes and four new-test failures: locator assertions waited for a deliberately held native navigation instead of observing the old form. A second exploratory run was stopped after the pending-navigation instrumentation timed out. The final test cancels navigation only in a one-shot test listener registered after the app listener to inspect/capture transient UI, then reloads and verifies a separate real invalid native POST, its CSRF/confirmation payload and server response. No runtime behavior or security assertion was weakened to accommodate the test. The isolated corrected scenario passed before the final full 18-test run.

A primeira execução teve 14 aprovações e quatro falhas novas: asserções aguardavam uma navegação nativa retida pela fixture. Uma tentativa exploratória posterior foi interrompida após timeout da instrumentação. O teste final cancela a navegação apenas em um listener de teste único, posterior ao listener do app, para observar/capturar o estado transitório. Depois recarrega e verifica POST nativo inválido real, payload CSRF/confirmação e resposta do servidor. Não houve relaxamento do runtime ou das asserções de segurança. Cenário isolado corrigido passou antes da suíte final de 18 testes.

Generated screenshots live under `work/browser-results/feedback-progressive-request-feedback-{en|pt-BR}-{dark|light}/`, named `e5-{submitting|validation}-{language}-{theme}.png`. Mobile dark English submitting and light Portuguese validation captures were visually inspected. These are controlled functional fixtures and generated artifacts, not visual regression baselines or proof of a particular browser back/forward-cache mode. / Capturas geradas nesses caminhos; envio mobile escuro em inglês e validação clara em português foram inspecionados. São fixtures controladas e artefatos, não baselines nem prova de modo específico de cache do navegador.

## Limits / Limites

Controlled browser delays and HTTP/network failures are functional fixtures, not latency/load measurements or actual emulator outages. Browser/axe evidence is partial, not full WCAG or screen-reader certification. No new vulnerability scan, bounded-heap streaming test or publication is claimed. E1–E4 evidence remains historical; those stages did not add this script. StackEmu legal/name validation remains pending.

Atrasos/falhas controlados no navegador são fixtures funcionais, não medições de latência/carga nem indisponibilidade real do emulador. Evidência navegador/axe é parcial, não certificação WCAG ou de leitores de tela. Sem novo scan, teste de heap limitado ou publicação. E1–E4 permanecem históricas, anteriores ao script. Validação jurídica/do nome StackEmu continua pendente.
