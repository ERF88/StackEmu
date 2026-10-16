# Design system direction / Direção do design system

## Next visual/UX cycle / Próximo ciclo visual/UX

UX-1 implements Slate neutrals, one developer-blue accent (`#93c5fd` dark / `#1d4ed8` light), generated local CSS and incremental shell utilities. Desktop sidebar is 256px, sticky and independently scrollable, reserving space for the full keyboard-focus outline. At 900px and below it returns to document flow and stretches to fill its grid row, preserving the mobile panel background. Header minimum is 80px; layout uses 8px rhythm with documented 4px dense/optical exceptions and 44px minimum controls. [Build ownership](development/frontend-build.md) governs sources/output and literal classes; [fresh verification and acceptance](spikes/ux-1-results.md) is separate from historical E1–E7/UX-0 evidence.

UX-1 implementa neutros Slate, azul adaptado por tema, CSS local gerado e utilitários no shell. Sidebar desktop de 256px sticky/rolável, com espaço para o contorno de foco inteiro; até 900px volta ao fluxo e preenche a linha do grid, preservando o fundo mobile. Header mínimo de 80px, ritmo de 8px e exceções de 4px, sem reduzir alvos de 44px. Guias vinculados registram fontes/gerados e validação/aceite atual, separados das evidências históricas.

Historical UX-0 selected Tailwind/CLI 4.3.3 without Preflight through an isolated prototype; [results and limits](spikes/ux-0-css-results.md) record that comparison before production migration. / UX-0 histórico selecionou Tailwind/CLI sem Preflight via protótipo isolado; resultados/limites vinculados registram a comparação anterior à migração do produto.

The [UX-0–7 plan](product/visual-ux-evolution.md) is the current evolution backlog: staged build-time Tailwind migration, 8px rhythm/neutral palette, explorer/drawer, accessible single-file drop/progress and supplementary notifications. Automatic themes/sidebar/breadcrumbs already exist. Preview/deletion/presigning require separate capability gates. Candidate blue/amber colors are not approved contrast pairs. Existing E1–E7 sections describe the delivered baseline; the isolated selection does not silently supersede production tokens, utility restrictions, CSP or contracts. See [ADR-0013](decisions/ADR-0013-visual-ux-evolution.md).

O plano UX-0–7 orienta migração Tailwind em etapas, ritmo de 8px/neutros, explorer/drawer, drop/progresso e notificações. Temas/sidebar/breadcrumbs já existem. Preview/exclusão/assinatura têm gates próprios; cores candidatas não têm contraste aprovado. Seleção isolada não altera silenciosamente tokens, CSS, CSP ou contratos de produção. Consultar ADR-0013.

## E7 acceptance boundary / Limite de aceite E7

The [final validation record](spikes/redesign-e7-results.md) separates executed technical checks and representative screenshot inspection from pending human acceptance. E1–E6 evidence remains historical. P2 proposals are not enabled; legal/name validation, broader assistive-technology/browser review and release approval remain separate.

O [registro final](spikes/redesign-e7-results.md) separa verificações técnicas executadas e inspeção de capturas representativas do aceite humano pendente. Evidências E1–E6 permanecem históricas. P2 não está ativado; validação jurídica/do nome, revisão ampliada de tecnologias assistivas/navegadores e aprovação de release são separadas.

## E6 gallery and visual contracts / Galeria e contratos visuais E6

The gallery labels illustrative values and documents native keyboard/ARIA contracts alongside action, status, loading, empty, table, field and disclosure samples. Hover/focus are real browser interactions, not fake state classes. Deterministic Qute presentation fixtures support 80 screenshot comparisons across themes, languages and 1440/390px; actual local unavailable/error responses receive axe checks. These fixtures are not emulator measurements or certification. [Contracts](development/component-accessibility.md) and [baseline workflow](development/visual-regression.md) describe review limits. [ADR-0012](decisions/ADR-0012-proposed-browser-follow-ups.md) is proposed only; no P2 header, cookie, performance budget or contribution-policy change is enabled.

A galeria identifica valores ilustrativos e contratos de teclado/ARIA junto aos exemplos. Hover/foco são interações reais. Fixtures Qute determinísticas permitem 80 comparações por idioma/tema/largura; respostas locais reais de indisponibilidade/erro recebem axe. Fixtures não são medições ou certificação. Guias vinculados registram limites de revisão. ADR-0012 é apenas proposta; sem ativar cabeçalhos, cookies, orçamento de desempenho ou política de contribuição P2.

