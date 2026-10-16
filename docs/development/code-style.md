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

First-party JavaScript includes development tooling (`.mjs`) and the small progressive-feedback `app.js`. Pinned ESLint recommended rules and Prettier cover both. Runtime feedback uses DOM text/attributes and local htmx events only; native POSTs/server validation remain authoritative. No inline handlers, HTML injection, eval or secrets; translate user-facing text through Qute data attributes. Fingerprint the script like other bundled assets.

## Java and architecture review

UX-2B exception: [ADR-0015](../decisions/ADR-0015-object-drawer-clipboard.md) allows the bundled htmx to swap only the checked, escaped same-origin object-detail fragment after active-request/status/marker checks. First-party code still must not assign innerHTML, evaluate strings, inject scripts or interpret emulator HTML. Local JavaScript also handles native dialog/focus and user-initiated Clipboard API promises with selectable fallback. / Exceção UX-2B: swap htmx restrito ao fragmento local Qute escapado/verificado; sem innerHTML próprio, eval, scripts ou HTML do emulador. JS local controla diálogo/foco e clipboard por gesto, com fallback selecionável.

Detailed naming/role rules and review checklist: [naming and responsibilities](naming-and-responsibilities.md). / Regras detalhadas de nomenclatura, papéis e revisão no guia vinculado.

- Use two-space indentation and the formatter's 100-column layout. Imports are explicit, sorted and separated from the package declaration.
- Name classes with UpperCamelCase, methods/fields with lowerCamelCase and constants with UPPER_SNAKE_CASE; use names that reveal intent.
- Prefer immutable records/value objects for results and small interfaces for real boundaries. Do not introduce an abstraction without a consumer.
- Keep application contracts independent of Quarkus, Qute, Jakarta and AWS SDK; ArchUnit enforces the existing boundaries.
- Require constructor injection for service collaborators; configuration-property field injection is the documented exception. Avoid global mutable state.
- Catch expected failures at an adapter boundary and translate them into stable results. Do not swallow unexpected exceptions. If an expected exception is intentionally ignored, explain why; never expose SDK details or credentials in UI/logs.
- Tests describe observable behavior, include meaningful error cases and isolate cleanup to resources created by that test.

## Templates and assets

UX-1 integrates Tailwind CSS/CLI 4.3.3 into the product build, without Preflight. Follow [CSS source/output and build rules](frontend-build.md): full literal utility tokens are allowed for migrated shell layout, never assembled from emulator data. Semantic components and theme tokens remain authoritative. Compiler-specific Stylelint exceptions apply only to input files, not ordinary component selectors. UX-0 remains historical spike evidence. / UX-1 integra Tailwind no build, sem Preflight. Seguir guia vinculado: utilitários literais no shell migrado, nunca montados de dados externos; componentes/tokens semânticos continuam. Exceções do compilador restritas às entradas; UX-0 é evidência histórica.

Visual/UX changes follow [UX-0–7](../product/visual-ux-evolution.md) and ADR-0013. Apply UX-1's scoped utility/source/output rules above; preserve semantic tokens and kebab-case component classes outside that migration. Do not copy illustrative routes/bindings or inline event scripts. Maintain native links/forms, external local JavaScript, theme/i18n parity and safe encoded keys. / Evolução UX segue plano/ADR e as regras delimitadas UX-1 acima; manter tokens e componentes semânticos fora da migração, sem copiar APIs ilustrativas/scripts inline, preservando fallback e segurança.

Qute directives are not ordinary HTML. Prettier deliberately excludes `src/main/resources/templates/`; an HTML-only formatter can alter directive/whitespace semantics. Keep block directives on separate lines, indent HTML hierarchy, preserve meaningful inline spaces, and run a checked-template build after edits. Do not claim templates are fully automatically formatted.

Use landmark elements, associated labels, appropriate headings, table headers, visible focus and text alongside status colors. Avoid unescaped/raw output for emulator data. Use semantic CSS tokens rather than duplicating colors in components. Avoid IDs for styling, `!important` except a documented accessibility override, and selectors coupled to unrelated pages.

Markdown line-length limits are disabled for prose/tables; duplicate headings are checked within siblings, and inline HTML is allowed where documentation needs it. These targeted exceptions live in `.markdownlint-cli2.jsonc`; the other default rules remain enabled. Embedded code blocks are not reformatted by Prettier.

## Progressive upload / Upload progressivo

UX-3 follows [ADR-0016](../decisions/ADR-0016-single-file-upload-progress.md) and the [upload contract](s3-upload.md). Bundled htmx may select/swap the checked upload workspace only after its request/target/status/type/marker gates; script execution stays disabled. First-party code manages native progress properties and translated text/attributes, not innerHTML or emulator content. Correlate native upload samples before consuming htmx progress, preserve native multipart/CSRF and never auto-retry a mutation. / UX-3 permite somente swap restrito do workspace verificado, sem scripts/innerHTML próprio; progresso nativo e texto seguro, com correlação de envio, fallback/CSRF e sem retry automático do navegador.

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

Templates Qute exigem HTML semântico, indentação de dois espaços e blocos legíveis; recebem checagem básica de espaços e validação no build. Não são tratados como HTML comum por Prettier. O htmx externo permanece intacto. O JavaScript próprio inclui ferramentas `.mjs` e `app.js` para feedback progressivo, ambos com ESLint fixado/Prettier. O runtime altera apenas texto/atributos e acompanha eventos locais do htmx; POSTs nativos e validação no servidor permanecem autoritativos. Sem handlers inline, HTML dinâmico, eval ou segredos; textos traduzidos chegam por atributos Qute e o script recebe hash de conteúdo.

O CI verifica as regras e não reescreve arquivos. Execute os comandos acima antes de contribuir. Formatação não substitui revisão de nomes, limites de camada, erros, segurança, acessibilidade e comportamento. Node é usado somente pelas ferramentas de desenvolvimento; executar/compilar o produto segue o fluxo Maven/Java/Compose.

## Tool references

- [Spotless Maven](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md)
- [Prettier options](https://prettier.io/docs/options)
- [Stylelint standard configuration](https://github.com/stylelint/stylelint-config-standard)
- [markdownlint rules](https://github.com/DavidAnson/markdownlint/blob/main/doc/Rules.md)
- [PSScriptAnalyzer](https://github.com/PowerShell/PSScriptAnalyzer)
- [Hadolint](https://github.com/hadolint/hadolint)
- [actionlint](https://github.com/rhysd/actionlint)
