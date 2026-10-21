# ADR-0020 — Service catalog and contextual S3 / Central de serviços e S3 contextual

- Date / Data: 2026-10-20.
- Status / Estado: ACCEPTED implementation direction by the maintainer; visual acceptance and verification are separate / Direção de implementação ACEITA pelo mantenedor; aceite visual e verificações separados.

## Decision / Decisão

2026-10-20 correction: [ADR-0024](ADR-0024-direct-actions-navigation.md) supersedes the quiet-shell Actions/Preferences disclosures below after maintainer feedback. Use direct row actions and a native nonmodal Preferences popover; preserve the service catalog and existing contracts. The earlier in-flow interaction is historical. / Correção posterior substitui expansores por ações diretas e popover nativo, mantendo catálogo/contratos; interação anterior é histórica.

### Quiet-shell refinement — 2026-10-20 / Refinamento do shell

Following maintainer feedback and authorization, group language/theme in one closed-by-default native `details` disclosure named Preferences. Use ordinary links and Tab/Enter/Space, not ARIA menu roles, modal focus traps or a new runtime script. The panel expands in flow rather than obscuring content. Move density to real S3 listing toolbars; URL density still survives all navigation/forms. Remove repeated product subtitles/workspace captions and the duplicate sidebar warning; keep the local-only warning once in the footer. The header minimum becomes 64px with 44px controls, superseding the older 80px visual guidance for this refinement only.

Após feedback/autorização, agrupar idioma/tema em disclosure nativo Preferências, fechado inicialmente, com links/Tab/Enter/Espaço, sem simular menu ARIA, prender foco ou criar script. Painel expande no fluxo. Densidade nas listagens reais, mantendo estado por URL/form. Remover subtítulos/legendas repetidos e aviso duplicado; manter aviso local único no rodapé. Header mínimo de 64px e controles de 44px substituem orientação visual anterior de 80px nesta fatia.

Bucket rows expose existing browse/upload/folder-form links through native Actions disclosures. Object rows retain direct attachment download plus Details and bounded text-preview links in Actions. These links preserve exact encoded targets/preferences and add no SDK calls to decorate listings. Menus contain implemented capabilities only; no disabled/fictitious delete, copy/move or signing controls. Operational completeness remains tracked in the S3 scope, not inferred from this UI change.

Linhas de buckets oferecem navegação/upload/formulário de pasta; objetos mantêm download direto e Detalhes/Prévia de texto em Ações. Alvos codificados/preferências preservados, sem chamadas SDK para decorar linhas. Sem controles fictícios de exclusão, cópia/movimentação ou assinatura. Esta fatia não conclui o escopo operacional S3.

The home is a service entry point, not a resource dashboard. `HomeResource` renders `GET /` without a service port or emulator I/O. Only implemented services appear; currently S3. The home header links to the existing diagnostic with “S3 · Not verified”. This is availability of a check, not aggregate environment health. Each new home request resets that presentation; no health cache, polling, cookies or browser storage is introduced.

A home é entrada de serviços, não painel de recursos. `HomeResource` renderiza `GET /` sem port de serviço ou I/O com emulador. Só serviços implementados aparecem, atualmente S3. O cabeçalho oferece diagnóstico com “S3 · Não verificado”, sem afirmar saúde global. Cada nova entrada volta a esse estado, sem cache, polling, cookies ou storage.

`GET /s3` owns the bucket workspace. Existing `GET /buckets` remains its read-only htmx fragment. S3 and `/connection` perform the existing bucket-list read on demand; success means that listing succeeded, not full service health. Failure may reflect permissions, configuration or connectivity. A failed listing has no numeric count; an empty successful listing has zero. Refresh is contextual, retains native navigation and existing stale-result feedback on transport failure. `/connection` uses native reload for explicit rechecking and no longer embeds the bucket workspace.

`GET /s3` contém a área de buckets; `/buckets` mantém fragmento htmx de leitura. S3 e `/connection` consultam a listagem existente sob demanda. Sucesso comprova essa leitura, não saúde completa; falha pode ser permissão, configuração ou conectividade. Falha não tem número, lista vazia válida tem zero. Atualização contextual mantém fallback nativo e aviso de resultado anterior em falha de transporte. `/connection` usa recarga nativa para nova consulta e não incorpora a área de buckets.

Sidebar navigation contains Home and implemented services. Diagnostic access is contextual; `/components` remains directly accessible and documented for contributors, not linked in product navigation. Hiding the gallery link is not access control. Header context follows the page. Existing preferences, local CSS, accessibility fallbacks, CSP, CSRF, original-file policy and read/mutation boundaries remain unchanged. No framework, generic service registry or speculative application module is added.

Sidebar contém Início e serviços implementados. Diagnóstico é contextual; `/components` continua acessível diretamente e documentado para contribuição, fora da navegação de produto. Ocultar o link não é controle de acesso. Cabeçalho acompanha contexto. Preferências, CSS local, fallback acessível, CSP, CSRF, arquivo original e limites leitura/mutação preservados, sem framework, catálogo genérico ou módulo especulativo.

## Supersession and compatibility / Substituição e compatibilidade

This supersedes only ADR-0013 V2's dominant connection card, home S3 listing and global refresh/test controls. V1 tokens and V3/V4 density/motion remain. Historical evidence is not rewritten. Existing object, preview, download and mutation routes retain their semantics and targets. Old `/#bucket-panel` bookmarks now open Home; use `/s3` for buckets. Query preferences `lang`, `theme`, `density` remain supported throughout.

Substitui somente a hierarquia V2 da ADR-0013: card dominante de conexão, listagem S3 na home e controles globais de atualização/teste. Tokens V1, densidade/movimento V3/V4 continuam. Evidências históricas preservadas; rotas de objetos, preview, download e mutações mantêm semântica/alvos. Favorito `/#bucket-panel` agora abre Início; usar `/s3` para buckets. Preferências continuam propagadas.

## References and limits / Referências e limites

Design references reviewed during planning: [Cloudscape service navigation](https://cloudscape.design/patterns/general/service-navigation/) separates structural and utility navigation; [PatternFly dashboards](https://www.patternfly.org/patterns/dashboard/design-guidelines/) prioritizes task-relevant content; [Carbon empty states](https://www.carbondesignsystem.com/building-blocks/core/patterns/empty-states) distinguishes missing content from errors. Adopt principles, not their libraries. These references are not StackEmu user research. Maintainer validation, human accessibility and final pinned-Linux/new-image comparison remain separate. Legal/name validation is still pending; no release or publication approval is implied.

Referências revisadas no planejamento orientam navegação, prioridade por tarefa e estados vazios/erros, sem adotar bibliotecas. Não são pesquisa com usuários StackEmu. Validação do mantenedor, acessibilidade humana e comparação final Linux/imagem nova permanecem separadas. Nome/jurídico seguem pendentes; sem aprovação de release/publicação.
