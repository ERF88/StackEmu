# Action windows: local checkpoint / Janelas de ação: checkpoint local

Date / Data: 2026-10-21. Contract / Contrato: [ADR-0025](../decisions/ADR-0025-action-dialogs.md).

## Source changes / Alterações de fonte

Apply the maintainer-requested apple-hig-designer clarity/content-first principles: remove routine connection status and the permanent footer, move density/diagnostics/local-use disclosure into Preferences, collapse optional CLI help and use a shared native action window for existing creation/upload/deletion flows. Explicit named submits replace generic checkboxes; server validation, CSRF, original-file handling and deletion tickets/version/ETag checks remain. No runtime dependency or new S3 capability.

Aplicar clareza/conteúdo primeiro da skill: remover status/rodapé repetitivos, mover controles secundários para Preferências, recolher CLI e compartilhar janela de ação. Envios explícitos substituem checkboxes, mantendo validação e segurança reais. Sem dependência/capacidade nova.

## Verification so far / Verificação até este checkpoint

- Direct host Node invocations passed Prettier, markdownlint, document-index/link checks (120 documents, 580 local links at that checkpoint), Stylelint, ESLint and the existing palette contrast script. These are source/tool checks, not browser, HIG/WCAG or security certification. / Ferramentas diretas aprovadas; contagens anteriores à inclusão deste registro, sem certificar UI/segurança.
- Final direct repeats passed whole-checkout Prettier, Markdown, 121-document/589-link validation, CSS/JavaScript lint and palette contrast. Seven checker unit tests passed with Node's `--test-isolation=none`. Earlier 20-second format/test/Git attempts timed out; the longer format repeat and non-isolated checker repeat are separate completed results, not a clean `npm run check` execution. An earlier `git diff --check` completed successfully, while its final short-bound repeat timed out. / Repetições diretas aprovadas, com sete testes dos verificadores; registrar timeouts anteriores sem tratá-los como sucesso ou execução consolidada do npm.
- Playwright discovered five new action-window cases. Discovery is not execution. Existing tests were updated for relocated controls, explicit consent and collapsed help; they still require fresh browser execution. / Cinco casos novos descobertos, não executados; testes anteriores ajustados exigem nova execução.
- A one-case preflight was subsequently attempted and failed in `beforeEach` (58ms) with `apiRequestContext.put: connect EACCES 127.0.0.1:4566`, before browser/UI assertions. The other four new cases were not run. Error context and trace: `work/action-dialogs-blocked-preflight/`. No visual acceptance is claimed. / Preflight de um caso bloqueado no setup; quatro casos novos não executados e nenhuma validação visual concluída.
- Initial Java 25/Maven integration `verify` reached test startup but was interrupted without a final result. Do not reuse older 125-test evidence for this change. `work/action-dialogs-maven.log` records that attempt. / Tentativa inicial interrompida sem resultado final; não reaproveitar contagem histórica.
- A subsequent package attempt with tests skipped failed at fast-jar copying because a running process locks `target/quarkus-app/lib/boot/io.smallrye.common.smallrye-common-os-2.19.0.jar`. This is not BUILD SUCCESS. The existing GUI was not deliberately stopped because process inspection is denied. / Empacotamento falhou por JAR aberto, sem alegar sucesso ou encerrar processo não verificado.
- The fixed Tailwind CLI was invoked directly to rebuild generated CSS; authored/generated ownership and existing local license files remain unchanged. / CLI fixada recompilou CSS gerado, sem editar saída manualmente.
- Java 25/Maven packaging then passed with tests explicitly skipped and `quarkus.package.output-directory` pointing to the separate workspace directory `target/action-preview` (31.503s). Quarkus augmentation/checked templates completed. This avoids the locked running GUI; it is not an integration pass or a claim that the open GUI was updated. / Empacotamento separado aprovado, com testes pulados; valida templates, não integrações nem atualização da instância aberta.
- A separate selected Java test run passed 31 tests, zero failures/errors/skips (1m07s): architecture boundaries, translation parity, file content types, local icons, deletion ticket store, CLI quoting, download delivery, view mapping, URL preferences and asset fingerprints. These selected non-network tests are not the complete Maven/integration suite and do not prove UI behavior, cohesion or security certification. / Seleção Java de 31 testes aprovada; não substitui suíte completa, testes funcionais das janelas ou revisão humana.
- An isolated `ServiceCatalogTest` attempt failed during HTTP fixture startup with `Unable to establish loopback connection` / `Permission denied: connect`: three reported tests, one setup error and two skipped (1m04s). No Home/S3 behavioral assertion passed in that attempt; it is not a product regression diagnosis or an integration pass. / Tentativa isolada bloqueada antes das verificações funcionais por conexão local recusada; um erro de setup e dois ignorados, sem alegar aprovação.
- The execution environment denied Docker API access and loopback requests to ports 8080/4566, including after a network-permission grant. These are environment errors, not evidence of an emulator outage. / Ambiente recusou Docker e conexões locais; não inferir emulador parado.

