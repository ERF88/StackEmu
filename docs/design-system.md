# Design system direction / Direção do design system

## Reference and boundary / Referência e limite

The visual mood of [Wassim Nasr’s portfolio](https://wassimnasr.com/) is an approved inspiration: a dark, cinematic workspace; deep blue surfaces; restrained luminous blue-violet accents; rounded, layered panels; compact navigation; and guided, conversational prompts. It is a reference for atmosphere and interaction rhythm only.

StackEmu must not copy that site’s artwork, character/illustration, text, layout, brand assets, or distinctive portfolio experience. It is an operational console, so data clarity, safe actions, speed, responsive behavior, and accessibility take priority over spectacle. This direction is unimplemented and should be validated with prototypes.

O clima visual do [portfólio de Wassim Nasr](https://wassimnasr.com/) é uma inspiração aprovada: workspace escuro e cinematográfico; superfícies azul-profundo; acentos azul-violeta luminosos e contidos; painéis arredondados em camadas; navegação compacta; e sugestões guiadas. É referência de atmosfera e ritmo de interação, não de cópia.

StackEmu não deve copiar ilustrações/personagem, textos, layout, ativos de marca nem a experiência de portfólio do site. Trata-se de um console operacional: clareza de dados, ações seguras, velocidade, responsividade e acessibilidade têm prioridade.

## Design principles / Princípios

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

- Prototype both themes and validate text, focus, status, and disabled-state contrast.
- Verify keyboard navigation, focus return, and visible focus on the dashboard, bucket list, object list, upload, and errors.
- Test responsive tables and long bucket/object names at narrow widths.
- Confirm that suggested actions match actual implemented capabilities.
- Gather user feedback before treating the direction as a final brand system.
