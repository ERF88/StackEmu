# Changelog

All notable changes will be documented in this file. This project intends to follow Keep a Changelog principles and semantic versioning after its first release.

## [Unreleased]

- Correct contextual navigation: keep metadata/versions/previews in the inspector with bounded history and listing-origin restoration; preserve share targets and pagination in preferences, add canonical transfer/share returns and explicit unsent-change discard, and hide ineligible text previews. [Contract](docs/decisions/ADR-0032-contextual-navigation.md).

- Fix duplicate upload selectors: keep one native file input inside the drop surface, removing the second picker button while preserving keyboard, drag/drop and no-JavaScript upload.

- Adapt S3 controls from the maintainer's Stackport reference: contextual explorer toolbar, explicit folder creation, page-local name filters with clear/no-match states and distinct move/share icons. Preserve native routes, security gates and existing typography; no React/Cloudscape import or speculative batch controls. [Scope](docs/decisions/ADR-0031-s3-reference-controls.md).

- Adopt locally bundled Space Grotesk/IBM Plex Sans typography and compact precision-console controls. Remove Operations from primary navigation without disabling durable execution. Replace bare metadata/version links with accessible icon controls. [Decision and scoped verification](docs/decisions/ADR-0030-precision-console.md). / Adotar tipografia local e controles compactos; remover Operações do menu, preservar execução persistente e substituir links soltos de metadados/versões por ícones acessíveis.

- Introduce minimal action dialogs with automatic success return, separate metadata/operation pages and persistent H2-backed S3 execution. Add reviewed prefix/version cleanup beyond 25 entries, interruption/reconciliation, individual copy/move, conditional upload replacement and explicit presigned downloads. Earlier bounded-cleanup entries below describe preceding slices superseded by [ADR-0029](docs/decisions/ADR-0029-minimal-workspace-operations.md).

- Add separate reviewed Empty and delete for at most 25 entries in never-versioned buckets, with typed-name confirmation, independent one-use tickets/CSRF, fresh manifest checks, per-key outcomes and no automatic destructive retries. Simplify action dialogs in both languages; retain security details in disclosures and all original-file gates.

- Fix the folder-marker trap with explicit empty-folder review and one exact conditional deletion after fresh prefix/size/version checks. Keep children untouched, CSRF and one-use tickets. Rename creation to Create folder and center table action headings. No recursive cleanup.

- Show only the next folder name in prefix rows while retaining exact navigation targets and breadcrumbs. Simplify shared object details with a filename heading, grouped actions, inline exact-value copy controls and collapsible technical metadata/policy; retain canonical pages and all operation gates.

- Focus the explorer on resource operations: remove bucket region/tags and listing-help blocks together with their metadata reads; align table names/icons/actions and heading controls. Hide redundant first-page navigation while retaining cursor recovery. Security and original-file flows remain unchanged.

