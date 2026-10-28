# ADR-0031 — S3 reference controls / Controles S3 de referência

- Date / Data: 2026-10-26.
- Status / Estado: implementation direction; maintainer acceptance pending / direção implementada; aceite do mantenedor pendente.

## Reference and decision / Referência e decisão

The maintainer selected [Stackport](https://github.com/DaviReisVieira/stackport) as the S3 interaction reference. Inspected its [S3Browser source](https://github.com/DaviReisVieira/stackport/blob/f428f12ec95fb73b270f593695db056af195bfbe/ui/src/components/cloudscape/views/S3Browser.tsx) and repository S3 screenshot at commit `f428f12ec95fb73b270f593695db056af195bfbe`. Adopt contextual table controls, nearby filtering, folder navigation and clear object actions. This is source/design inspection, not execution or security validation of Stackport.

Mantenedor escolheu Stackport como referência. Foram inspecionados o código S3Browser e a captura S3 na revisão fixada acima. Adotar barra contextual junto à tabela, filtro próximo, navegação por pastas e ações claras. Inspeção de fonte/design não é execução nem validação de segurança do Stackport.

Keep Quarkus/Qute/HTMX, ADR-0030 typography and local assets. Use the apple-hig-designer guidance for hierarchy, focus and target sizes. No React/Cloudscape dependency, copied source, logo, AWS icon or screenshot is bundled. New search/move/share SVGs are first-party geometric drawings.

Manter stack, tipografia e assets locais da ADR-0030, com orientação HIG para hierarquia/foco/alvos. Sem React/Cloudscape ou distribuição de código, marca, ícone AWS ou captura do Stackport. Novos SVGs de busca/mover/compartilhar são desenhos geométricos próprios.

## Delivered scope / Escopo entregue

- Explorer tools sit beside the object-list heading: upload, create folder, refresh and versions. Bucket deletion stays beside the bucket title, separate from object actions; existing routes/security remain authoritative. This replaces the earlier placement of all tools beside the bucket title, not exact targets. / Barra junto à listagem; excluir bucket permanece junto ao nome do bucket, separado das ações de objetos. Não mudar alvos/rotas/segurança.
- Filters search names only on the currently rendered page, explicitly labelled as such. They make no emulator request, persist no query and do not change pagination, counts from the server or mutation targets. Case-insensitive NFC matching does not rewrite keys. Clear restores rows/focus; no matches is not an empty bucket. Fragment refresh starts a fresh local filter. / Filtros locais da página, sem I/O/persistência ou alteração de chaves/alvos/paginação; limpar restaura linhas/foco, nenhum resultado não equivale a bucket vazio e atualização reinicia filtro.
- Filters stay hidden without JavaScript; canonical links and tables remain usable. No bulk-selection/delete, tags, settings or exports are advertised merely because the reference has them. Those require separate real contracts and verification. / Sem JS, filtros ocultos e links/tabelas nativos; não expor seleção em lote, tags, configurações ou exportação sem implementação própria.

Preserve CSRF/CSP, bounded processing, exact-key review, honest partial/uncertain outcomes and Home without emulator reads. Do not restore Operations to navigation. Local JVM development and MiniStack-only Docker remain. Maintainer visual acceptance, final new-image validation and legal validation of the StackEmu name remain pending.

Preservar segurança, limites, alvos exatos, resultados parciais/incertos e Home sem consultas. Operações permanece fora do menu. Desenvolvimento no JVM e Docker só MiniStack; aceite visual, imagem final e validação jurídica do nome continuam pendentes.

## Local verification / Verificação local

- Java 25/Maven `verify` with `STACKEMU_INTEGRATION=true`: 184 tests, zero failures/errors/skips, BUILD SUCCESS in 3m17s. Log: `work/stackport-controls-integration.log`. The preceding non-integration run passed with 58 gated skips; it is not the integration result. / Verificação com integração local: 184 aprovados, nenhum ignorado; rodada anterior sem integração é evidência distinta.
- Initial browser selection: 26/26 passed in 2.9m, covering action dialogs, shared object details, shell and new filters. Log: `work/stackport-controls-browser.log`. After CSS cascade, bucket-action scope and mobile-header refinements, the final packaged JVM passed 16/16 in 44.6s: filters, native fallback, 51-object pagination, local fonts, preferences and 390/1366/1440px layouts. Log: `work/stackport-controls-mobile-browser.log`. These selections overlap; do not add them as distinct cases. / Rodadas de navegador aprovadas, com sobreposição; não somar como cenários distintos.
- Representative final desktop-dark/mobile-light captures were inspected against the reference and the preceding StackEmu captures. Screenshots: `work/stackport-controls-mobile-browser/`. Existing versioned baselines were not regenerated. Axe, page-overflow and exact-target assertions passed in the selected tests, not a claim of complete accessibility certification. / Capturas representativas comparadas, sem regenerar baselines nem alegar certificação integral.
- `npm run check` passed after correcting six initial CSS specificity/order lint errors without disabling rules. Includes formatting, Markdown/docs links, Stylelint, ESLint and 44 palette comparisons. Log: `work/stackport-controls-completion-checks.log`. Final UI-only refinements were packaged separately with tests skipped, followed by the 16-case browser repeat; they do not constitute another Java-suite execution. / Checks aprovados após corrigir seis erros iniciais de CSS, sem desligar regras; empacotamento final sem repetir a suíte Java, seguido pela rodada de navegador.

Browser mutation fixtures used an isolated local operation database under `work/`; the delivered JVM uses the existing user database. Docker runs only MiniStack. No application image, container suite, fresh vulnerability scan, push or release was performed. / Testes de navegador usaram banco de operações separado; entrega mantém o banco do usuário. Só MiniStack no Docker, sem imagem nova, suíte em contêiner, scan novo ou publicação.
