# ADR-0016 — single-file drop and transport progress / Drop individual e progresso

- Date / Data: 2026-10-15.
- Status: Accepted implementation contract after real local transport spike; full acceptance requires fresh evidence / Contrato aceito após spike real local; aceite completo exige evidência nova.

## Boundaries / Limites

2026-10-21: [ADR-0025](ADR-0025-action-dialogs.md) allows this form inside an action window and replaces the checkbox with explicit submission. Existing multipart/progress/CSRF/original-file/no-retry contracts remain. Complete no-JS parity is no longer a UX blocker; the existing native form remains available. / Formulário pode abrir em janela, sem checkbox; demais contratos preservados e JavaScript autorizado.

Enhance only the existing single-file upload form. Keep native file input/keyboard/touch picker, multipart POST route, explicit required confirmation, CSRF, server-generated temporaries, no-overwrite, basename/extension/MIME policy and separate read/write ports. Drop selects exactly one regular file and never submits. Reject multi-file, directory and text drops without replacing a prior selection. During an active upload prevent selection/confirmation changes and duplicate submits. No multi-upload, resume, cancel guarantee, preview, signing or deletion is added.

Melhorar só upload individual existente, preservando input nativo/teclado/toque, POST multipart, confirmação, CSRF, temporários seguros, não sobrescrita e nome/extensão/MIME. Drop seleciona um arquivo comum sem enviar; rejeitar múltiplos/pastas/texto sem alterar seleção anterior. Bloquear alterações/duplicidade enquanto envia. Sem novas capacidades além desta fatia.

## Transport and rendering / Transporte e renderização

Use bundled htmx 2.0.8 with multipart encoding and local external JavaScript, without eval/inline handlers/inline styles. The real local spike `upload-transport.spec.mjs` verifies that native xhr.upload progress listeners registered during beforeRequest precede htmx forwarding, whereas response progress has the same htmx event name. Consume only a correlated one-shot native-upload sample in htmx:xhr:progress; ignore response/stale/synthetic uncorrelated samples. Do not infer direction from byte totals alone.

Usar versão empacotada e JS local sem eval/inline. Spike real comprova ordem dos eventos nativos de envio e encaminhamento htmx. Consumir só amostra de upload correlacionada uma vez; ignorar resposta, eventos obsoletos ou sem correlação. Não deduzir direção só pelos totais.

A labelled native progress element is indeterminate until loaded/total are finite, nonnegative and computable with total greater than zero. Determinate values are monotonic per request, bounded 0–100. Multipart totals include framing, not only file bytes. At 100% sent or upload load show processing; only a server-rendered successful result confirms completion. Announce phases and ten-percent steps, not every raw progress event. Client wait is bounded to 120 seconds, a chosen UI timeout, not capacity evidence or SDK cancellation. No automatic retry; unknown outcome instructs checking the listing before explicit reconfirmation/retry.

Progress rotulado começa indeterminado, aceita totais válidos e mantém 0–100 monotônico. Totais incluem multipart. 100% vira processamento, não sucesso; confirmação apenas por resultado do servidor. Anúncios por fase/dez pontos. Timeout escolhido de 120 segundos não é prova de capacidade/cancelamento SDK; falha incerta exige consultar listagem e reconfirmar, sem retry automático.

Reuse checked full-page form/result templates for both transports. Their stable mutation-workspace wrapper is selected by htmx only after active-XHR/target/status/content-type and explicit X-StackEmu-Upload: workspace checks. Allow known server validation/conflict/error pages to retain real HTTP status while swapping their escaped workspace. No first-party innerHTML assignment or emulator-body rendering; htmx script tags stay disabled. Framework CSRF/body-limit and unmarked/network/timeout failures do not swap arbitrary HTML; keep the form, restore controls, clear confirmation and show persistent localized recovery text. Never replay the mutation to obtain a result page.

Reutilizar templates verificados completos, selecionando wrapper estável somente após checar XHR/alvo/status/tipo/marcador. Páginas de validação/conflito preservam status real e workspace escapado. Sem innerHTML próprio ou corpo do emulador. Falhas de framework/não marcadas/rede/timeout mantêm formulário e recuperação persistente, restaurando controles e limpando confirmação. Não repetir mutação para obter resultado.

Enhancement keeps the original GET form URL/history; refresh is a fresh GET, not an automatic re-POST. Move focus to the first invalid field or result heading after a workspace replacement; reselect files after server validation as before. Page navigation/abort may leave an unknown server outcome, never claim rollback. Native no-JavaScript/full-page navigation remains authoritative when enhancement is unavailable.

Melhoria mantém URL GET/histórico, sem re-POST automático ao atualizar. Após swap focar campo inválido ou título; reselecionar arquivo após erro servidor. Navegação/abort pode deixar resultado incerto, sem prometer rollback. Fallback nativo permanece.

## Evidence and unknowns / Evidência e incógnitas

Fresh Java 25/local MiniStack, browser native/no-JS, drop/picker, transfer-vs-processing, duplicate/error/limit/CSRF/MIME/hash/focus/theme/language checks are required. Controlled network/timeout/synthetic progress fixtures must be labelled, not called production evidence. No throughput benchmark, full accessibility/security certification, publication or legal/name validation follows from this ADR.

Exigir evidência nova nos cenários acima, distinguindo simulações de rede/timeout/progresso e testes reais. Sem benchmark, certificação, publicação ou validação jurídica/do nome implícitos.

References / Referências: [htmx events](https://htmx.org/events/), [multipart encoding](https://htmx.org/attributes/hx-encoding/). Test the pinned implementation; illustrative scripts are not copied into runtime.
