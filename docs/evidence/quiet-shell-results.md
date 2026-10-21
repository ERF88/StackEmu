# Quiet operational shell / Shell operacional simplificado

Date / Data: 2026-10-20. Local uncommitted tree, not a release / Árvore local sem commit, não release.

## Delivered slice / Fatia entregue

Language and theme now live in one initially closed native Preferences disclosure. Density controls move to S3 bucket/object listings; URL preferences still survive native links, forms, detail and preview. Repeated brand/workspace captions and the duplicate sidebar warning are removed; the footer retains the local-only warning. Header minimum is 64px, with controls at least 44px. Panels expand in flow, without ARIA menu roles, modal focus trapping, new JavaScript, cookies or storage. Home still performs no emulator reads.

Idioma/tema ficam em Preferências nativas, inicialmente fechadas. Densidade nas listagens S3, mantendo preferências por URL em links/formulários/detalhes/prévia. Subtítulos repetidos e aviso duplicado removidos; aviso local único no rodapé. Header mínimo 64px e controles de ao menos 44px; painéis no fluxo, sem menu ARIA, foco modal, JS/storage/cookies novos. Home sem leituras do emulador.

Bucket rows expose real Browse objects / Upload / Create folder form links through Actions. Object rows keep direct attachment download and offer full Details and the existing bounded text preview through Actions. Exact opaque targets/preferences are encoded in the web layer; no extra metadata SDK calls are introduced per row. Opening a form or disclosure does not mutate a resource. Native fallback and CSP/CSRF/original-file contracts remain unchanged.

Buckets oferecem navegação/envio/formulário de pasta em Ações; objetos mantêm download direto e oferecem Detalhes completos/Prévia de texto existente. Alvos/preferências codificados na apresentação, sem metadados adicionais por linha; abrir formulário/disclosure não faz mutação. Fallback/CSP/CSRF/arquivo original preservados.

This is **not full operational S3 completion**: object/prefix/bucket deletion/emptying, copy/move and presigned URLs still require their dedicated technical contracts and implementation. No fictitious or disabled action button represents those capabilities. [Scope and next gates](../product/s3-management-scope.md), [decision amendment](../decisions/ADR-0020-service-catalog-navigation.md).

Esta fatia **não conclui o gerenciamento operacional**. Exclusão/esvaziamento, cópia/movimentação e URLs assinadas continuam exigindo contratos e implementação; sem botões fictícios representando essas capacidades.

## Local verification / Verificação local

Environment: Windows/amd64, Java 25.0.4.1, Maven 3.9.9, build-only Node 24.10.0, Playwright 1.64.1/host Chrome 155.0.8059.80, local MiniStack 1.5.25 at loopback 4566. GUI runs on the host JVM, never a GUI container during this iteration. / Ambiente local, sem recursos de produção ou contêiner da GUI.

- First Maven verification: **111 tests, one failure**, because the old pagination test counted every object-details link and therefore counted the new action link as a second object entry. The selector now counts actual `data-object-detail` entries; five unique keys, page-size limit and three-page termination assertions remain. No timeout/tolerance/assertion was removed. Log: `work/quiet-shell-maven.log`. / Falha inicial do seletor corrigida sem enfraquecer paginação.
- After correction: **111 Java tests passed**, zero failures/errors/skips, full `mvn verify` BUILD SUCCESS at 11:03:04 -03:00. Includes checked Qute, architecture/translations, native HTTP/security, actual local S3 operations and encoded contextual-action link coverage. Log: `work/quiet-shell-maven-final.log`. / 111 testes aprovados após correção.
- Six new focused host-browser scenarios passed (26.9s), covering language/theme combinations, native keyboard disclosures, axe/overflow checks at 390/1440, exact bucket upload target, existing text preview, and no-JS preferences/folder-form navigation. Log/report: `work/quiet-shell-focused.log`, `work/quiet-shell-focused-report.json`. Do not add these repeats to later full-suite totals. / Seis cenários novos aprovados, sem inflar total posterior com repetições.
- Initial broader run found the unavailable-page test's old `#bucket-list a` selector also matching contextual density links. It now selects the direct retry action (`#bucket-list > a`), retaining its exact target check. The completed first broad run had **81 passed / 13 failed** (6.7m): four retry-selector failures and nine real mobile page-overflow failures in density, clipboard-motion and full S3 workflow checks. The overflow was traced with actual Qute fixtures to absolutely positioned accessible labels in the horizontally scrollable action column. A relative positioning context on `.resource-actions` contains those labels; root width in the diagnostic returned from 725px to 390px, without hiding overflow or changing assertions/tolerances. Logs/report: `work/quiet-shell-browser.log`, `work/quiet-shell-browser-first-report.json`. / Primeira rodada: 81 passaram/13 falharam; quatro seletores e nove overflows reais. Contexto relativo corrige rótulos acessíveis sem mascarar overflow, mantendo verificações.
- Initial `npm run check`, local smoke and PowerShell analysis passed. Style logs: `work/quiet-shell-style-first.log`. / Checks iniciais aprovados.

## Representative visual review / Revisão visual representativa

Agent inspected focused-run pt-BR dark Home at 390/1440 and pt-BR light expanded Preferences at 1440. Controls are now collapsed by default. Inspection exposed an oversized blank mobile navigation row caused by the minimum-height grid distributing free height into both automatic rows; the mobile grid now uses an automatic navigation row and a flexible workspace row. Fresh package/browser verification after this refinement is recorded below. These are agent inspections, not maintainer acceptance, user research or human assistive-technology certification.

Agente revisou capturas pt-BR de Home mobile/desktop e Preferências abertas. Revisão identificou espaço vazio excessivo no topo mobile; grid agora mantém navegação em linha automática e espaço restante na área de trabalho. A validação nova após o ajuste está registrada abaixo. Não equivale a aceite humano, pesquisa ou certificação de acessibilidade.

