# Contributing to StackEmu

[Português (Brasil)](CONTRIBUTING.pt-BR.md)

StackEmu has a Phase 1 foundation. Consult the recorded validation before making implementation or compatibility claims.

1. Read the [roadmap](docs/product/roadmap.md), applicable [ADRs](docs/decisions/), and [Code of Conduct](CODE_OF_CONDUCT.md).
2. Open an issue or discussion for material design changes before implementing them.
3. Keep a change narrow, document observable behavior and limitations, and add/adjust tests when code exists.
4. Never add credentials, test results you did not run, or claims of emulator compatibility without reproducible evidence.
5. Keep English and pt-BR documentation synchronized where the plan requires bilingual documentation.

Follow the [coding conventions and commands](docs/development/code-style.md). Run `mvn spotless:apply` and `npm run format` locally, then `npm run check` and `mvn clean verify`. CI checks formatting, Markdown/CSS, PowerShell, Dockerfile, workflows, Compose, translations, architecture, integration and smoke tests. Include only validation you actually executed.

Use conventional, descriptive commits where practical. By contributing, you agree that contributions are licensed under the MIT License.
