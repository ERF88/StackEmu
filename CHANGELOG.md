# Changelog

All notable changes will be documented in this file. This project intends to follow Keep a Changelog principles and semantic versioning after its first release.

## [Unreleased]

### Added

- UX-5B native bilingual S3 image preview for bounded static PNG/JPEG, structural/refusal checks and isolated fresh-PNG output; original attachment unchanged, no active formats or CSP relaxation. [Local verification and remaining final-image gates](docs/spikes/ux-5b-results.md).

- UX-5B isolated JDK raster-worker foundation with bounded input/output, heap/process-wait policy and local lifecycle tests; no image-preview route/UI enabled. [Contract](docs/decisions/ADR-0019-raster-preview-isolation.md), [evidence](docs/spikes/ux-5b-worker-results.md).

- UX-5A separate bounded UTF-8 text/JSON-as-text preview with strict byte checks, escaped native page and attachment fallback; no active content, decompression or raster preview. [Contract](docs/decisions/ADR-0018-bounded-text-preview.md), [verification](docs/spikes/ux-5a-results.md).

- UX-4 dismissible, indefinitely retained in-flow notices for confirmed native bucket/folder and enhanced upload results; persistent no-JS fallback and nonduplicated focus/live behavior. Destructive-dialog contract documented, no deletion/modal enabled. [Verification](docs/spikes/ux-4-results.md).

- UX-3 single-file drop/picker enhancement and native labelled upload progress, separating transfer from server-confirmed completion; checked upload workspace, duplicate guards and explicit recovery without browser auto-retry. Native multipart, CSRF, original-file and no-overwrite policies retained. [Execution/remaining acceptance](docs/spikes/ux-3-results.md).

- UX-2B native modal object drawer, escaped checked metadata fragment, stale-response/timeout/manual-retry controls and exact bucket/key clipboard with selectable failure fallback. No preview, ARN, signing or mutation capability is implied; validation recorded separately.

- Confirmed first-delivery operational S3 scope and scope-only ADR-0014: buckets, prefixes, objects, preview, upload/download, copy/move, deletion, clipboard and presigned downloads. Advanced configuration remains excluded; requirements are not claims of implementation.
- Escopo operacional S3 confirmado para a primeira entrega e ADR-0014: buckets, prefixos, objetos, preview, upload/download, cópia/movimentação, exclusão, clipboard e downloads assinados. Configurações avançadas continuam fora; requisitos não comprovam implementação.
- UX-2A explorer table: localized IEC sizes with exact byte counts, filename-type hints and native attachment downloads. Drawer and expanded operational capabilities remain pending their documented gates.
- Tabela UX-2A: tamanhos IEC localizados com bytes exatos, indícios de tipo pelo nome e download nativo. Drawer e demais capacidades operacionais seguem pendentes dos gates documentados.

- UX-1: locked build-only Tailwind generation for Maven/Docker, packaged MIT license, Slate/blue themes and scoped shell utilities; keyboard-reachable short desktop sidebar and restored mobile panel fill. [Validation/acceptance record](docs/spikes/ux-1-results.md); no new S3 capability or publication.

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
