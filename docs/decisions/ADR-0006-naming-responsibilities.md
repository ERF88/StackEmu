# ADR-0006 — Naming and responsibility baseline

- Status: Accepted / Aceita
- Date: 2026-10-13

## Context / Contexto

After Phase 2, port names differed, generic helpers mixed application and delivery, and one resource owned buckets, connection and the component gallery. Preserve verified behavior while preparing mutation work.

Após a Fase 2, havia inconsistência de nomes e responsabilidades de apresentação/transmissão. Preservar comportamento validado antes das escritas.

## Decision / Decisão

Apply the [naming/responsibility guide](../development/naming-and-responsibilities.md). Keep ADR-0004's packages and one Maven module. Standardize port/adapter, exception, mapper/context and page-view names; separate connection/components resources and download headers/copying; use constructor injection for collaborators. Preserve HTTP routes and existing error/stream behavior. Strengthen architecture tests.

Aplicar o guia, mantendo pacotes e módulo da ADR-0004. Padronizar nomes por papel, separar recursos e helpers de download, usar construtores e preservar contratos HTTP. Reforçar testes de arquitetura.

## Consequences / Consequências

No speculative module/domain/use-case scaffolding. Cohesion still needs review: automated naming checks cannot prove good design. Phase 3 needs separate mutation contracts and security tests. Existing spike evidence remains a historical record; old class names there describe the version tested at that time.

Sem scaffolding especulativo. Coesão depende de revisão; a Fase 3 precisa de contratos de escrita e testes de segurança próprios. Evidências anteriores são históricas e podem citar nomes antigos.
