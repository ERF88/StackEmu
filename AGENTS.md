# StackEmu contributor instructions

Read `docs/development/code-style.md`, `docs/development/naming-and-responsibilities.md` and applicable ADRs before changing code.

- Use English identifiers, purpose-revealing names and the documented role suffixes.
- Preserve `application`, `adapter.aws`, `adapter.web` boundaries. Do not introduce speculative modules or framework dependencies in application.
- Use constructor injection for collaborators. Keep presentation, SDK translation and HTTP security in their respective adapters.
- Preserve routes and checked-template bindings when renaming. Keep read and mutation contracts separate.
- Keep bilingual documentation synchronized. Do not rewrite historical test evidence as new results.
- Validate with Java 25 Maven verification and the established style checks. Integration requires `STACKEMU_INTEGRATION=true` and local MiniStack; never target production resources.
- Report only tests actually run. Do not claim naming tests prove cohesion, full style compliance or security.

Leia os guias e ADRs; preserve limites, contratos HTTP e docs bilíngues. Não crie arquitetura especulativa nem declare verificações não executadas.
