# ADR-0003: Environment configuration and local-only default

- **Status:** Accepted for planning; controls require implementation verification
- **Date:** 2026-10-12

## Context

Connection profiles can introduce persistence, secret, SSRF, and user-expectation complexity. The GUI has privileged access to emulator data.

## Decision

Use `STACKEMU_` environment variables as the MVP source of truth. Show effective endpoint/region, mask secrets, and offer a safe connection test. Do not support persistent UI editing or per-request arbitrary endpoints. Bind the reference Compose UI port to loopback.

## Consequences

Restart is required for lasting changes. A future dynamic endpoint needs an allow-destination policy and threat review. The unauthenticated MVP is only for trusted local development; it must never be documented as safe for public exposure.
