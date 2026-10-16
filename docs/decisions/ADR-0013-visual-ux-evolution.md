# ADR-0013 — visual/UX evolution boundary / Limite da evolução visual/UX

- Date / Data: 2026-10-15.
- Status: ACCEPTED CSS selection after UX-0; UX-1 product build integrated, verification/acceptance tracked separately / SELEÇÃO CSS ACEITA após UX-0; build UX-1 integrado, validação/aceite no registro próprio.

## UX-0 selection (historical gate) / Seleção UX-0 (gate histórico)

Select Tailwind CSS/CLI 4.3.3, build-time only and without Preflight; reuse semantic tokens and native auto/explicit theme semantics. Exact versions/lockfile, real escaped Qute rendering, deterministic isolated Maven/Docker builds, direct MIT licenses and browser candidate diagnostics are recorded in [UX-0 evidence](../spikes/ux-0-css-results.md). Default additive Pico 2.1.1 is not selected because the tested overlay introduces CSP-blocked data images and dark contrast failures; DaisyUI is unnecessary for the tested slice. Do not generalize those findings to all custom Pico configurations. Production assets/routes are unchanged; UX-1 must implement real packaging, component migration and reviewed removals. CSS utilities are full literal tokens in Qute branches, never built from emulator data. Keep generated output separate from authored sources and preserve root lint rules; macro-specific exceptions apply only to the isolated input. All destructive/preview/presigning and ADR-0012 gates remain unchanged.

Selecionar Tailwind/CLI 4.3.3 no build e sem Preflight, reutilizando tokens/temas. Evidência vinculada registra versões, Qute, builds isolados, licenças e diagnósticos. Pico padrão aditivo não foi escolhido por bloqueios CSP/contraste; DaisyUI não é necessário nesta fatia, sem generalizar para toda customização Pico. Produção permanece igual; UX-1 integra empacotamento e migração. Classes literais completas, fontes/gerados separados e exceções de macro somente na entrada isolada. Gates de capacidades/P2 permanecem.

## Context / Contexto

The user now includes preview, deletion, clipboard, presigned download URLs and full operational S3 management in the first current functional delivery, excluding advanced bucket configuration. [Scope record](../product/s3-management-scope.md) supersedes earlier optional/MVP+ scheduling labels. Capability-specific technical ADRs/threat models, local evidence and safe boundaries are still required; scope approval alone does not define batch deletion, version or signing semantics. / Escopo operacional ampliado confirmado para a primeira entrega, sem configurações avançadas. Registro vinculado substitui rótulos antigos de agendamento opcional/MVP+, mantendo ADRs/modelos de ameaça, evidências locais e contratos específicos de exclusão/versões/assinatura.

The [UX-1 execution record](../spikes/ux-1-results.md) owns fresh test results and remaining human acceptance; historical UX-0 statements below do not describe the current packaged product. / O registro UX-1 reúne testes atuais e aceite humano pendente; declarações históricas UX-0 não descrevem o produto empacotado atual.

UX-1 implementation update: root Tailwind/CLI dependencies are pinned to 4.3.3. Maven provisions Node/npm for CSS generation; Docker uses a separate Node stage. [Source/output ownership](../development/frontend-build.md) preserves the existing fingerprinted asset URL, semantic layers and no-Preflight rule. Literal utilities are permitted in migrated shell layout; component CSS remains kebab-case/token-based. Slate neutrals use blue accents `#93c5fd` (dark) and `#1d4ed8` (light), accessible derivatives of the proposed developer-blue direction. No capability gate changes. Verification status is recorded separately, not inferred from this decision.

Atualização UX-1: Tailwind/CLI 4.3.3 no build Maven/Docker, com Node separado do runtime e regras de fontes/gerados no guia vinculado. Utilitários literais no shell migrado; componentes semânticos preservados. Neutros Slate e azul adaptado por tema; nenhum gate de capacidade muda. Status de testes é registrado separadamente.

The new user plan requests utility-first styling, an explorer/drawer, drag/drop/progress, toasts/modals and automatic dark mode. E1–E7 already provide a themed accessible shell and real S3 flows; preview, deletion and presigning are not cosmetic changes. / O novo plano evolui um baseline existente; preview, exclusão e assinatura são capacidades novas, não apenas aparência.

## Initial proposed direction (historical) / Direção inicial proposta (histórico)

Keep Quarkus/Qute/htmx, local assets, system fonts, constructor-injected adapter boundaries and native-page/form fallbacks. Prefer build-time Tailwind subject to the UX-0 comparison/build/CSP/license gates in the [execution plan](../product/visual-ux-evolution.md). Pico remains a candidate; DaisyUI is an optional Tailwind plugin. No dependency/version is selected or installed here, and no global lint relaxation or CDN is permitted.

Manter stack, assets/fontes locais, limites e fallback nativo. Preferir Tailwind no build condicionado ao UX-0; Pico é candidata e DaisyUI plugin opcional. Sem dependência/versão adotada, CDN ou relaxamento global de lint nesta ADR.

The user example is a design sketch, NOT a production contract: `object.formattedSize` and `/s3/preview/{object.key}` or `/s3/delete/{object.key}` are not established bindings/routes. Do not paste them as implemented APIs. Preserve existing links/routes and encode opaque keys through the web mapper. Avoid flex display on a table cell if it breaks table layout; use an inner wrapper. Use translated accessible labels and safe icon fragments, not copied unreviewed markup. A `hx-confirm` prompt is not the requested custom modal or a substitute for CSRF/server confirmation.

O exemplo é esboço, NÃO contrato pronto: propriedade/rotas ilustradas não existem como bindings/APIs estabelecidos. Preservar contratos e codificar chaves no mapper. Usar wrapper interno para flex sem comprometer tabela, traduções e ícones revisados. `hx-confirm` não é modal customizado nem substitui CSRF/confirmação servidor.

Stage safe explorer/form feedback first. Preview needs a dedicated bounded-content threat model; deletion needs a dedicated accepted mutation ADR and verified version semantics; presigning is deferred separately. ADR-0002 MVP exclusions, ADR-0007 write boundaries, ADR-0008 original-file attachment policy and ADR-0009 CSP remain in force until explicitly superseded. ADR-0012 P2 proposals remain unapproved. Documentation of future capabilities does not enable them.

Priorizar explorer/formulários seguros. Preview exige modelo de ameaça e limites; exclusão exige ADR aceita e semântica de versões comprovada; assinatura fica separada. ADRs existentes continuam válidas até substituição explícita; P2 da ADR-0012 não é aprovado por este plano. Planejar capacidade não a ativa.

## Initial acceptance gate / Gate inicial de aceite

UX-0 must produce an exact pinned choice, reproducible local/CI build, class detection proof, style/layer migration rules, license notices, contrast/theme parity and reviewed visual changes before this ADR can become accepted for CSS adoption. Capability-specific approvals remain separate. No improvement in speed, security, user satisfaction or maintenance cost is claimed without measurements/research. StackEmu name/legal validation remains pending.

UX-0 deve comprovar escolha fixada, build, classes, migração/lint/camadas, licenças, contraste/temas e imagens revisadas antes de aceitar adoção CSS. Aprovações de capacidades são separadas. Sem promessas de desempenho, segurança, satisfação ou manutenção sem evidência; nome/jurídico seguem pendentes.
