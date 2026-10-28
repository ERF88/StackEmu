# Platform interaction standard results / Resultados do padrão de interação da plataforma

Date / Data: 2026-10-27. Scope / Escopo: [ADR-0033](../decisions/ADR-0033-interaction-feedback.md) and [shared standard](../design/interaction-feedback.md).

## English

The current platform shares `beginInteractionFeedback` for action/review opening, inspector reads, listing refresh, native read navigation and clipboard. Local controls remain immediate; uploads/mutations retain immediate submission guards and actual progress. Downloads retain native attachment behavior without simulated completion. Fast operation preparation can reach its checked review before revealing a transient preparation window; slower preparation reveals real, named work at the common threshold. No form/token/file cache, new framework, emulator-health polling or mutation retries were introduced.

- Java 25/Maven verification against local MiniStack passed 186 tests, zero failures/errors/skips: `work/interaction-standard-maven.log`.
- Browser preflight selected 19 cases: 15 passed and four exposed Shift+Tab escaping the modal boundary. A shared modal boundary handler corrected the issue without weakening those assertions or trapping modified browser shortcuts. Original evidence: `work/interaction-standard-preflight.log`.
- Full browser regression passed 87 cases in 4.9 minutes: `work/interaction-standard-regression.log`. Includes current actions, preparation, stateful read navigation, stale/refused/unmarked responses, timeout without automatic retry, native fallbacks, actual clipboard/manual fallback, transfer/replacement/signing without bearer capture, text/raster preview, preferences, English/Portuguese and light/dark. Relevant cases exercise reduced motion, forced colors and 390/1366/1440 px; not every state/combination or assistive technology.
- After extending the indicator's styling to native links and preserving modified Tab shortcuts, the final UI package passed 10 overlapping targeted cases: `work/interaction-standard-final-browser.log`. These are repeats, not 97 distinct cases. Package verification passed with Java tests deliberately skipped in the UI-only packaging stage: `work/interaction-standard-release-build.log`.
- A held native Home navigation was observed from the page event: busy source, visible indicator, no modal, no animation under reduced motion, normal navigation and no residual busy state after Back. Confirmed probe: `work/interaction-standard-native-confirmed.log`. The first probe used a locator assertion that auto-waited for the intentionally held navigation; its timeout remains in `work/interaction-standard-native-probe.log` and is not presented as an application failure.
- Early frontend checks found a test's unqualified browser global and formatting drift; both were corrected. Final frontend checks are retained in `work/interaction-standard-delivery-checks.log`.

All execution used the host JVM/browser/tooling and local MiniStack; no new application image or container suite. Tests used owned fixtures and retained exact-target/CSRF/CSP/checked-fragment contracts. No claim of full HIG conformity, production performance, user research or naming-test proof of cohesion/security. New-image acceptance and legal validation of the StackEmu name remain pending.

## Português

A plataforma atual compartilha `beginInteractionFeedback` para ações/revisões, leituras do inspector, atualização de listagens, navegação nativa e clipboard. Controles locais continuam imediatos; uploads/mutações mantêm bloqueio imediato de duplicidade e progresso real. Downloads preservam anexo nativo sem conclusão simulada. Preparação rápida pode chegar à revisão verificada sem janela transitória; preparação lenta apresenta trabalho real/com alvo no limiar comum. Sem cache de formulário/token/arquivo, framework novo, polling de saúde ou retry de mutações.

- Java 25/Maven com MiniStack local: 186 testes aprovados, sem falhas/erros/skips; log indicado acima.
- Preflight: 19 casos, 15 aprovados e quatro identificando fuga de foco por Shift+Tab. Corrigida com limite modal compartilhado, sem enfraquecer asserções ou capturar atalhos modificados do navegador. Evidência inicial preservada.
- Regressão completa: 87 casos aprovados em 4,9 minutos. Abrange ações/preparação, navegação com estado, recusa/resposta obsoleta/sem marcador, timeout sem retry automático, fallback nativo, clipboard/cópia manual, transferência/substituição/assinatura sem capturar bearer, prévias texto/imagem, preferências, idiomas e temas. Casos relevantes exercitam movimento reduzido, cores forçadas e larguras indicadas, sem alegar todas as combinações ou tecnologias assistivas.
- Após ajuste visual dos links nativos e preservação dos atalhos Tab modificados, dez casos sobrepostos passaram no pacote final; não são 97 casos distintos. Empacotamento final aprovado com testes Java intencionalmente ignorados nessa etapa apenas de UI.
- Navegação Home retida observada por evento na própria página: acionador ocupado, indicador visível, sem modal/animação em movimento reduzido, navegação normal e sem estado ocupado residual ao voltar. Probe inicial usava asserção que aguardava a navegação retida; timeout preservado sem ser apresentado como defeito da aplicação.
- Checks iniciais encontraram global de navegador sem qualificação e formatação no teste, corrigidos. Checks finais no log indicado.

Execução somente JVM/navegador/ferramentas do host e MiniStack local; sem imagem da aplicação ou suíte em contêiner. Fixtures próprias, alvos exatos, CSRF/CSP e fragmentos verificados preservados. Sem alegar conformidade HIG integral, desempenho em produção, pesquisa ou prova de coesão/segurança por testes de nomes. Aceite em nova imagem e validação jurídica do nome StackEmu pendentes.
