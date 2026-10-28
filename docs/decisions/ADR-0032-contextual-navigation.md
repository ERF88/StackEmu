# ADR-0032 — Contextual object navigation / Navegação contextual do objeto

- Date / Data: 2026-10-26.
- Status / Estado: maintainer-authorized correction; verification recorded separately / correção autorizada pelo mantenedor; verificação registrada separadamente.

## Decision / Decisão

Correct NAV-01–09 from the local navigation audit. Detail, metadata, versions and existing previews remain in one fixed-geometry inspector when entered from the explorer. Preserve canonical pages for direct links and practical native fallback. This supersedes ADR-0015/0024's no-history-interception and drawer-reset expectations only for the approved read-only inspector; it does not authorize a framework migration or interception of mutations.

Corrigir NAV-01–09 da auditoria local. Detalhes, metadados, versões e prévias existentes permanecem no mesmo inspector ao entrar pelo explorer. Manter páginas canônicas e fallback nativo útil. Substitui restrições anteriores de histórico/reset do drawer somente para o inspector de leitura; sem migração de framework ou interceptação de mutações.

## Contracts / Contratos

Responsiveness extension: current bucket/object listings may refresh in place using `bucket-list`/`object-list` response markers, exact same-origin read URL, HTML/status/selection and active-request checks. Preserve bounded client context and native URLs; never swap the shell, dialogs, arbitrary URLs or emulator HTML. Completion notices update locally and may refresh the current service once; the home still makes no emulator calls. / Extensão de fluidez: listagens atuais atualizam por fragmentos marcados e URL/status/HTML/seleção/requisição verificados; preservar contexto e URLs nativas, sem trocar shell/diálogos ou aceitar HTML do emulador. Avisos atualizam localmente e podem atualizar serviço uma vez; home sem consultas ao emulador.

Maintainer refinement: the inspector header exposes only Close; no full-page details shortcut. Canonical URLs remain available for direct entry and native fallback. Technical metadata omits application-policy explanations; response security headers and metadata-validation rules remain enforced. / Refinamento do mantenedor: cabeçalho do inspector somente com Fechar, sem atalho para página completa. URLs canônicas preservadas para entrada direta e fallback nativo. Metadados técnicos sem explicações de política; cabeçalhos de segurança e regras de validação mantidos.

- Inspector history uses a bounded URL fragment containing an allowlisted same-origin read route, within the current bucket. Back/Forward/reload restore that view; closing returns to the original listing. Read pages expose a checked escaped fragment marker. Retain active-XHR, exact response URL, content-type/status/header and stale-response gates before htmx selection. Never interpret emulator HTML. / Histórico delimitado a leituras locais do mesmo bucket, com marcadores e gates antes de selecionar HTML Qute escapado.
- Listing origin/filter/scroll is bounded local navigation state in URL fragments/history, not persistent storage or a health cache. Validate return routes against the internal bucket-list path and current bucket. Preserve cursor/preferences; never accept an arbitrary redirect URL. Canonical direct entry without origin still has a useful server-generated parent. Without JS, native pages retain the correct object/prefix and preferences; client-only filter/scroll restoration is not promised. / Origem/filtro/rolagem delimitados em fragmento/histórico, sem armazenamento persistente ou redirecionamento arbitrário; fallback preserva pai/alvo, não promete restauração de estado exclusivo do cliente.
- Share preferences retain exact bucket/key; versions and local journal preferences retain pagination markers. Versions of an object return to that object; bucket-wide versions return to the bucket. / Preferências preservam alvo e marcadores; versões do objeto voltam ao objeto.
- Canonical transfer/share forms have contextual return links. App-link navigation from edited canonical forms asks to keep editing or discard explicitly. Do not serialize draft values, selected files, credentials or signed URLs. Native fallback warns that preference navigation reloads/discards unsent changes. Browser controls/closing the tab remain normal browser navigation; do not promise draft recovery or cancellation of server work. / Retornos próprios e confirmação de descarte em links da aplicação, sem persistir dados; fallback avisa sobre recarga e não promete recuperar arquivos ou cancelar operações remotas.
- Offer text preview only when the existing metadata allowlist indicates eligibility; the bounded preview backend still validates bytes/encoding and may refuse. Do not expand formats or treat metadata as proof. / Link de prévia textual condicionado à política existente; backend continua validando/recusando, sem formatos novos.

Keep Quarkus/Qute/HTMX, local scripts/assets, CSP/CSRF, exact-target confirmations, original files, persistent operations and honest uncertain outcomes. Home remains a service catalog with no emulator calls. Operations stays outside primary navigation. Daily validation remains host JVM plus MiniStack-only Docker; new image/container validation and legal validation of the StackEmu name remain separate.

Preservar stack, segurança, alvos exatos, arquivos originais, operações persistentes e incerteza real. Home continua sem consultas e Operações fora do menu. JVM local e somente MiniStack no Docker; imagem final e validação jurídica do nome seguem separadas.

## Verification / Verificação — 2026-10-26

Java 25 Maven verification against local MiniStack passed: 184 tests, no failures, errors or skips (`work/navigation-context-integration.log`). Local frontend checks cover formatting, documentation, CSS, JavaScript and contrast; no application image or container suite was run.

Verificação Maven com Java 25 e MiniStack local aprovada: 184 testes, sem falhas, erros ou skips. Checks frontend locais abrangem formatação, documentação, CSS, JavaScript e contraste; sem imagem da aplicação ou suíte em contêiner.

The browser regression selected 63 cases: 58 passed initially; four historical assertions still expected an unchanged URL instead of the approved inspector fragment, and one native image case timed out during an unusually long interrupted run. After correcting those assertions, all five repeated cases passed (24.6 s). Two further cases passed (16.5 s), covering cross-document Back/Forward and geometry/overflow at 390, 1366 and 1440 px. This is a regression plus targeted repeats, not a claim of one clean 63-case run. Logs remain under `work/navigation-context-*`; original audit evidence was preserved.

A regressão de navegador selecionou 63 casos: 58 passaram inicialmente; quatro asserções históricas ainda exigiam URL sem o fragmento aprovado, e uma prévia nativa excedeu o tempo numa execução excepcionalmente longa/interrompida. Após corrigir as asserções, os cinco casos repetidos passaram (24,6 s). Mais dois passaram (16,5 s), cobrindo voltar/avançar entre documentos e geometria/overflow em 390, 1366 e 1440 px. Evidência de regressão com repetições direcionadas, sem alegar execução única limpa de 63 casos. Logs e auditoria original preservados.

Corrected desktop/mobile captures were inspected for metadata and versions. These checks do not establish full HIG compliance, user-research outcomes or final container acceptance.

Capturas corrigidas de metadados e versões foram inspecionadas em desktop/mobile. Estes checks não comprovam conformidade HIG integral, resultados de pesquisa com usuários ou aceite final em contêiner.