## E5 forms and request feedback / Formulários e feedback E5

Mutation forms retain native POST, CSRF, enctype, server validation and original-file policies. Component classes replace the form compatibility selector; fields/CLI previews use 16px text, invalid fields have explicit messages and borders, and confirmation/submit controls have clear targets. File-policy/help/error descriptions are associated; file reselection after errors is explained. The small local, content-fingerprinted `app.js` is a progressive enhancement: htmx refresh sets/clears panel `aria-busy` and localized status text, including failure that explicitly identifies stale results. Native submit marks the form busy and disables only the submit button, never payload inputs, then resets on page restoration. This is not server-side idempotency or upload progress. Without JavaScript, native forms and refresh navigation remain available. CSP and htmx eval restrictions stay unchanged.

Formulários mantêm POST nativo, CSRF, enctype, validação no servidor e políticas do arquivo original. Classes substituem o seletor temporário de formulário; campos/CLI usam 16px, erros têm mensagens/bordas e confirmação/envio têm alvos claros. Política, ajuda e erros do arquivo são associados; a necessidade de selecionar o arquivo novamente é explicada. `app.js` local e versionado por hash é melhoria progressiva: acompanha `aria-busy` e status traduzido do refresh, inclusive falha que identifica resultado anterior. No POST, marca o formulário ocupado e desabilita somente o botão, preservando payload, com restauração ao voltar à página. Não é idempotência no servidor nem progresso de upload. Sem JS, formulários e navegação continuam disponíveis. CSP e restrições de eval são preservadas.

## E4 resource presentation / Apresentação de recursos E4

Bucket/object/tag tables have captions, column and row headers, and labelled focusable scroll regions. Only table containers scroll; long keys remain escaped, verbatim and monospace. Bounded table regions use opaque sticky column headers, row hover/focus highlighting and keyboard scrolling. Breadcrumbs mark the current location and separate ancestors visually; pagination keeps the existing opaque cursor contract without invented page numbers. Prefix/page-limit chips show existing values. Object details separate metadata and download actions into two desktop columns, stacking on mobile. No new commands, copy script, SDK calls or runtime dependencies are introduced; CLI commands remain in the existing mutation flow.

Tabelas de buckets/objetos/tags têm captions, cabeçalhos de coluna/linha e regiões rotuladas, focáveis e roláveis. Somente os contêineres das tabelas rolam; chaves longas continuam escapadas, integrais e monoespaçadas. Cabeçalhos fixos opacos e destaque de linha por hover/foco acompanham a rolagem por teclado. Breadcrumb identifica a localização atual; paginação mantém cursores opacos, sem números inventados. Chips mostram prefixo/limite existentes. Detalhe separa metadados e download em duas colunas no desktop e uma no mobile. Sem novos comandos, script de cópia, chamadas SDK ou dependências; CLI permanece no fluxo de mutação existente.

## E3 overview / Visão geral E3

The responsive bento shows the last bucket-list result, the redacted effective endpoint/region, the returned bucket count and a contextual next action. Status, count and list share one atomic htmx fragment; the refresh trigger stays outside it to retain focus. A failed listing means an unknown count, never zero. No object totals, storage estimates, continuous health monitoring or editable connection profiles are implied. Credentials remain hidden and settings remain startup configuration.

O bento responsivo apresenta o resultado da última listagem, endpoint/região efetivos sem dados sensíveis, quantidade retornada e próxima ação contextual. Estado, contagem e lista são atualizados no mesmo fragmento htmx; o controle permanece fora dele para preservar foco. Falha significa contagem desconhecida, nunca zero. Não há totais de objetos, estimativas de armazenamento, monitoramento contínuo nem perfis editáveis. Credenciais permanecem ocultas e a configuração é lida na inicialização.

## Reference and boundary / Referência e limite