## Remaining acceptance / Aceite pendente

### Resumption on 2026-10-21 / Retomada

After a session network grant, fresh terminal processes obtained HTTP 200 from both loopback endpoints, including a fresh Node process. The persistent auxiliary process still reported EACCES; that older process is not evidence that all fresh commands are blocked. The live Home still lacked `id="action-dialog"`, confirming it was not the new revision. HTTP reachability alone does not validate S3 operations.

Após permissão de rede, processos novos no terminal obtiveram HTTP 200 nos dois endpoints, inclusive Node novo; o auxiliar persistente continuou com EACCES. A Home aberta ainda não tinha o marcador da janela nova. Conectividade HTTP não comprova operações S3.

A fresh full local Maven `verify` attempt used `STACKEMU_INTEGRATION=true` and a separate `target/action-validated` output directory. It installed dependencies, generated CSS and reached test startup, but was interrupted without a completed test summary after startup made no observable progress. Log: `work/action-dialogs-resumed-maven.log`. No new passing integration count is claimed. Starting a separate hidden preview JVM at the unused `127.0.0.2:8080` address was then rejected by the session's process-approval policy; no alternate launch was attempted to bypass that rejection and no new GUI was started.

Nova tentativa completa local chegou à inicialização dos testes e foi interrompida sem resumo final, sem nova contagem aprovada. A política recusou iniciar uma JVM separada no endereço local livre; nenhuma alternativa foi usada para contornar essa recusa e nenhuma GUI nova foi iniciada. É necessário iniciar o pacote existente `target/action-preview/quarkus-run.jar` em um terminal do mantenedor, com endpoint MiniStack e credenciais locais de teste, antes de concluir a matriz funcional.

Complete fresh Java/integration and browser checks against the new packaged GUI with local MiniStack. Exercise real bucket/folder/upload creation, exact deletion/neighbor preservation, cancel/focus, duplicate submission, validation, stale/unmarked/error/timeout responses, language/theme/density, 390/1440px and reduced motion. Compare screenshots deliberately; no versioned baseline was regenerated. Keep only MiniStack in Docker; a new application image/consolidated suite remains a separate final stage.

The complete Maven verification, fresh PowerShell analysis/local smoke and functional/visual browser matrix are not passing results at this checkpoint. Existing historical evidence does not fill these gaps. / Suíte Maven completa, análise PowerShell/smoke novos e matriz funcional/visual não estão aprovados neste checkpoint; histórico não preenche essas lacunas.

Concluir checks novos com a GUI recompilada e MiniStack local; validar interação, segurança, modos e layouts reais. Comparar capturas sem regenerar baselines. Docker somente MiniStack; imagem/suíte consolidada apenas na etapa final.

Maintainer visual/assistive-technology acceptance and StackEmu legal/name validation remain pending. No user research, performance claim, release, commit or push is implied by this checkpoint.

Aceite humano e validação jurídica/do nome pendentes. Sem pesquisa, ganho de desempenho, release, commit ou push implícitos.
