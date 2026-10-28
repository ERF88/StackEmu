# ADR-0034 — Compact notifications / Notificações compactas

- Date / Data: 2026-10-27.
- Status / Estado: maintainer-authorized UI refinement / refinamento de UI autorizado pelo mantenedor.

The maintainer requests smaller notifications and automatic dismissal as a platform-wide standard. This supersedes the manual-only success-notice rule in ADR-0033's interaction guide and preceding UI guidance; historical evidence remains unchanged. Use the existing shared notice controller, at most 24rem wide within the viewport, compact spacing and a named close icon with a 44px target.

O mantenedor solicita notificações menores e fechamento automático como padrão global. Substitui a regra de sucesso somente manual do guia da ADR-0033 e diretrizes anteriores, preservando evidência histórica. Usar controlador comum, largura máxima de 24rem limitada à viewport, espaçamento compacto e ícone de fechar nomeado com alvo de 44px.

Confirmed success expires after five seconds of visible, unattended time. Pause while hovered, focused or the document is hidden; resume the remaining time. A replacement notice gets a fresh deadline and cancels the previous timer. A pending operation does not expire; a confirmed transition to success starts its timer. Partial, failed, interrupted or uncertain outcomes remain explicitly dismissible. Field validation and durable records never disappear with the toast. Automatic dismissal never moves focus or cancels work; manual dismissal returns focus only when it was in the notice. Reduced motion remains respected, with no exit animation required.

Sucesso confirmado fecha após cinco segundos de tempo visível sem interação. Pausar com mouse/foco ou documento oculto; retomar tempo restante. Aviso novo reinicia prazo e cancela timer anterior. Operação pendente não expira; transição confirmada para sucesso inicia prazo. Resultados parciais, falhos, interrompidos ou incertos permanecem dispensáveis manualmente. Validação de campo e registros persistentes não desaparecem com o aviso. Fechamento automático não move foco nem cancela trabalho; fechamento manual devolve foco somente se estava no aviso. Preservar movimento reduzido, sem exigir animação de saída.

## Local validation / Validação local

Java 25/Maven package verification passed (`work/compact-notices-release-build.log`); Java tests were deliberately skipped for this UI-only change, not reported as a fresh full suite. Nineteen browser cases passed in 50.9 s (`work/compact-notices-final-browser.log`), covering shared notice timing with a virtual clock, hover/focus pause, simulated document visibility, replacement, persistent attention states, terminal success and stale deferred announcements, plus real create/delete/upload/copy/move/share flows. Relevant cases cover English/Portuguese, light/dark and 390/1366/1440 px. Compact captures were inspected; screenshot baselines were not updated. An initial test lint failure from unqualified browser events was corrected; final frontend checks passed (`work/compact-notices-final-checks.log`).

Empacotamento Java 25/Maven aprovado; testes Java ignorados intencionalmente nesta alteração somente de UI, sem alegar nova suíte completa. Dezenove casos de navegador passaram em 50,9 s: tempo com relógio virtual, pausas por mouse/foco, visibilidade simulada, substituição, alertas persistentes, conclusão e anúncios obsoletos; também fluxos reais de criação/exclusão/upload/cópia/movimentação/compartilhamento. Casos relevantes em idiomas/temas e larguras indicados. Capturas compactas inspecionadas, sem atualizar baselines. Lint inicial de eventos do navegador sem qualificação corrigido; checks frontend finais aprovados.

Host JVM/browser/local tooling and MiniStack only; no new application image/container suite, production benchmark, full HIG certification or legal-name validation claim.

Somente JVM/navegador/ferramentas locais e MiniStack; sem nova imagem/suíte em contêiner, benchmark de produção, certificação HIG integral ou alegação de validação jurídica do nome.
