# Single-object deletion — local evidence / Exclusão individual — evidência local

Date / Data: 2026-10-20. Contract / Contrato: [ADR-0021](../decisions/ADR-0021-single-object-deletion.md).

## Scope / Escopo

One exact non-marker object in a never-versioned bucket, with a native GET review and CSRF-protected POST. The authoritative bucket/key/ETag stays in a bounded server-held, random one-use confirmation (five minutes, 128 pending maximum). Version state is rechecked before conditional DELETE. Existing row Actions, full object details and drawer navigate to review; opening a link never deletes. English/pt-BR and URL language/theme/density remain supported. No extra metadata requests decorate object listings. No runtime JavaScript/CSS or new dependency was added for this slice.

Um objeto exato não-marker em bucket nunca versionado, com revisão GET nativa e POST protegido por CSRF. Bucket/chave/ETag ficam em confirmação aleatória guardada no servidor, uso único, cinco minutos e máximo 128 pendentes. Estado de versão é revalidado antes do DELETE condicional. Ações da linha/detalhes/drawer abrem revisão, sem excluir por GET. Idiomas/tema/densidade por URL preservados, sem HEAD adicional para decorar linhas. Sem novo JS/CSS de runtime ou dependência nesta fatia.

## Investigations and failed runs / Investigações e execuções com falha

These failures remain evidence, not passing checks / Falhas preservadas, não checks aprovados:

- `work/delete-format.log`: initial Spotless rejected wildcard imports in two new tests; replaced with explicit imports, no suppression.
- `work/delete-maven-first.log`: compilation refused request-level `retryStrategy`; installed SDK 2.54.13 has no such public request override. Moved retry ownership into a scoped adapter-owned client, preserving managed-client lifecycle.
- `work/delete-maven-second.log`: 123 tests, one failure and one error. The no-retry test observed **four** wire DELETEs on 503 because inherited resolved client overrides replaced the explicit strategy. The existing raster fixture could expose an empty PID file before writing finished; publish its readiness by atomic rename after writing, without removing interruption/termination assertions or changing production preview code.
- `work/delete-focused-third.log`: 15 tests, one failure; `doNotRetry()` alone still inherited the overriding resolved configuration. Do not claim this was enough.
- `work/delete-focused-fourth.log`: 15/15 passed using fresh deletion override configuration, but this did not yet test a lost response.
- `work/delete-transport.log`: seven tests, one failure; URLConnection replayed a bodyless DELETE twice after a lost response despite SDK retries being disabled.
- `work/delete-fixed-length.log`: seven tests, three failures/one error; adding only Content-Length: 0 prevented transmission. No UI action was exposed on that failed basis.
- `work/delete-empty-body.log`: seven adapter tests passed after adding a scoped empty RequestBody interceptor alongside Content-Length: 0. Both 503 and lost-response fixtures observed exactly one DELETE and an uncertain outcome. Client-close test confirms the separately supplied configured client remains usable. This is local JDK/SDK evidence, not a universal transport guarantee.

Correções: imports explícitos; cliente de exclusão isolado com overrides novos e `AwsRetryStrategy.doNotRetry()`; corpo vazio real e comprimento fixo para impedir replay interno do transporte; publicação atômica do PID somente no fixture. Nenhuma asserção/tolerância foi relaxada. Links contextuais foram acrescentados após os gates locais de alvo/versão/CSRF e transporte, antes da regressão final completa.

- `work/delete-browser-focused.log`: first browser run had four passes/four timeouts. Traces showed the axe scan waiting for browser-script work with JavaScript disabled; cleanup tied to the timed-out test context also failed. Move cleanup to a hook-owned request context, and run axe only in the four JS-enabled cases. Keep all eight native layout/navigation/keyboard/POST/target/neighbor/preference assertions. Removed only the four recorded fixture buckets and their two exact keys; all cleanup responses were 204, no traversal or user data touched.
- `work/delete-browser-corrected.log`: all eight corrected cases passed in 25.5s, with and without JS; this is repeated inside the final regression, not additive coverage.

Correção do navegador: scanner axe depende de JS; auditoria fica nos casos com JS, enquanto fallback sem JS mantém os gates funcionais. Cleanup passou a contexto próprio do hook, independente do teste expirado. Quatro fixtures UUID antigos foram limpos por alvo exato, sem afetar dados do usuário.

## Current verification / Verificação atual

Host Windows/amd64, Java 25.0.4.1/Maven 3.9.9, packaged Quarkus 3.40.1/SDK 2.54.13, Node 24.10.0/Playwright 1.64.1 and host Chrome 155.0.8059.80; local MiniStack 1.5.25. GUI restored from the verified package on loopback 8080 (PID 46576 at this checkpoint).

