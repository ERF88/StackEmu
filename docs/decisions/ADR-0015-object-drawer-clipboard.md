# ADR-0015 — object drawer and clipboard / Drawer e clipboard

- Date / Data: 2026-10-15.
- Status: Accepted implementation contract; verification is separate / Contrato de implementação aceito; validação separada.

## Decision / Decisão

Enhance unmodified primary object-link clicks only when native dialog and the bundled htmx API are available. Use a modal right-side native dialog, full width on narrow screens. Name it with a persistent heading; focus Close on entry, retain native focus containment/inert background and Escape, and restore the exact triggering link on close. Do not put modal semantics on a nonmodal aside. Keep the ordinary object href for no-JavaScript, modified clicks, new tabs and direct navigation.

Melhorar apenas clique primário sem modificadores quando diálogo nativo e htmx estiverem disponíveis. Drawer modal à direita, largura total mobile, título persistente, foco em Fechar, contenção/inert/Escape nativos e retorno ao link exato. Preservar href normal para fallback, nova aba e navegação direta.

The drawer is transient: opening/closing does not push, replace or rewrite bucket history, prefix or pagination. Back/Forward navigate normal documents; pagehide/pageshow/popstate close and discard transient content. A visible full-page link provides the canonical navigable detail URL; reloading that URL renders the existing object page, not a fragment. This slice does not promise deep-linked modal state.

Drawer transitório: não modifica URL/histórico/prefixo/paginação. Back/Forward navegam documentos; eventos de navegação descartam conteúdo transitório. Link visível abre a página completa canônica. Sem promessa de deep link para estado modal.

Add read-only `GET /s3/object-panel` with the existing bucket/key/language/theme validation and one metadata read, no object-body download. Only `HX-Request: true` receives the checked `S3Resource/objectPanel` HTML fragment and `X-StackEmu-Fragment: object-details`; ordinary navigation renders the full page. Responses use no-store and Vary: HX-Request. Preserve real HTTP errors and existing security headers. Fragment data is Qute-escaped; no scripts, inline handlers, unescaped object HTML or SDK errors. Permit htmx to swap this one same-origin server template into the drawer only after checking active XHR identity, open state, 200 status and fragment marker. Runtime code must not assign innerHTML or parse emulator HTML. Disable htmx script-tag execution globally without relaxing CSP. This narrowly extends the prior feedback-only JavaScript convention.

Nova leitura de metadados pela rota acima, sem corpo do objeto. Fragmento Qute verificado apenas com HX-Request; navegação normal recebe página inteira. No-store/Vary e status reais, preservando cabeçalhos. Dados escapados, sem scripts/HTML bruto/erros SDK. Swap htmx restrito ao template local com XHR ativo, modal aberto, status 200 e marcador. JS próprio não atribui innerHTML nem interpreta HTML do emulador. Desabilitar execução de scripts htmx sem relaxar CSP; exceção delimitada à regra anterior de feedback.

Abort pending XHR on close/reselection. Independently reject stale responses before swapping; abortion alone is not the race contract. Bound browser requests to 10 seconds, show translated loading/error and an explicit retry; no automatic retry. This timeout stops waiting in the browser, not a guarantee of SDK/server cancellation. Never retain old metadata while a new selection loads. Show application response policy separately from untrusted Content-Type/ETag; neither proves file safety/integrity.

Abort ao fechar/selecionar novamente e recusa independente de resposta obsoleta. Limite de espera no navegador de 10 segundos, carga/erro/retry manual traduzidos, sem metadados antigos. Timeout não garante cancelar SDK/servidor. Separar política HTTP da aplicação de MIME/ETag não confiáveis.

Clipboard initially exposes exact bucket/key only. Read-only labelled textareas remain manually selectable even if Clipboard API is absent/denied; buttons are revealed only by functioning enhancement. Report copied only after writeText resolves, and retain failure feedback. No execCommand fallback, automatic copy, ARN construction, emulator/public/presigned URL is introduced. Those remain in the expanded first-delivery backlog with their own endpoint/partition/signing contracts.

Clipboard inicial apenas bucket/chave exatos, textareas rotuladas selecionáveis, botões ativados pela melhoria funcional e sucesso só após writeText resolvido. Falha permanece visível. Sem execCommand, cópia automática, ARN ou URL S3/assinada implícita; continuam no escopo ampliado com contratos próprios.

## Acceptance / Aceite

Control-character clarification: derive exact clipboard strings from the encoded canonical full-page URL rather than normalized HTML textareas. Show an explicitly percent-encoded manual representation for values with control characters; buttons still send the original string to the API. Test the CR/LF case locally. / Para controles, extrair string original da URL codificada; textarea manual mostra representação percent-encoded identificada. Botões enviam original à API; testar CR/LF localmente.

Require fresh Java/local-emulator and browser checks for escaping, HTTP fallback/errors, keyboard/focus, mobile/themes/languages, no-JS, close/reopen races, missing object, network/timeout/retry, clipboard success/denial and normal history/deep links. Fixtures simulating browser failures are not real permission, compatibility or security certification. Legal/name validation and full operational delivery remain pending.

Exigir evidência nova Java/emulador/navegador nos cenários acima. Simulações não certificam permissões/compatibilidade/segurança. Validação jurídica/do nome e entrega operacional completa ainda pendentes.

References / Referências: [htmx AJAX API](https://htmx.org/api/#ajax), [htmx events](https://htmx.org/events/#htmx:beforeSwap). Runtime version remains the bundled 2.0.8; test its actual behavior, not only current online documentation.
