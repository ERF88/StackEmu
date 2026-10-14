# Changelog

All notable changes will be documented in this file. This project intends to follow Keep a Changelog principles and semantic versioning after its first release.

## [Unreleased]

### Added

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
