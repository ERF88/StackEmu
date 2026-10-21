# ADR-0022 — Apple HIG skill as visual baseline / Skill HIG como padrão visual

- Date / Data: 2026-10-20.
- Status / Estado: ACCEPTED maintainer direction; component migration and visual acceptance separate / Direção do mantenedor ACEITA; migração de componentes e aceite visual separados.

## Decision / Decisão

The maintainer explicitly requested [apple-hig-designer](https://github.com/axiaoge2/Apple-Hig-Designer) as the project's design skill. Use it for subsequent visual/UX work, through the [StackEmu adaptation guide](../design/apple-hig-guidelines.md). Prefer clear hierarchy, quieter chrome, consistent content-first surfaces and deliberate interaction feedback. Read the skill and the guide before changing UI; if the skill is unavailable, install it through the supported installer or report that limitation, rather than silently ignoring it.

Mantenedor pediu a skill como padrão do projeto. Usá-la no trabalho visual/UX com o guia de adaptação: hierarquia clara, menos elementos visuais desnecessários, superfícies consistentes e feedback deliberado. Ler skill/guia antes de mudar UI; instalar pelo mecanismo suportado ou informar indisponibilidade, sem ignorá-la silenciosamente.

The skill is third-party MIT guidance, not official Apple documentation, certification, tested production components or permission to bundle Apple fonts/symbol assets. Retain system fonts, existing licensed SVG icons, semantic authored CSS, local Tailwind generation without Preflight and Quarkus/Qute/HTMX. Do not import its global CSS/reset, JSX, inline styles or an alternate framework. Glass remains opt-in; measured accessibility, native fallback and existing motion/security constraints take precedence over conflicting illustrative recipes. No automatic wholesale re-skin is authorized by installing a skill.

É orientação comunitária MIT, não documentação/certificação Apple, componente comprovado ou licença de fontes/símbolos Apple. Manter fontes locais, SVGs licenciados, CSS semântico/build Tailwind sem Preflight e stack atual. Sem importar CSS/reset global, JSX, estilos inline ou framework novo. Vidro exige pedido; acessibilidade medida, fallback e limites de movimento/segurança prevalecem sobre exemplos conflitantes. Instalação não dispara reestilização integral automática.

## Supersession and limits / Substituição e limites

2026-10-20: the maintainer subsequently authorized cohesive redesign and removal of initial visual blockers under [ADR-0023](ADR-0023-hig-fluidity.md). Frozen V1 decoration and earlier numeric recipes are superseded where changed; this does not waive security/accessibility/licensing or require importing all illustrative effects. / Pedido posterior autoriza composição coesa e substitui receitas visuais, sem dispensar contratos ou impor todos os efeitos.

2026-10-20 first bounded migration: capsule `.button` controls and explicit semantic danger styling for deletion confirmation supersede only the previous button presentation. Existing surface, motion, navigation and ADR-0021 security contracts remain. [Scoped audit and verification](../design/apple-hig-guidelines.md#first-controls-slice--primeira-fatia-de-controles). / Primeira migração delimitada: botões cápsula e variante destrutiva; demais contratos preservados, sem concluir auditoria/migração integral.

Make this the primary visual reference for future refinements, replacing generic inspiration as the default aesthetic direction, not erasing historical work. ADR-0020 service-centered information architecture and ADR-0013 build/motion/accessibility rules remain. S3 capability gates, original-file policy, version/ETag checks, CSRF/CSP and read/mutation separation are unchanged. Existing runtime tokens/layout remain implemented until explicitly migrated and checked. Installation/adoption does not assert HIG/WCAG conformance, user-research findings or release approval. Record source provenance and review future upstream updates rather than treating mutable `main` as pinned.

Torna-se referência visual principal das próximas evoluções, substituindo inspiração genérica como direção estética, sem apagar histórico. Arquitetura de informação da ADR-0020 e regras de build/movimento/acessibilidade da ADR-0013 permanecem; gates S3/arquivo original/versão/ETag/CSRF/CSP/leitura-mutações não mudam. Tokens/layout só mudam em migração explícita verificada. Adotar não comprova HIG/WCAG, pesquisa ou release. Registrar origem e revisar atualizações; `main` mutável não é versão fixada.
