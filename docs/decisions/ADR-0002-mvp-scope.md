# ADR-0002: MVP is an S3 vertical slice

- **Status:** Accepted
- **Date:** 2026-10-12

## Context

Broad multi-service scope risks delaying a usable first release and generic abstractions lack a second real consumer.

## Decision

Deliver an S3 vertical slice: connect/status, list/create buckets, prefix/paginated object navigation, upload, streaming download, clear failures, CLI equivalents, and en/pt-BR. Keep architectural boundaries extensible but avoid a generic module/plugin system until justified by another real module.

## Consequences

Complex S3 editing, destructive bucket cleanup, additional services, persistent profiles, shell/terminal, real AWS administration, and internet exposure are excluded from MVP. Product research must validate demand before scope expansion.