The visual mood of [Wassim Nasr’s portfolio](https://wassimnasr.com/) is an approved inspiration: a dark, cinematic workspace; deep blue surfaces; restrained luminous blue-violet accents; rounded, layered panels; compact navigation; and guided, conversational prompts. It is a reference for atmosphere and interaction rhythm only.

StackEmu must not copy that site’s artwork, character/illustration, text, layout, brand assets, or distinctive portfolio experience. It is an operational console, so data clarity, safe actions, speed, responsive behavior, and accessibility take priority over spectacle. The first implementation uses a shared responsive workspace shell; further visual and accessibility validation remains necessary.

O clima visual do [portfólio de Wassim Nasr](https://wassimnasr.com/) é uma inspiração aprovada: workspace escuro e cinematográfico; superfícies azul-profundo; acentos azul-violeta luminosos e contidos; painéis arredondados em camadas; navegação compacta; e sugestões guiadas. É referência de atmosfera e ritmo de interação, não de cópia.

StackEmu não deve copiar ilustrações/personagem, textos, layout, ativos de marca nem a experiência de portfólio do site. Trata-se de um console operacional: clareza de dados, ações seguras, velocidade, responsividade e acessibilidade têm prioridade.

## Design principles / Princípios

### E2 shell, icons and public assets / Shell, ícones e assets públicos

The shared shell uses three opaque surface levels, a compact service navigation, local SVG icons and explicitly labelled system/light/dark controls. Language/theme links preserve the existing page/query contract; there are no preference cookies or first-party runtime scripts; existing CSRF cookies are unchanged. Decorative coolicons v4.1 Line fragments use currentColor, aria-hidden and no repeated SVG IDs. Only the twenty icons currently used are bundled; copy/search icons wait for their actual components. [Provenance and license](design/icon-sources.md) and the English/Portuguese READMEs retain attribution. The original stack-shaped favicon is separate first-party artwork.

O shell usa três níveis opacos de superfície, navegação compacta, SVGs locais e controles rotulados de tema sistema/claro/escuro. Links preservam página/query; sem cookies de preferência ou JS próprio em runtime; cookies CSRF existentes permanecem. Fragmentos coolicons v4.1 Line são decorativos, currentColor e sem IDs repetidos. Apenas vinte ícones usados são incluídos; copiar/buscar ficam para os componentes futuros. Origem/licença e READMEs mantêm atribuição. O favicon geométrico próprio é arte separada.

Each full page overrides a shared Qute title slot; bucket/object titles use escaped resource names and form titles name the selected action. The shell declares color-scheme/theme-color metadata for explicit and automatic preferences. WebAssetCatalog fingerprints the bundled stylesheet/favicon bytes with SHA-256 once per bean lifecycle and exposes same-origin versioned URLs to Qute; it reads only fixed classpath assets, never user/emulator data. Changing asset contents changes the URL even within one snapshot version. CSP, htmx configuration, checked-template method signatures and HTTP routes remain unchanged. Solid surfaces deliberately defer translucency/blur auditing; manual accessibility and final visual baselines remain pending.

Cada página completa sobrescreve o título Qute; bucket/objeto usam nomes escapados e formulários identificam a ação. Metadados de cor acompanham temas explícitos e automático. WebAssetCatalog calcula SHA-256 de CSS/favicon locais e expõe URLs versionadas; lê apenas caminhos fixos do classpath, nunca dados do usuário/emulador. O conteúdo altera o hash mesmo sem nova versão snapshot. CSP, htmx, assinaturas dos templates e rotas não mudam. Superfícies sólidas adiam a auditoria de vidro/blur; acessibilidade manual e baselines visuais finais seguem pendentes.

### Operational workspace / Workspace operacional

Modernize is a layout reference, not a runtime dependency or imported template. The implementation retains Qute, htmx, local CSS and both themes. A desktop sidebar becomes wrapping navigation on smaller screens; the shared header holds locale/theme preferences. Bucket actions remain next to the page heading, connection results stay in the refreshable bucket panel, and bucket metadata is available in a native disclosure so objects remain the main focus. No analytics, invented metrics, user accounts or inactive future services are added. Template routes, upload/download contracts and the self-only CSP remain unchanged.

Modernize é referência de organização, não dependência nem template importado. A implementação preserva Qute, htmx, CSS local e os dois temas. A sidebar vira navegação com quebra de linha em telas menores; o cabeçalho reúne idioma e tema. Ações ficam junto ao título, o resultado da conexão continua no painel atualizável e os metadados do bucket ficam em um painel nativo recolhível, priorizando os objetos. Não foram adicionados gráficos, métricas fictícias, contas ou serviços futuros inativos. Rotas, contratos de upload/download e CSP permanecem preservados. A implementação inicial não equivale à aprovação final de acessibilidade ou da marca.

1. **Quiet confidence / Confiança silenciosa.** Use a calm dark workspace and a small accent palette; reserve glow for focus, primary action, and live connection status.
2. **Operational clarity / Clareza operacional.** Tables, paths, metadata, and errors remain denser and more legible than decorative content.
3. **Guided first run / Primeira execução guiada.** Empty and unavailable states suggest the next safe action, such as “Test connection” or “Create bucket,” without pretending that configuration persists.
4. **Layered, not noisy / Em camadas, sem ruído.** Use subtle elevation, restrained translucency, and generous spacing. Avoid glass effects behind dense tables where they reduce contrast.
5. **Accessibility is visual quality / Acessibilidade é qualidade visual.** Every state is communicated with text/iconography in addition to color; focus is prominent; contrast meets WCAG AA; and reduced-motion preferences are respected.
6. **Two complete themes / Dois temas completos.** Dark is the visual default candidate inspired by the reference, but light and dark themes are equally supported and token-driven.

## Initial token proposal / Proposta inicial de tokens

These values are starting points for prototyping, not final brand specifications. Contrast must be checked with real text sizes and states.

| Token family  | Dark candidate               | Light candidate          | Intended use                                   |
| ------------- | ---------------------------- | ------------------------ | ---------------------------------------------- |
| Canvas        | midnight navy/blue-black     | cool off-white           | App background                                 |
| Surface       | layered navy                 | white/slate              | cards, navigation, dialogs                     |
| Border        | low-contrast blue-gray       | cool gray                | boundaries without visual clutter              |
| Primary       | electric indigo/violet       | deep indigo              | primary action, focus, selected state          |
| Success       | accessible teal/green        | accessible teal/green    | connection/operation success, always with text |
| Warning/error | accessible amber/red         | accessible amber/red     | risk and failures, always with text            |
| Typography    | near-white + muted blue-gray | near-black + muted slate | readable hierarchy                             |

Use semantic tokens such as `--color-canvas`, `--color-surface`, `--color-primary`, `--color-focus`, `--color-danger`, `--space-*`, `--radius-*`, and `--shadow-*`, rather than hard-coding palette values in components.

## Component language / Linguagem de componentes

| Component         | Direction                                                                                                                                            |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| App shell         | Compact header/sidebar with the active service, effective endpoint status, language switcher, and theme control                                      |
| Connection card   | Endpoint/region summary, masked credential status, explicit connection state, and a “Test connection” action                                         |
| Action buttons    | Rounded but not pill-heavy; one unmistakable primary action per region; destructive actions never share the primary visual treatment                 |
| Data table        | Strong row/column alignment, sticky context where useful, keyboard focus, horizontal scrolling on narrow screens                                     |
| Path/breadcrumb   | Clearly show bucket and prefix; preserve user-provided keys verbatim and do not translate them                                                       |
| Empty/error state | Short explanation, state icon, next safe action; no stack trace or secret                                                                            |
| Guided prompts    | Optional concise chips for safe onboarding actions, e.g. “Test connection”, “Create a bucket”, “Upload a file”; never an arbitrary-command interface |
| Dialogs           | Exact target for destructive confirmations, managed focus, Escape when safe, and focus return to the trigger                                         |

## Motion and imagery / Movimento e imagens

Do not use an illustrated character or non-essential hero visual in the data console MVP. If motion is added, limit it to fast, subtle feedback; obey `prefers-reduced-motion`; never use animation as the sole connection/loading/error indicator. The product should feel polished through hierarchy, spacing, and feedback rather than large decorative artwork.

## Validation checklist / Checklist de validação

### E1 token foundation / Fundação de tokens E1

The single local `app.css` now declares layers in the order reset, tokens, base, layout, components, utilities. Semantic `--color-*`, `--space-*`, `--radius-*`, `--shadow-*` and motion tokens retain the legacy aliases. OKLCH palette declarations are guarded by `@supports`, with hexadecimal fallbacks. Hover uses an sRGB `color-mix()` to stay in gamut and falls back to the primary color without mix support. Interface/monospace system font stacks avoid web-font downloads and CSP changes. CSS layers themselves require a modern browser; only the established Chromium test environment is currently validated.

O único CSS local declara as camadas reset, tokens, base, layout, components e utilities. Tokens semânticos preservam aliases antigos. OKLCH tem `@supports` e fallback hexadecimal; hover usa mistura sRGB dentro do gamut e fallback para a cor primária. Fontes de sistema evitam downloads e mudanças na CSP. As camadas exigem navegador moderno; a validação atual está limitada ao Chromium estabelecido.

Missing/invalid theme parameters select `auto`, following `prefers-color-scheme` with dark fallback. Explicit `dark`/`light` override the OS; the legacy theme helper cycles auto → dark → light → auto; E2 now exposes explicit controls. URL propagation remains; no cookies are added. `prefers-contrast: more` strengthens borders/muted text/focus; forced colors use system colors and a visible current-page border. Motion reduction disables transitions; E1 surfaces remain solid, also under reduced transparency. No templates or CSP were changed. Full card/form class migration, class-free state cleanup and scrollable table wrappers belong to E2–E5; E1 retains scoped content-only compatibility selectors rather than global section/form/nav/header styling.

Tema ausente/inválido seleciona `auto`, seguindo o sistema com fallback escuro. `dark`/`light` explícitos prevalecem; o helper legado alterna auto → dark → light → auto, agora com controles explícitos na E2, mantendo parâmetros e sem cookies. Alto contraste reforça bordas/texto/foco; forced-colors usa cores de sistema e borda na página atual. Movimento reduzido desliga transições; superfícies são sólidas, inclusive com transparência reduzida. Templates/CSP não foram alterados. Migração integral de classes e estados e wrappers de tabela ficam para E2–E5; seletores temporários de conteúdo substituem estilos globais de section/form/nav/header.

#### Measured contrast / Contraste medido

`node scripts/check-contrast.mjs` checks both actual OKLCH declarations (converted to sRGB for WCAG relative luminance) and hex fallbacks, failing below the unrounded threshold. The table reports the minimum across canvas and all three surfaces, rounded to two decimals; both palettes produce the same displayed values. It also checks on-primary text against primary and the mixed hover. These are opaque token-pair checks, not full-page WCAG certification; OS forced-color palettes and future translucent surfaces need separate rendered review. Tiny rounding excursions at the sRGB boundary are clamped within 0.0005 linear-channel tolerance; larger out-of-gamut values fail closed.

O script verifica OKLCH real convertido para sRGB e fallback hexadecimal; falha pelo limite sem arredondamento. A tabela traz mínimos entre canvas e três superfícies, com duas casas; ambas as paletas têm os mesmos valores exibidos. Verifica também texto sobre primário/hover. São pares opacos, não certificação WCAG; paletas do sistema e transparência futura exigem revisão renderizada. Pequenos desvios de arredondamento no limite sRGB são limitados à tolerância linear 0,0005; valores maiores fora do gamut falham.

| Pair / Par            | Dark  | Light | Minimum / Mínimo |
| --------------------- | ----- | ----- | ---------------- |
| Text / surfaces       | 12.18 | 12.95 | 4.5              |
| Muted text / surfaces | 8.49  | 6.36  | 4.5              |
| Primary / surfaces    | 7.07  | 7.42  | 4.5              |
| Success / surfaces    | 9.41  | 6.30  | 4.5              |
| Danger / surfaces     | 8.14  | 6.24  | 4.5              |
| Warning / surfaces    | 9.77  | 6.81  | 4.5              |
| Info / surfaces       | 7.07  | 7.42  | 4.5              |
| Border / surfaces     | 4.64  | 3.32  | 3                |
| Focus / surfaces      | 10.26 | 7.42  | 3                |
| On-primary / primary  | 9.68  | 8.89  | 4.5              |
| On-primary / hover    | 10.57 | 9.78  | 4.5              |

The dependency-free checker and its reference/failure tests run through `npm run check:contrast`, included in `npm run check`. CSS lint covers every local top-level CSS file. / O verificador sem dependências e seus testes de referência/falha integram `npm run check:contrast` e `npm run check`. O lint cobre todo CSS local de primeiro nível.

- Prototype both themes and validate text, focus, status, and disabled-state contrast.
- Verify keyboard navigation, focus return, and visible focus on the dashboard, bucket list, object list, upload, and errors.
- Test responsive tables and long bucket/object names at narrow widths.
- Confirm that suggested actions match actual implemented capabilities.
- Gather user feedback before treating the direction as a final brand system.
