# Coding conventions / Convenções de código

These are the StackEmu defaults. They use established language tools while preserving Qute semantics and the project's small JVM build. Formatting is automated; design and behavior still require review. Tool versions are pinned in `pom.xml`, `package-lock.json` and the workflow. Update them deliberately and rerun checks.

## File conventions

| File type              | Standard and checks                                                                                                                                                      |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Java                   | [Google Java Style](https://google.github.io/styleguide/javaguide.html), google-java-format 1.35.0 via Spotless; explicit imports and unused-import removal              |
| Maven/XML              | Spotless SortPom; consistent element order, two-space indentation, declared plugin versions and platform-managed dependencies                                            |
| Qute/HTML              | Semantic HTML, two-space indentation, readable template blocks, escaped expressions, accessible labels and checked Qute compilation; Spotless checks whitespace/newlines |
| CSS                    | Prettier and Stylelint standard configuration; semantic tokens, kebab-case classes, modern media queries, visible focus and responsive components                        |
| YAML/JSON/JSONC        | Prettier; two spaces; actionlint checks workflow expressions/actions, Compose validates its configuration                                                                |
| Markdown               | Prettier and markdownlint; meaningful headings, fenced code with a language, descriptive links and bilingual updates                                                     |
| PowerShell             | PSScriptAnalyzer 1.24.0 plus its formatter, four spaces, approved verbs for future functions, explicit errors and predictable cleanup                                    |
| Dockerfile             | Hadolint, explicit image tags, multi-stage build, exec-form entrypoint, non-root runtime, small build context                                                            |
| Properties             | UTF-8, semantic keys, translation parity tests; no secrets; whitespace/newline checks                                                                                    |
| Editor/Git files       | EditorConfig and Git attributes: UTF-8, LF, final newline, spaces, no trailing whitespace                                                                                |
| Third-party JavaScript | Vendored htmx is excluded from formatting; keep upstream contents and notices intact                                                                                     |

First-party JavaScript currently exists only in browser-test tooling (`.mjs`), with pinned ESLint recommended rules and Prettier. Runtime UI still uses Qute/CSS and vendored htmx. Avoid inline event handlers and translate any future runtime user-facing strings.

## Java and architecture review

Detailed naming/role rules and review checklist: [naming and responsibilities](naming-and-responsibilities.md). / Regras detalhadas de nomenclatura, papéis e revisão no guia vinculado.

- Use two-space indentation and the formatter's 100-column layout. Imports are explicit, sorted and separated from the package declaration.
- Name classes with UpperCamelCase, methods/fields with lowerCamelCase and constants with UPPER_SNAKE_CASE; use names that reveal intent.
- Prefer immutable records/value objects for results and small interfaces for real boundaries. Do not introduce an abstraction without a consumer.
- Keep application contracts independent of Quarkus, Qute, Jakarta and AWS SDK; ArchUnit enforces the existing boundaries.
- Require constructor injection for service collaborators; configuration-property field injection is the documented exception. Avoid global mutable state.
- Catch expected failures at an adapter boundary and translate them into stable results. Do not swallow unexpected exceptions. If an expected exception is intentionally ignored, explain why; never expose SDK details or credentials in UI/logs.
- Tests describe observable behavior, include meaningful error cases and isolate cleanup to resources created by that test.

## Templates and assets

Qute directives are not ordinary HTML. Prettier deliberately excludes `src/main/resources/templates/`; an HTML-only formatter can alter directive/whitespace semantics. Keep block directives on separate lines, indent HTML hierarchy, preserve meaningful inline spaces, and run a checked-template build after edits. Do not claim templates are fully automatically formatted.

Use landmark elements, associated labels, appropriate headings, table headers, visible focus and text alongside status colors. Avoid unescaped/raw output for emulator data. Use semantic CSS tokens rather than duplicating colors in components. Avoid IDs for styling, `!important` except a documented accessibility override, and selectors coupled to unrelated pages.

Markdown line-length limits are disabled for prose/tables; duplicate headings are checked within siblings, and inline HTML is allowed where documentation needs it. These targeted exceptions live in `.markdownlint-cli2.jsonc`; the other default rules remain enabled. Embedded code blocks are not reformatted by Prettier.

## Commands

JDK 25 and Maven:

```sh
mvn -B -ntp spotless:apply
mvn -B -ntp spotless:check
mvn -B -ntp clean verify
```

The check runs during Maven `validate`; normal builds reject formatting drift. Applying formatting is an explicit local action, never an automatic CI rewrite.

Development tooling with Node 24.10.0 (only for style tools, not the application):

```sh
npm ci --ignore-scripts --no-audit --no-fund
npm run format
npm run check
```

Without host Node, run the same commands in a container. PowerShell example:

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm ci --ignore-scripts --no-audit --no-fund
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run format
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run check
Save-Module -Name PSScriptAnalyzer -RequiredVersion 1.24.0 -Path work/modules -Force
./scripts/check-style.ps1 -Fix
./scripts/check-style.ps1
docker run --rm -v "${PWD}:/repo" -w /repo hadolint/hadolint:v2.12.0 hadolint Dockerfile
docker run --rm -v "${PWD}:/repo" -w /repo rhysd/actionlint:1.7.7
docker compose config --quiet
```

Generated output, `node_modules`, build directories and vendor assets are excluded. Never run tools against credentials or unrelated directories. GitHub actions are pinned to verified release commit SHAs. Build/runtime bases and the Phase 5 scanner use verified digests; other tool images retain fixed version tags. Digest updates require verification and scanning. / Bases e scanner usam digests; demais ferramentas mantêm tags fixadas, sem promessa de imutabilidade integral.

## Convenções em português

Java segue Google Java Style, com Spotless/google-java-format e imports explícitos. XML Maven segue SortPom. CSS, YAML, JSON e Markdown usam Prettier; CSS recebe Stylelint e Markdown recebe markdownlint. Workflows são verificados por actionlint, Compose por sua validação nativa e Dockerfile por Hadolint. PowerShell usa PSScriptAnalyzer, com quatro espaços. Todos os arquivos próprios usam UTF-8/LF.

Templates Qute exigem HTML semântico, indentação de dois espaços e blocos legíveis; recebem checagem básica de espaços e validação no build. Não são tratados como HTML comum por Prettier. O htmx externo permanece intacto. O JavaScript próprio é restrito às ferramentas de teste `.mjs`, com ESLint fixado e Prettier; não adiciona runtime JS próprio.

O CI verifica as regras e não reescreve arquivos. Execute os comandos acima antes de contribuir. Formatação não substitui revisão de nomes, limites de camada, erros, segurança, acessibilidade e comportamento. Node é usado somente pelas ferramentas de desenvolvimento; executar/compilar o produto segue o fluxo Maven/Java/Compose.

## Tool references

- [Spotless Maven](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md)
- [Prettier options](https://prettier.io/docs/options)
- [Stylelint standard configuration](https://github.com/stylelint/stylelint-config-standard)
- [markdownlint rules](https://github.com/DavidAnson/markdownlint/blob/main/doc/Rules.md)
- [PSScriptAnalyzer](https://github.com/PowerShell/PSScriptAnalyzer)
- [Hadolint](https://github.com/hadolint/hadolint)
- [actionlint](https://github.com/rhysd/actionlint)