- `work/delete-maven-final.log`: **125 tests, zero failures/errors/skips**, full `mvn verify` with `STACKEMU_INTEGRATION=true`; BUILD SUCCESS at 2026-10-20 12:09:16 -03:00, 2m23s. Includes five real SDK/web MiniStack deletion cases, seven controlled adapter HTTP/lifecycle cases and two confirmation-store cases; repeats above are not added.
- `work/delete-browser-final.log` / `work/delete-browser-final-report.json`: **102/102 host behavioral cases**, zero failed/skipped/flaky, 322418ms. Includes the eight new deletion flows, two languages/two themes, 390/1440px, no-JS fallback, keyboard consent/submission, read-only cancel, neighboring-object preservation and exact encoded return with compact preference. Axe runs only in JS-enabled cases. The host config intentionally excludes pinned-Linux `visual.spec.mjs` and the container-dependent `upload.spec.mjs`; no new pixel-baseline pass is claimed.
- Representative captures actually reviewed: pt-BR dark at 390px and pt-BR light at 1440px from `work/delete-browser-corrected`; not an all-image human accessibility/design acceptance.
- `work/delete-browser-lint-repeat.log` / matching JSON report: the eight deletion cases passed again in 28.1s after the test-only explicit `window.innerWidth` lint fix; do not add this repeat to 102.
- `work/delete-hig-style-first.log`: `npm run check` exited 0: Prettier, markdownlint on 115 Markdown files, five documentation-checker tests plus exact-once index/link checks, Stylelint, ESLint and two contrast-checker tests with 44 existing measured pairs. This verifies the current project palette, not the skill's unimported palette or HIG conformance.
- `scripts/check-style.ps1`, `scripts/smoke.ps1` and `git diff --check` passed locally. Only MiniStack is running in Docker; GUI PID 46576 listens on 127.0.0.1:8080.
- SHA-256 comparison: all 80 versioned PNGs match the preceding `work/v6-proposed-baselines/visual.spec.mjs` checkpoint. No baseline changed for deletion or HIG adoption.

Verificação final local: 125 Java e 102 cenários comportamentais de navegador aprovados, sem somar repetições. Ambiente/pacote e exclusões explicitados; axe só com JS, fallback real testado sem JS. Duas capturas representativas revisadas, não aceite humano completo. Checks locais de estilo/documentação/PowerShell/smoke/whitespace aprovados, 80 baselines inalteradas. Repetição final dos oito casos não aumenta o total de 102.

## Subsequent design-standard request / Pedido posterior de padrão visual

The maintainer then requested apple-hig-designer. It was installed in the user's Codex skills directory and read with its applicable references. [ADR-0022](../decisions/ADR-0022-apple-hig-design-baseline.md) and the [project guide](../design/apple-hig-guidelines.md) record adoption and web adaptations. Those changes are documentation/contributor instructions only: the deletion screenshots/tests above do not demonstrate an Apple-style re-skin or HIG/WCAG certification. Broader visual migration remains a separate next slice.

Depois, o mantenedor solicitou a skill HIG, instalada/lida com referências aplicáveis. Adoção registrada em guia/ADR; mudanças somente nas diretrizes/documentação, sem alegar que as capturas/testes da exclusão comprovam reestilização Apple ou certificação. Migração visual ampla segue próxima fatia.

## Boundaries / Limites

Fixtures use only loopback MiniStack 1.5.25 and their own UUID buckets/exact keys. Version-state tests leave no object versions/delete markers and delete only their own empty fixture bucket. HTTP fault tests use an ephemeral loopback server. No production target, GUI container, application image build, consolidated container suite, new scan, baseline update, commit/push/tag or release was performed for this slice.

Fixtures usam somente MiniStack local e buckets UUID/chaves próprias; teste de versão não deixa versões/markers. Servidor de falhas usa loopback efêmero. Sem produção, contêiner GUI, imagem nova, suíte consolidada em contêiner, scan novo, atualização de baseline, commit/push/tag ou release.

This does **not** finish bulk/prefix deletion, bucket empty/delete, copy/move, ARN/URL clipboard or presigned downloads. External version-state changes cannot be atomically guarded by S3 DELETE; equal-ETag replacements cannot be distinguished. Transport/5xx/version-marker ambiguity is shown as uncertain, consumes the ticket and never offers automatic retry. Human accessibility/maintainer acceptance, final pinned-Linux/new-image validation and legal/name validation remain pending. No user-research or security certification is claimed.

Não conclui exclusão em lote/prefixo, esvaziar/excluir bucket, cópia/movimentação, clipboard ARN/URL ou assinatura. Mudança externa de versão não é atomicamente protegida por DELETE; ETag igual não distingue substituições. Incerteza consome ticket e não gera retry automático. Aceite humano, validação final Linux/imagem nova e nome/jurídico seguem pendentes, sem alegar pesquisa/certificação.

References / Referências: [AWS retry configuration](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/retry-strategy.html), [Java 25 HttpURLConnection](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/HttpURLConnection.html), installed SDK public API/bytecode inspected locally. Documentation supports configuration semantics; exact wire counts above come from executed fixtures, not inferred from documentation. / Contagens de rede são dos fixtures executados, não inferidas da documentação.
