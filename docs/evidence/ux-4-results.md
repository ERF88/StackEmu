# UX-4 execution / Execução UX-4

Date / Data: 2026-10-15. Contract / Contrato: [ADR-0017](../decisions/ADR-0017-operation-notifications.md).

## Scope / Escopo

One in-flow, dismissible, indefinitely retained notice supplements real server-confirmed bucket/folder/upload result pages. Native navigation uses polite status without focus stealing; enhanced upload relies on its focused heading, with no duplicate live message. No-JS retains authoritative result/target/link/CLI. Clipboard/errors keep existing local feedback. No new mutation, modal, dependency or weakened CSP/CSRF. Destructive-dialog requirements are documented only; deletion and other operational capabilities remain separate first-delivery work.

Aviso único no fluxo, dispensável e sem expiração complementa resultado real de bucket/pasta/upload. Navegação nativa usa status polite sem roubar foco; upload usa título focado sem live duplicado. Sem JS mantém resultado/alvo/link/CLI. Clipboard/erros mantêm feedback local. Sem nova mutação/modal/dependência ou enfraquecer CSP/CSRF. Contrato destrutivo é só documentado, não implementado.

## Verification / Verificação

Fresh Java 25 Maven verification against local MiniStack passed: 68 tests, zero failures/errors/skips; checked templates and translation parity passed. The final GUI image built and passed container non-root/read-only/capability/temp/memory/loopback/license checks, smoke and native write/download/SHA-256/CSRF scripts. No application/SDK contract, route or dependency was added.

Nova verificação Maven Java 25/MiniStack: 68 testes, zero falhas/erros/skips, templates e traduções aprovados. Imagem final construída; checks de container/licenças, smoke e escrita/download/SHA-256/CSRF nativos passaram. Sem nova rota, dependência ou contrato aplicação/SDK.

Focused browser verification passed 15 tests: five notification cases plus ten upload/transport regressions. Real native bucket/folder success and enhanced upload retain authoritative results, with polite status only on native navigation and focused-heading-only announcement semantics for upload. Dismissal by keyboard returns focus without removing target/link/CLI. No-JS success remains readable with the optional notice hidden. Processing, real 422/409/CSRF400 and controlled transport/body-limit failures do not show success. Clipboard regressions remain part of the full suite. Simulated page-restoration events are lifecycle fixtures, not proof of actual bfcache or screen-reader speech.

Navegador focado: 15 testes aprovados, cinco de notificações e dez de upload/transporte. Operações reais preservam resultado, polite na navegação nativa e título focado sem live duplicado no upload. Fechar por teclado retorna foco sem remover alvo/link/CLI. Sem JS mantém resultado e aviso opcional oculto. Processamento, erros reais 422/409/CSRF400 e falhas controladas não exibem sucesso. Eventos simulados de restauração não certificam bfcache/anúncio real; clipboard permanece na suíte completa.

All eight notification screenshots were agent-reviewed at 390/1440 pixels, both languages/themes: text remains readable and controls unobscured, with no horizontal overflow detected. Axe reported no violations in those viewports. Prettier, Markdown (93 files), CSS/JavaScript lint, two contrast tests/44 palette pairs, PowerShell analysis, both Dockerfiles/Hadolint, actionlint, Compose and diff whitespace checks passed. Initial lint found three unqualified browser globals in the test; corrected with window qualification, without weakening rules.

O agente revisou oito capturas em 390/1440px/idiomas/temas: textos legíveis, controles sem sobreposição e nenhum overflow horizontal detectado. Axe sem violações nesses viewports. Passaram Prettier, Markdown (93 arquivos), CSS/JS, dois testes/44 pares de contraste, PowerShell, Hadolint, actionlint, Compose e whitespace. Lint inicial apontou três globais do navegador no teste, corrigidos com window sem relaxar regras.

Full pinned Linux Chromium regression passed: 112 tests, zero failures/skips/flaky results, including 80 screenshot comparisons. The 80 baseline PNGs were not updated; SHA-256 over ascending filenames concatenated with bytes remained `8b6aabf658292b9deed76a9254498ce31bf046785028b84ed5b58558e78550a3`. Local ignored evidence is under work/browser-report.json and work/browser-results/, not a GitHub CI run or published release.

Regressão completa Chromium/Linux fixado: 112 testes aprovados, zero falhas/skips/flaky, incluindo 80 comparações visuais. As 80 referências PNG não mudaram; hash agregado acima preservado. Evidência local ignorada em work/browser-report.json e work/browser-results/, não execução CI no GitHub nem release publicado.

## Reproduction / Reprodução

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'
docker compose up -d --build gui
docker run --rm --ipc=host --memory=768m -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node scripts/run-browser-local.mjs notifications.spec.mjs
```

Omit the spec for full regression. Do not update baselines automatically. / Omitir spec para regressão completa; não atualizar referências automaticamente.

## Remaining acceptance / Aceite restante

No human assistive-technology approval, complete accessibility/security certification, new vulnerability scan, performance benchmark, production compatibility, release/publication or legal/name validation is claimed. Retention is indefinite by design; no pause timer is needed. Preview, deletion, signing and copy/move remain pending documented gates.

Sem aceite humano/certificação/scan novo/benchmark/produção/release/publicação/validação jurídica do nome. Retenção indefinida dispensa timer de pausa. Preview, exclusão, assinatura e cópia/movimentação continuam pendentes.