- Extend the compact HIG-inspired treatment to the bucket explorer: named icon actions, contextual refresh, smaller action column and secondary metadata/guidance below the list. Preserve full keys, canonical routes, previews/downloads and independent deletion gates. [Contract](docs/decisions/ADR-0026-bucket-workspace-deletion.md#explorer-continuation--continuação-do-explorer).

- Compact bucket row icons with accessible labels, a narrower shell and one balanced table spacing; remove the density selector while preserving legacy URLs. Add reviewed empty, never-versioned bucket deletion with CSRF, one-use tickets and no automatic retry. Empty and delete is mapped, not enabled. [Contract](docs/decisions/ADR-0026-bucket-workspace-deletion.md) · [Verification](docs/evidence/bucket-workspace-results.md).

- Add JavaScript-driven creation/upload/deletion-review windows with explicit action buttons instead of generic checkboxes; move density, diagnostics and the local-only notice into Preferences. Preserve server validation and deletion gates. [Decision and limits](docs/decisions/ADR-0025-action-dialogs.md); local runtime verification remains separate.

- Replace expanding Actions/Preferences with direct resource actions and native nonmodal Preferences; standardize icon/label buttons and contextual returns before titles. Preserve original-file, native fallback and security contracts. [Decision](docs/decisions/ADR-0024-direct-actions-navigation.md) · [Local checks](docs/evidence/direct-controls-results.md).

- Cohesive HIG fluidity iteration under ADR-0023: neutral solid surfaces, quieter separators, compact mobile navigation, continuous breadcrumbs, grouped metadata and 100/200ms feedback with reduced-motion fallback. Supersedes frozen V1 decoration; [fresh checks and limits](docs/evidence/hig-fluidity-results.md), without relaxing security or claiming certification.

- HIG explorer refinement: native collapsible guidance, actionable empty-prefix state without an empty table, grouped existing drawer actions ahead of auxiliary content and solid loading/error surfaces. Security, native fallbacks and HTTP status contracts retained; [scope and checks](docs/design/apple-hig-guidelines.md#explorer-and-state-slice--fatia-de-explorer-e-estados).

- First bounded HIG refinement: capsule action buttons and explicit semantic danger styling for native deletion confirmation. Existing targets, focus, routes and security gates retained; [scoped audit and verification](docs/design/apple-hig-guidelines.md#first-controls-slice--primeira-fatia-de-controles), not full HIG certification or maintainer acceptance.

- Adopt the maintainer-requested apple-hig-designer skill as the visual/UX baseline, with project-specific Qute/CSS, accessibility and security adaptations; install locally without adding a GUI dependency. [ADR-0022](docs/decisions/ADR-0022-apple-hig-design-baseline.md). This is standards adoption, not a completed visual migration or HIG certification.

- Single exact-object deletion with native review, bounded five-minute one-use confirmations, CSRF, version-state refusal and ETag-conditional mutation. Isolated SDK client plus explicit empty fixed-length DELETE prevent retries in tested 503/lost-response fixtures; uncertain outcomes remain explicit. [Contract and limits](docs/decisions/ADR-0021-single-object-deletion.md) · [Local evidence](docs/evidence/single-object-deletion-results.md).

- Quieter native Preferences disclosure, listing-local density and per-resource action links; remove repeated captions/warnings. Add local conditional-delete/version-state readiness spike without enabling destructive GUI operations.

- Service-oriented Home without emulator reads; dedicated `/s3` bucket workspace, contextual diagnostics/status and refresh; contributor gallery removed from product navigation, still available at `/components`. New ADR-0020 preserves existing operation/security contracts.

### Added

- Maintainer V4 visual acceptance and V6 consolidated local validation: fresh Linux/amd64 image, 107 clean host Java tests, 157 full Linux browser cases, 80 deliberately reviewed/reconciled PNGs, container/S3/CSRF checks, 128 MiB/64 MiB heap streaming and zero reported all-severity scan findings with OS/Java inventories. Density endpoint and shell-selection test fixtures corrected, no product-policy/tolerance relaxation. [Execution, failed first comparison and remaining release gates](docs/evidence/visual-v6-results.md).

- V4 optional 180ms card entry and CSS-only same-origin document transitions, reduced-motion opt-out and settled bucket/key clipboard feedback with persistent text/border (120ms); native navigation, static skeletons, CSP and original files unchanged. [Fresh verification and remaining acceptance](docs/evidence/visual-v4-results.md).

- Preview transport correction: one adapter-owned bounded response reader/cleanup owner, 1s socket timeout, repeated stalled/trickling and incomplete XML error-body regressions, metadata refusal and shared-client lifecycle checks; existing byte/deadline/security policies retained. [Fresh evidence and remaining final gates](docs/evidence/preview-transport-correction.md).

- V3 native comfortable/compact table-density preference through allowlisted URL/form values, preserved across navigation and outcomes; semantic spacing tokens, retained sticky headers/icons, always-visible focus-emphasized actions and unchanged original-key/44px target rules. [Verification and pending acceptance](docs/evidence/visual-v3-results.md).

- V2 dashboard hierarchy, separate native connection-diagnostic link and refresh, and static count skeleton tied to the existing real busy state. Existing failure feedback/native fallback retained; no fabricated counts or new health API. [Execution and pending acceptance](docs/evidence/visual-v2-results.md).

- V0 accepted with system fonts/current CSP; V1 canvas depth, restricted decorative glow, accessibility preference fallbacks and single card/table frame. Local verification and pending Linux baseline/human acceptance are tracked in the [V1 record](docs/evidence/visual-v1-results.md). Density/motion, D2–D6 and release gates remain pending.

- Documentation consolidation: dated paired READMEs with illustrative baseline captures and host-JVM quick start, four-group index, current gate/status page, phase history and maturity inventory. Historical evidence relocated byte-for-byte to `docs/evidence/`; local link/index checks added. No runtime, language-policy, visual-decision or release approval changes.

- UX-5B native bilingual S3 image preview for bounded static PNG/JPEG, structural/refusal checks and isolated fresh-PNG output; original attachment unchanged, no active formats or CSP relaxation. [Local verification and remaining final-image gates](docs/evidence/ux-5b-results.md).

- UX-5B isolated JDK raster-worker foundation with bounded input/output, heap/process-wait policy and local lifecycle tests; no image-preview route/UI enabled. [Contract](docs/decisions/ADR-0019-raster-preview-isolation.md), [evidence](docs/evidence/ux-5b-worker-results.md).

- UX-5A separate bounded UTF-8 text/JSON-as-text preview with strict byte checks, escaped native page and attachment fallback; no active content, decompression or raster preview. [Contract](docs/decisions/ADR-0018-bounded-text-preview.md), [verification](docs/evidence/ux-5a-results.md).

- UX-4 dismissible, indefinitely retained in-flow notices for confirmed native bucket/folder and enhanced upload results; persistent no-JS fallback and nonduplicated focus/live behavior. Destructive-dialog contract documented, no deletion/modal enabled. [Verification](docs/evidence/ux-4-results.md).

- UX-3 single-file drop/picker enhancement and native labelled upload progress, separating transfer from server-confirmed completion; checked upload workspace, duplicate guards and explicit recovery without browser auto-retry. Native multipart, CSRF, original-file and no-overwrite policies retained. [Execution/remaining acceptance](docs/evidence/ux-3-results.md).

- UX-2B native modal object drawer, escaped checked metadata fragment, stale-response/timeout/manual-retry controls and exact bucket/key clipboard with selectable failure fallback. No preview, ARN, signing or mutation capability is implied; validation recorded separately.

- Confirmed first-delivery operational S3 scope and scope-only ADR-0014: buckets, prefixes, objects, preview, upload/download, copy/move, deletion, clipboard and presigned downloads. Advanced configuration remains excluded; requirements are not claims of implementation.
- Escopo operacional S3 confirmado para a primeira entrega e ADR-0014: buckets, prefixos, objetos, preview, upload/download, cópia/movimentação, exclusão, clipboard e downloads assinados. Configurações avançadas continuam fora; requisitos não comprovam implementação.
- UX-2A explorer table: localized IEC sizes with exact byte counts, filename-type hints and native attachment downloads. Drawer and expanded operational capabilities remain pending their documented gates.
- Tabela UX-2A: tamanhos IEC localizados com bytes exatos, indícios de tipo pelo nome e download nativo. Drawer e demais capacidades operacionais seguem pendentes dos gates documentados.

- UX-1: locked build-only Tailwind generation for Maven/Docker, packaged MIT license, Slate/blue themes and scoped shell utilities; keyboard-reachable short desktop sidebar and restored mobile panel fill. [Validation/acceptance record](docs/evidence/ux-1-results.md); no new S3 capability or publication.

- UX-0 isolated locked Tailwind/Pico comparison, escaped Qute fixtures, deterministic CSS build/size/license reports and Chromium diagnostics; Tailwind/CLI 4.3.3 without Preflight selected for staged UX-1 migration, production assets unchanged.

- Documentation-only UX-0–7 evolution plan and proposed ADR-0013: build-time CSS evaluation, explorer/drawer, drop/progress, notifications and separately gated preview/deletion; updated contributor/security guidance, no runtime changes.

- E7 final redesign validation record, reproducible commands, screenshot references and explicit human-review/release limits; no P2 activation or publication.

- E6 bilingual component gallery/contracts, deterministic Qute presentation fixtures, unavailable/error axe checks and container-based visual snapshots; P2 follow-ups recorded as unapproved ADR-0012 proposals only.

- E5 native-form presentation and progressive local request feedback: localized loading/failure/submitting status, real `aria-busy`, disabled submit-only state and page-restoration reset; unchanged CSRF/upload/download policies.

- E4 semantic resource tables with keyboard-scrollable regions, sticky headers, breadcrumb/context chips and responsive object metadata/download panels; unchanged S3 contracts.

- E3 responsive overview with effective connection settings, snapshot-based bucket count and contextual next action; atomic htmx updates and unknown-count failure state.

- E2 local SVG shell, explicit system/light/dark preference controls, localized page titles and original SVG favicon.
- Bundled asset SHA-256 URL fingerprints and coolicons v4.1 CC BY 4.0 attribution/source inventory; unchanged CSP.

- E1 layered CSS, semantic OKLCH tokens with hex fallbacks/legacy aliases, automatic system theme and accessibility preferences.
- Dependency-free contrast gate/reference tests and browser preference/responsive checks; no template or CSP changes.

- Complete runtime package/CVE inventory and all-severity clean-image CI gate with OS/Java inventory checks.

- Phase 5 Maven CycloneDX SBOM, isolated archive-based Trivy scan and retained CI reports.
- Hardened local Compose, pinned build/runtime digests, container policy checks and packaged license/notices.

- Phase 4 Chromium/axe browser tooling, bilingual/theme/mobile navigation checks and isolated S3 UI flow.
- REST browser security headers and accessible field-error associations; pinned ESLint for test JavaScript.

- Phase 3 bucket creation, single-file upload, folder markers, per-field validation and explicit confirmation.
- Quarkus CSRF forms, upload/request limits, temporary cleanup and illustrative POSIX CLI equivalents.
- Separate mutation port/adapter and automated end-to-end mutation/security cases.

- Naming/responsibility baseline, bilingual guide, class inventory, ADR-0006 and contributor/agent instructions.
- Stronger architecture rules for roles, identifiers, constructor injection and AWS/web isolation.

- Phase 2 bucket details/tags, paginated objects/common prefixes, breadcrumbs, metadata and streaming downloads.
- MiniStack integration coverage, stream cancellation/closure tests and a 128 MiB download proof with a 64 MiB Java heap.
- Fase 2: leitura/paginação/metadados/download; guias/evidências bilíngues e ADR de streaming.

- Per-file coding conventions, EditorConfig, LF Git attributes, Maven formatting checks and development lint tools.
- CI checks for Markdown, CSS, PowerShell, Dockerfile, workflows and Compose; Java/XML style is checked during Maven validate.

- Phase 1 effective connection view, shared Qute layout/navigation, light/dark semantic tokens and component gallery.
- Locale negotiation and English/pt-BR catalogs, translation parity and ArchUnit boundary checks.

- Phase 0 Java 25 / Quarkus 3.40.1 read-only S3 screen, checked Qute templates and local htmx asset.
- MiniStack 1.5.25 Compose environment, non-root JVM image, four tests and passing Linux/amd64 CI.
- Reproducible run guide and actual spike evidence, including explicit remaining validation.

- Foundational planning documentation in English and Brazilian Portuguese.
- Initial architecture, scope, security, configuration, testing, release, and ADR records.

### Changed

- Responsive operational workspace with sidebar, shared preference header, grouped S3 actions and collapsible bucket metadata; existing routes and runtime stack retained.
- Workspace operacional responsivo com sidebar, preferências compartilhadas, ações S3 agrupadas e metadados recolhíveis; rotas e stack preservados.

- Adopt official Temurin 25/Alpine runtime with pinned corrected libpng/zlib packages, without vulnerability suppression.

- Upload keys now use the current prefix and original filename automatically; valid MIME metadata and download filename/extension are retained, with safe fallbacks.

- Purpose-revealing port/adapter/view/exception names; separate connection/component resources and download helpers. HTTP routes remain unchanged.

### Not yet released or verified

- Full compatibility, benchmarks, arm64 and broader security review remain pending.
