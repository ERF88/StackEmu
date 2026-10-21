# ADR-0017 — operation notifications / Notificações de operações

- Date / Data: 2026-10-15.
- Status: Accepted implementation contract; fresh verification required / Contrato aceito; exige verificação nova.

## Decision / Decisão

UX-4 supplements server-confirmed bucket/folder/upload success pages with one dismissible notice. The checked result heading supplies escaped localized text; never infer success from a click, progress, arbitrary response, URL parameter or storage. Native results remain readable without JavaScript. The notice stays in normal document flow, end-aligned on desktop and full width on mobile, rather than covering controls. No timer, animation, queue, cookie or cross-navigation persistence: interaction never races an expiry deadline.

UX-4 complementa resultados confirmados de bucket/pasta/upload com um aviso dispensável. Texto escapado/localizado vem do título verificado, nunca de clique, progresso, resposta arbitrária, query ou storage. Resultado nativo permanece sem JS. Aviso no fluxo, alinhado à direita no desktop e largura disponível no mobile, sem cobrir controles. Sem timer/animação/fila/cookie/persistência entre páginas: interação não corre contra expiração.

Native result navigation uses one polite live message, without automatic focus. Upload replacement already focuses the result heading under ADR-0016; its supplementary notice is not live, avoiding a second success announcement. Dismissal leaves result/target/link/CLI intact and returns focus to the result heading only when focus was in the notice. Page restoration does not replay announcements. Clipboard keeps its modal-local status after the actual promise; no duplicate global toast. Validation, transport errors and processing remain in their persistent original locations, not a toast-only error channel.

Navegação nativa usa mensagem polite sem autofoco. Swap de upload já foca título pela ADR-0016: aviso complementar sem live para evitar duplicidade. Fechar mantém resultado/alvo/link/CLI e devolve foco ao título somente se estava no aviso. Restauração não repete anúncios. Clipboard mantém status local após promessa real, sem toast global duplicado. Erros/processamento continuam nos locais persistentes.

## Confirmation contract / Contrato de confirmação

No destructive capability or modal is enabled here. Its accepted capability ADR must first define version state, exact target, stale selection, collision/partial failure, bounded work and retry semantics. Only then enhance equivalent server confirmation with native dialog: named operation, verbatim escaped bucket/key, irreversibility, initial cancel focus, safe Escape/cancel/return focus and explicit submit. Native confirmation/CSRF and server validation remain authoritative; a modal is not authorization. Unsupported dialog retains the native page. Never copy illustrative hx-delete routes or window.confirm.

Sem capacidade destrutiva/modal nesta fatia. ADR da capacidade deve definir versões, alvo, seleção obsoleta, colisão/falhas parciais, limites e retry antes. Só então melhorar confirmação servidor com dialog nativo: operação/alvo escapados, irreversibilidade, foco inicial cancelar, Escape/retorno seguros e envio explícito. Confirmação/CSRF/validação servidor permanecem autoritativos; modal não autoriza. Sem API dialog manter página nativa; sem copiar rotas ilustrativas/window.confirm.

## Verification and limits / Verificação e limites

Require fresh Java 25/local MiniStack, real native/enhanced operations, no-JS, languages/themes, keyboard dismissal, focus/live-region assertions, responsive/axe checks and baseline integrity. Retention policy is indefinite, not a screen-reader announcement guarantee. Human assistive-technology acceptance, legal/name validation and publication remain pending. Evidence: [UX-4 execution](../evidence/ux-4-results.md).

Exigir evidência nova Java/MiniStack, operações reais, sem JS, idiomas/temas, teclado, foco/live, responsividade/axe e referências. Retenção indefinida não comprova anúncio por leitor de tela. Aceite humano, nome/jurídico e publicação pendentes.
