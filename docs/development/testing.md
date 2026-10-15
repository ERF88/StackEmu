# Testing strategy / Estratégia de testes

Phase 2 coverage/commands: [guide](phase-2.md), [evidence](../spikes/phase-2-results.md). / Cobertura e comandos da Fase 2 nos documentos vinculados.

## E6 browser references / Referências de navegador E6

Read [component accessibility contracts](component-accessibility.md) and the [visual regression workflow](visual-regression.md). Baselines use deterministic Qute presentation fixtures and the CI Playwright container; real SDK flows remain separate. / Consultar contratos de componentes e fluxo visual. Baselines usam fixtures Qute e container do CI; fluxos SDK reais permanecem separados.

## Test pyramid / Pirâmide

| Layer         | Planned tool                  | Scope                                    |
| ------------- | ----------------------------- | ---------------------------------------- |
| Domain        | JUnit 5                       | Value objects and rules                  |
| Application   | JUnit 5 with fake ports       | Use cases, errors, limits                |
| AWS adapter   | Integration tests             | Real emulator operations                 |
| Web           | `@QuarkusTest` + REST Assured | Status, HTML, fragments, CSRF            |
| Architecture  | ArchUnit                      | Forbidden dependency direction           |
| E2E           | Playwright                    | Essential user flow                      |
| Accessibility | Automated tool + keyboard     | Detectable issues plus manual navigation |
| Container     | Smoke test                    | Startup, non-root user, port, health     |
| Compatibility | Progressive matrix            | Differences by emulator/resource         |

Implemented commands/evidence are recorded in the phase guides; browser tooling and current limits: [Phase 4](phase-4.md). JUnit/REST Assured/ArchUnit and MiniStack are established; browser/axe checks do not replace manual accessibility review. / Comandos e evidências nos guias; testes de navegador não substituem revisão manual.

## Critical cases / Casos críticos

Test correct host/Compose endpoints; unavailable emulator during navigation; empty bucket/absent tags; nested prefixes and pagination; limit/over-limit uploads; large download without full in-memory read; special spaces/Unicode/slashes; rejected CSRF; HTML-like payload displayed inertly; redacted secret logs; >1,000 objects; streaming-start failures; missing i18n keys; and a slow counter that does not break dashboard rendering.

## Evidence discipline / Disciplina de evidência

Final redesign commands, checklist coverage, screenshots and outstanding acceptance limits: [E7 evidence](../spikes/redesign-e7-results.md). / Comandos finais do redesenho, cobertura do checklist, capturas e limites de aceite: evidência E7 vinculada.

Pin emulator versions. Record test environment, operations, skipped tests with reason, and limitations in the compatibility matrix. A passing connection test is not an emulator certification. Automated accessibility checks complement rather than replace manual keyboard/focus review.
