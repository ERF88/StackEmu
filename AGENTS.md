# StackEmu contributor instructions

Read `docs/development/code-style.md`, `docs/development/naming-and-responsibilities.md` and applicable ADRs before changing code.

For visual/UX work also read `docs/product/visual-ux-evolution.md` and ADR-0013. UX stages are plans, not implemented capabilities. Keep current CSS conventions until UX-0 is accepted; preserve native fallbacks, CSP, CSRF, original-file policy and read/mutation boundaries. Preview, deletion and presigned URLs require their own documented gates; never infer working routes/bindings from the illustrative snippet. / Para UX, ler plano e ADR-0013; preservar fallback/segurança e não tratar exemplos como APIs implementadas.

- Use English identifiers, purpose-revealing names and the documented role suffixes.
- UX-1 CSS build ownership is in `docs/development/frontend-build.md`: literal shell utility tokens only, semantic component tokens retained, no Preflight/CDN or generated CSS edits. / UX-1: seguir guia do build CSS; utilitários literais no shell, componentes semânticos preservados e sem editar gerados.
- Preserve `application`, `adapter.aws`, `adapter.web` boundaries. Do not introduce speculative modules or framework dependencies in application.
- Use constructor injection for collaborators. Keep presentation, SDK translation and HTTP security in their respective adapters.
- Preserve routes and checked-template bindings when renaming. Keep read and mutation contracts separate.
- Keep bilingual documentation synchronized. Do not rewrite historical test evidence as new results.
- Validate with Java 25 Maven verification and the established style checks. Integration requires `STACKEMU_INTEGRATION=true` and local MiniStack; never target production resources.
- Daily development/testing runs on the host JVM and local tooling. Docker runs only the S3 emulator (`docker compose up -d --no-deps ministack`); keep the GUI container stopped. Do not build application images or run container test suites during iterations. Consolidated validation with a new image is reserved for the final validation stage. / Desenvolvimento e testes diários locais via JVM/ferramentas do host; Docker somente para emulador S3, GUI em contêiner parada. Imagem nova e testes completos em contêiner apenas na validação final.
- Report only tests actually run. Do not claim naming tests prove cohesion, full style compliance or security.

Leia os guias e ADRs; preserve limites, contratos HTTP e docs bilíngues. Não crie arquitetura especulativa nem declare verificações não executadas.
