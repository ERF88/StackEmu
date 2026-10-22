# ADR-0025 — Action windows / Janelas de ação

- Date / Data: 2026-10-21.
- Status / Estado: Accepted direction; verification and visual acceptance separate / Direção aceita; verificação e aceite visual separados.

## Decision / Decisão

The maintainer requested interactive creation/deletion windows, rejected generic confirmation checkboxes and explicitly authorized JavaScript. Apply apple-hig-designer through a named native modal for existing bucket/folder creation, single-file upload and exact-object deletion review. No new framework or service capability. Complete no-JavaScript parity is no longer a blocker for future UX work; existing canonical pages/forms are retained where practical.

O mantenedor pediu janelas interativas, rejeitou checkboxes genéricos e autorizou JavaScript. Aplicar a skill através de modal nativo rotulado para criação de bucket/pasta, upload individual e revisão de exclusão existentes. Sem framework/capacidade nova. Paridade completa sem JavaScript deixa de bloquear UX futura; páginas/formulários canônicos existentes continuam onde viável.

Supersede ADR-0020's Home “S3 · Not verified”, ADR-0024's listing-local density and the permanent local-only footer: move density, on-demand diagnostics and the trusted-local notice into Preferences. Keep diagnostics beside failed S3 reads. Home still has zero emulator I/O and makes no health claim. Relocating disclosure does not authorize public hosting.

Substituir indicador da Home, densidade na listagem e rodapé permanente: controles/diagnóstico/aviso ficam em Preferências, com diagnóstico junto às falhas. Home continua sem I/O nem alegação de saúde. Aviso relocacionado não autoriza exposição pública.

## Interaction and security / Interação e segurança

- Creation has fields and one named submit, no second confirmation. Server validation is authoritative; CLI examples become collapsed help. / Criação com campos/envio explícito, erros do servidor e CLI recolhida.
- Deletion fetches fresh read-only bucket/key/ETag review and a one-use ticket. Initial focus is Cancel, not Delete. Explicit Delete submission expresses consent; keep `confirm=yes` without a checkbox. A hidden parameter is not authorization: missing/invalid consent, CSRF, server-held target, ticket consumption, version/ETag gates remain independent. / Revisão fresca, foco em Cancelar e consentimento pelo envio; parâmetro compatível não substitui gates reais.
- Enhance only unmodified same-origin primary links to `/s3/write` and `/s3/delete-object`. Keep canonical URLs, native focus containment/inert background and idle Escape/Cancel with trigger focus restoration. No history rewriting, storage or polling. During POST block closing/duplicate submissions until response/timeout; leaving the document is not rollback. / Melhoria restrita com foco/cancelamento nativos, sem SPA; bloquear duplicidade durante POST sem prometer rollback.
- Checked Qute pages expose `#action-workspace` and `X-StackEmu-Action: workspace`. Only bundled htmx swaps that escaped selection after active-XHR, open-state, target, same-origin response-path, known-status, HTML-type and marker checks. Marked validation/failure pages keep real HTTP status. No first-party innerHTML, inline handlers, scripts or emulator HTML. / Wrapper/marcador Qute e gates antes do swap; erros mantêm status real, sem HTML arbitrário/scripts.
- Abort idle reads and independently reject stale responses. Never automatically retry mutations. Unmarked/network/timeout responses show uncertainty and disable the submitted action; inspect before a new manual attempt. Upload retains ADR-0016's checked multipart/progress handling: 100% sent is not success. / Recusar obsoletas; falha incerta sem repetição automática; upload mantém transporte/progresso próprios.
- Keep authoritative results visible. Closing confirmed success performs only a GET refresh/operational return, not another POST. / Resultado visível; fechar sucesso faz apenas GET contextual.

This narrowly supersedes checkbox/native-only/no-modal presentation in ADR-0007/0016/0021/0024, not CSP/CSRF, original-file handling, version/ETag, one-use tickets, bounded preview or read/mutation separation. No bucket/batch/prefix deletion, copy/move or signed URLs are fabricated.

Substitui apenas apresentação de consentimento/modal/fallback, preservando contratos de segurança. Sem inventar operações ainda pendentes.

## Verification and remaining gates / Verificação e gates pendentes

Require local Java 25/Maven, style/translation/docs and host-browser checks for languages/themes, 390/1440px, keyboard/focus, cancel without mutation, real create/upload/delete, exact targets/neighbors, validation/duplicate/failure/stale responses and contextual returns. Record actual results separately; historical evidence stays unchanged. Docker remains MiniStack-only. Human visual/assistive-technology review, final Linux/new-image validation and StackEmu legal/name validation remain pending. No research result, HIG/WCAG certification or release approval is implied.

Exigir checks locais e matriz de interação/segurança; resultados novos separados do histórico. Docker só MiniStack. Aceite humano, Linux/imagem nova e nome/jurídico pendentes, sem pesquisa/certificação/release implícitos.
