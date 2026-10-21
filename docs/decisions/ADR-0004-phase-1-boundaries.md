# ADR-0004: Phase 1 package boundaries and shared UI

- Date: 2026-10-13
- Status: Accepted

## Context

The Phase 0 SDK/container path works. Phase 1 requires useful layers, configuration visibility, navigation, themes, translations and architectural checks.

## Decision

Keep one Maven module, with pure application contracts, an AWS adapter and a web adapter. Keep S3 client creation/lifecycle in the Quarkiverse extension and read its effective properties for connection display. Share a Qute layout and semantic CSS tokens; use local htmx assets and two physical locale catalogs.

## Alternatives and consequences

Splitting five Maven modules or introducing plugin discovery now would add build and contract costs without a second service implementation. Package boundaries receive ArchUnit tests instead. Extract modules once another implementation demonstrates a need.

Locale and theme preferences travel through links at this stage. Cookie persistence is optional future work. Secrets are not part of any view model; endpoint credentials/tokens are redacted before display.

## Evidence

See [Phase 1 validation](../evidence/phase-1-results.md). Framework mechanics follow [Qute](https://quarkus.io/guides/qute-reference/) and [ArchUnit](https://www.archunit.org/userguide/html/000_Index.html) documentation.