## Deletion readiness spike — not an enabled capability / Spike de preparação de exclusão

Official [AWS conditional-delete guidance](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-deletes.html) describes ETag preconditions for rejecting changes to the selected object; the [Java SDK builder documentation](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/s3/model/DeleteObjectRequest.Builder.html) documents `ifMatch`. Consulted 2026-10-20; latest API documentation does not prove the resolved SDK/emulator flow works.

Referências oficiais descrevem precondição por ETag e builder Java; documentação mais recente não comprova o fluxo real da versão resolvida/emulador.

Ran `tools/spikes/delete-conditions.mjs` on the host, targeting hardcoded `http://127.0.0.1:4566`, with redirect refusal/3s request timeouts and UUID-owned fixtures only. The raw HTTP spike first checked conditions, then added Enabled/Suspended round-trips in a separate empty bucket that never receives object versions. Logs: `work/delete-conditions-spike.json`, `work/delete-conditions-and-versioning-spike.json`; final formatted-source repeat: `work/delete-readiness-final.json` (exit 0, same outcomes).

Spike HTTP executado só no loopback, com fixtures UUID próprias e timeouts/sem redirects. Segundo teste acrescentou estados de versionamento em bucket vazio separado, sem criar versões de objetos.

| Observed check / Verificação observada                              | Result / Resultado                                                                                |
| ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| Wrong ETag / ETag incorreto                                         | 412; original object retained / objeto original preservado                                        |
| Stale ETag after replacement / ETag antigo após troca               | 412; replacement retained / substituto preservado                                                 |
| Current ETag / ETag atual                                           | 204; exact object absent afterward / objeto exato ausente depois                                  |
| Separate guard object / Objeto sentinela                            | Retained unchanged / preservado sem alteração                                                     |
| Enabled and Suspended updates/reads / Escritas e leituras de estado | 200; corresponding state reported / estado correspondente informado                               |
| Cleanup / Limpeza                                                   | Exact two keys and both owned buckets returned 204 / dois objetos e dois buckets próprios com 204 |

The initial fresh bucket returned 200 without a reported Status; the spike records `not-reported` rather than asserting that every empty/unrecognized response proves unversioned safety. Read-only `javap` inspection also confirmed `DeleteObjectRequest.Builder.ifMatch(String)` in the resolved SDK **2.54.13**; method availability is not authenticated mutation execution. This is raw local protocol evidence, **not SDK-authenticated mutation integration**, atomicity/version-race proof, version-history/delete-marker handling, CSRF/confirmation/permission/error coverage or a validated GUI. No delete route/button is activated. Next work must accept a dedicated single-object deletion contract and prove fail-closed version handling, exact target/current condition, confirmation/CSRF and failure mapping through the real adapters before exposing the action.

Bucket novo respondeu 200 sem Status; registro não transforma toda resposta vazia/desconhecida em prova de segurança. Inspeção javap confirmou ifMatch no SDK 2.54.13, sem executar mutação autenticada por ele. Evidência HTTP local **não valida mutação via SDK**, corrida de versionamento, histórico/markers, confirmação/CSRF/permissões/erros ou GUI. Nenhuma rota/botão de exclusão ativada. Próximo trabalho exige contrato técnico aceito e validação completa nos adapters antes de expor a ação.

## Final local results / Resultados locais finais

- Fresh `mvn verify` after both layout fixes: **111 tests, zero failures/errors/skips**, BUILD SUCCESS **11:17:31 -03:00**, 2:06. Log: `work/quiet-shell-maven-reviewed.log`. / Verificação Java nova após ambos os ajustes aprovada.
- Final complete selected host-browser run: **94/94 passed**, zero failed/skipped/flaky, 5.6m, starting **14:19:28.422Z**. Log/report: `work/quiet-shell-browser-final.log`, `work/quiet-shell-browser-final-report.json`. This is the host suite, not pinned-Linux baseline or held-upload-proxy testing. / 94 cenários locais aprovados, não suíte Linux/imagem.
- An additional repeat of the six new scenarios passed **6/6 (31.9s)** after adding explicit viewport reachability assertions and captures for horizontally scrolled row actions. This is not six additional unique tests. Log: `work/quiet-shell-visible-actions.log`. / Repetição com assertions de alcance visual aprovada; não somar ao total único.
- Agent inspected final pt-BR dark mobile Home, pt-BR light desktop Home, pt-BR dark desktop bucket Actions, English light desktop object Actions and pt-BR dark mobile Actions after table-local scrolling. Navigation no longer absorbs spare mobile height; action labels no longer escape the scroll container. Table-local horizontal scrolling remains intentional, with reachable 44px controls and no page overflow in the checked cases. / Inspeções representativas confirmam correções, sem substituir aceite humano.
- Local smoke and PowerShell analysis passed against the reviewed fast-jar; `npm run check` passed after code/docs updates and is rerun after final documentation-only closing edits. All **80 Linux PNG hashes remained identical** to the V6 reference copy; no versioned baseline was updated. / Smoke/análise/checks aprovados; 80 hashes Linux preservados, última edição documental recebe check final.
- GUI remains on host Java, loopback 8080 (owned PID **26220**), local MiniStack on 4566; only MiniStack runs in Docker. / GUI na JVM local, só emulador no Docker.

## Remaining gates / Gates restantes

Pinned-Linux baseline reconciliation/new-image testing remains the separate final stage; no baselines, new application image, container suite, image scan, commit, push or release is performed here. Maintainer visual validation, human accessibility, independent onboarding and legal/name validation remain pending. / Imagem/Linux final e gates humanos/legais/release permanecem separados. Sem publicação ou contêiner de aplicação nesta iteração.
