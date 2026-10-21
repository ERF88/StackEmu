# ADR-0023 — Cohesive HIG fluidity / Fluidez HIG coesa

- Date / Data: 2026-10-20.
- Status / Estado: ACCEPTED maintainer direction; implementation checks and visual acceptance separate / Direção do mantenedor ACEITA; verificações e aceite visual separados.

## Authority and supersession / Autoridade e substituição

The maintainer explicitly authorized prioritizing fluidity and removing initial decisions that limit use of apple-hig-designer. This authorizes a cohesive visual iteration rather than freezing the earlier appearance or limiting work to isolated controls. Supersede ADR-0013's V1 gradient/decorative-glow direction and the earlier fixed palette, radii, spacing/composition and 120/180ms recipes where this iteration changes them. ADR-0022 remains the skill authority; its statement that installation alone does not authorize a re-skin is historical context, not a blocker after this explicit request. Preserve earlier evidence as history.

Mantenedor autorizou priorizar fluidez e remover decisões iniciais que limitam a skill. Isso permite uma composição coesa, sem congelar aparência ou restringir trabalho a controles isolados. Substituir gradiente/brilho V1 e receitas anteriores de paleta, raios, composição e 120/180ms nos pontos alterados. ADR-0022 segue como referência da skill; instalar sozinho não autorizava reestilização, mas o pedido atual autoriza. Preservar histórico.

## Implementation direction / Direção de implementação

Subsequent control/navigation correction: [ADR-0024](ADR-0024-direct-actions-navigation.md) replaces expanding action/preference disclosures and inconsistent return/control presentation. This is an interaction refinement, not another palette-only pass. / Correção posterior substitui expansores e padroniza controles/retornos; não é só alteração de paleta.

- Neutral light/dark canvases, solid grouped surfaces, subtle panel separators/shadows and one measured blue accent. Accessible control borders and focus outlines remain distinct from decorative separators. / Neutros claros/escuros, superfícies sólidas agrupadas e separadores discretos; bordas/foco funcionais separados da decoração.
- Responsive mobile brand/navigation row, continuous wrapping breadcrumbs with separators attached to their item, quieter context labels and shared metadata groups in drawer/full details. Preserve full opaque values, native tables, language/theme/density and directly navigable URLs. / Navegação mobile compacta, breadcrumbs contínuos, contexto discreto e metadados agrupados, mantendo valores/contratos completos.
- Standalone controls retain capsules, now at least 48 CSS px high, with 0.98 press feedback and 100ms feedback/200ms entry/document transitions using standard easing. Pressed minimum remains above 44px. Reduced motion removes transitions and press transforms; no looping, focus delay, automatic mutation retry or essential hover-only action. These timing values are current design choices, not permanent barriers to future reviewed changes. / Controles de 48px, pressão 0,98 e feedback 100/200ms; movimento reduzido remove efeitos. Valores são escolhas revisáveis, não novo bloqueio permanente.
- Use available system fonts, existing licensed SVGs and reviewed patterns from the skill, not wholesale CSS/JSX imports. The skill itself makes glass explicitly opt-in. Do not interpret “all the power” as a requirement for every effect, fabricated content, unnecessary dependencies or unlicensed Apple assets. / Usar fontes locais, SVGs licenciados e padrões revisados; não importar exemplos inteiros nem inferir pedido de vidro/assets/dependências desnecessárias.

## Boundaries and acceptance / Limites e aceite

Visual freedom does not waive accessibility, licensing, CSP/CSRF, escaping, original-file handling, preview isolation, ETag/version confirmation, native fallbacks or read/mutation boundaries. No new service capability, production endpoint access, framework migration, preference storage, polling, publication or release follows from this request. Quarkus/Qute/CSS can implement this iteration; a materially different stack needs a concrete tradeoff review, not a speculative rewrite.

Liberdade visual não dispensa acessibilidade, licenças, CSP/CSRF, escape, arquivo original, isolamento de preview, confirmação/versão/ETag, fallback ou limites de leitura/mutação. Sem inferir capacidade nova, acesso produtivo, migração de framework, armazenamento, polling ou publicação/release. Stack atual atende esta rodada; mudança estrutural exige justificativa concreta.

Run local Java 25/Maven, established style/contrast checks and host-browser coverage for keyboard, no-JS, both themes/languages/densities, reduced motion and 390/1440px layouts. Old tests asserting a gradient or fixed animation recipe must follow the explicitly superseding decision, not be disabled. Review representative before/after captures without silently updating versioned baselines. Docker remains MiniStack-only; final new-image/Linux comparison stays separate. No HIG/WCAG certification, user-research outcome, latency improvement or legal/name validation is implied. [Fresh execution record](../evidence/hig-fluidity-results.md).

Validar localmente Java/estilo/contraste/navegador, incluindo teclado/fallback/preferências e layouts. Atualizar asserts estéticos substituídos, sem desativar testes. Rever capturas sem regenerar baselines; Docker só MiniStack e etapa final separada. Sem alegar certificação, pesquisa, ganho de latência ou validação jurídica/do nome.
