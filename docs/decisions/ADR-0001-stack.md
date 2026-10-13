# ADR-0001: Initial application stack

- **Status:** Accepted for planning; implementation validation pending
- **Date:** 2026-10-12

## Context

StackEmu needs a low-friction, server-rendered local GUI with an extensible JVM architecture. Exact compatibility of Java 25, Quarkus 3.40, extensions, SDK, and toolchain has not been confirmed.

## Decision

Plan for Java 25 LTS (evaluate Java 21 alternative), Quarkus 3.40 LTS, Quarkus REST + Qute, htmx for bounded partial interactions, native CSS tokens, Maven, and AWS SDK for Java 2.x through the S3 extension/BOM.

## Alternatives

SPA/frontend pipeline; a CSS framework; an immediate native image; or a generic plugin platform. These do not fit the initial evidence or MVP focus.

## Consequences and evidence

Phase 0 must validate artifacts/BOM, Qute checked templates where relevant, SDK endpoint override/path style, htmx CSRF/error/history behavior, and asset bundling value. No stack example is declared tested by this ADR.
