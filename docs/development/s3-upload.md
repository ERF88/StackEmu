# Single-file upload enhancement / Melhoria de upload individual

## Contract / Contrato

[ADR-0016](../decisions/ADR-0016-single-file-upload-progress.md) owns UX-3. Keep the existing native multipart POST, required confirmation, CSRF, application file/request limits, original basename/extension/MIME, no-overwrite and safe temporary cleanup. Do not compute a custom destination key in the browser or read the file into an unbounded JavaScript buffer. Native file input remains visible; the optional drop surface/keyboard picker selects one file without submitting. Multiple files, directory entries/relative paths and text drops are rejected without replacing the previous selection. Unsupported browser assignment retains the native picker fallback.

Contrato na ADR vinculada: preservar POST, confirmação, CSRF, limites, arquivo original, não sobrescrita e temporários. Sem chave manual ou buffer arbitrário em JS. Input nativo visível, drop/seletor opcionais não enviam. Rejeitar múltiplos/pastas/texto sem substituir seleção; manter fallback nativo.

## State machine / Estados

Idle/selected → submitting indeterminate → browser-to-application transfer → processing → server-confirmed result or recoverable failure. A native progress element is labelled; its totals include multipart overhead, not just object bytes. Only correlated native xhr.upload samples forwarded through htmx:xhr:progress may update it; response progress cannot reset the bar. Valid computable samples are monotonic and capped at 100. Upload completion/100 means processing, not success. Live text changes by phase or ten-percent step. A 120-second client wait is an explicit UI policy, not a performance claim or server rollback guarantee.

Estados: seleção → envio indeterminado → transferência → processamento → resultado do servidor/falha. Progress nativo rotulado; totais incluem multipart. Somente eventos nativos de envio correlacionados ao encaminhamento htmx atualizam; resposta não reduz a barra. 100% não é sucesso, anúncios por fase/dez pontos. Espera cliente de 120 segundos é política, não desempenho/cancelamento.

Controls lock only after htmx captured FormData, so confirmation/CSRF/file fields are not omitted. Drop and duplicate submission are ignored while active. Unknown transport/framework outcomes restore controls, clear confirmation and retain a persistent instruction to check the listing before manual reconfirmation/retry. Request-size rejection has separate text. No browser auto-retry, resume or cancel promise. Navigation aborts waiting but may not stop a server write. Server validation replaces the form and requires file reselection as in native flow.

Bloquear controles após FormData capturado, sem omitir campos. Ignorar drop/envio duplicado durante requisição. Falhas incertas restauram controles, limpam confirmação e exigem consultar listagem antes de reconfirmar. Sem retry automático do navegador ou promessa de retomada/cancelamento. Navegação pode não parar escrita. Erro servidor recria formulário e exige reselecionar arquivo.

## Checked workspace / Workspace verificado

Only upload forms/results contain mutation-workspace. The existing checked full-page templates remain native fallback; X-StackEmu-Upload: workspace marks resource-generated POST HTML, not framework errors. Active request, target, content-type and known HTTP statuses gate htmx selection/outer swap of that wrapper. Preserve real validation/conflict/error status codes. No first-party innerHTML assignment, arbitrary HTML, inline/eval, CSS styles or script-tag execution. Server-localized title data updates the document title; focus moves to the first invalid field or result heading. Other bucket/folder mutation forms retain their native feedback behavior. No history push/re-POST is introduced.

Wrapper somente em upload; templates completos permanecem fallback. Marcador apenas em resposta POST do resource, com gates de XHR/alvo/tipo/status. Preservar códigos HTTP e nunca trocar HTML arbitrário/erros de framework. Sem innerHTML próprio, inline/eval/estilos/scripts. Título localizado seguro e foco em erro/título. Bucket/pasta continuam nativos, sem mudar histórico ou repetir POST.

## Evidence / Evidência

The real 1 MiB transport spike and focused/full verification are tracked in [UX-3 execution](../evidence/ux-3-results.md). Use local MiniStack only. Distinguish real uploads/hash/MIME/CSRF/limits from held-response, shortened-timeout and synthetic-progress fixtures. Native no-JS, responsive/themes/languages, keyboard/axe, error recovery and baseline integrity are required. Automated checks do not replace human assistive-technology review. Preview, deletion, clipboard URL/ARN, signing and copy/move remain separate first-delivery work; legal/name validation is pending.

Registro vinculado distingue upload real de simulações, exige fallback/idiomas/temas/teclado/axe/erros/baselines e não substitui revisão humana. Demais capacidades continuam no escopo com gates próprios; nome/jurídico pendentes.
