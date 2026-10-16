# Naming and responsibilities / Nomes e responsabilidades

Status: project baseline after Phase 2. These are StackEmu conventions, not a claim that one architecture is universally best. Read with [file conventions](code-style.md) and [ADR-0006](../decisions/ADR-0006-naming-responsibilities.md).

## English — naming

Use English identifiers and domain vocabulary. Packages are lowercase, classes/interfaces/records UpperCamelCase, methods/fields lowerCamelCase and constants UPPER_SNAKE_CASE. A public top-level type matches its filename. Avoid ambiguous abbreviations, Hungarian prefixes, `I` interface prefixes, `Impl`, `Manager`, `Helper` and generic `Utils` names. Recognizable technology names such as `S3`, `AWS` and `HTTP` may be used; follow the existing `AwsS3ReadAdapter` spelling.

Names express purpose, not just technology. Ports end in `Port`; SDK implementations use `Aws…Adapter`; HTTP endpoints end in `Resource`; presentation mapping uses `…ViewMapper`; rendered page records end in `PageView`; application exceptions end in `Exception`. Do not introduce these roles merely to satisfy a suffix. `BucketListSnapshot` models the listing and connectivity result, not a rendered page. Nested port records such as `ObjectPage` are application data, not Qute models.

Methods use precise verbs: `listBuckets`, `getBucketDetails`, `listObjects`, `getObjectDetails`, `openDownload`, `showBucket`, `downloadObject`, `toBucketPage`. Accessors such as `region()` and record accessors remain nouns. `get…` may perform network I/O here; document that behavior at the port. `openDownload` transfers ownership of a closable stream. HTTP paths, translation keys and checked-template names are separate contracts; Java renaming does not silently rename routes or catalogs.

## English — boundaries and responsibility

Keep the root `io.github.erf88.stackemu` and the implemented packages `application`, `adapter.aws` and `adapter.web`. One Maven module remains intentional. Do not rename to speculative `core`, `common`, `util` or an empty domain layer. A second real service can justify feature-oriented packages/modules through a new ADR, not an automatic migration.

- Application: immutable data, output ports and stable errors; no SDK, HTTP, CDI, Qute or localized strings. Add domain/use-case types only for actual rules or orchestration.
- AWS adapter: SDK requests/results, SDK failure translation and resource lifetime; never template/HTTP response handling. Client creation/lifecycle stays with the existing Quarkiverse integration.
- Web resources: request validation, invocation of ports, response/template selection and HTTP errors; no SDK calls or business rules. Split resources by screen/capability when reasons to change differ, not by line count alone.
- View mapper/context: localized display data, links, language and theme. Do not perform I/O. Keep attachment/header security outside page mapping.
- Download helpers: bounded stream transfer and HTTP attachment policy. Stream copying belongs to delivery, not the application contract; the caller still owns closure.

Constructor injection is required for service collaborators: explicit dependencies, private final fields and directly constructible unit tests. Quarkus accepts a single constructor without explicit `@Inject`; do not make normal-scoped beans final. Configuration-property field injection is a documented exception (`configuredPageSize`); it is not permission for mutable service injection. Keep stateless helpers narrowly named and prohibit mutable global state.

Single responsibility means one coherent reason to change, not one method per class. No arbitrary class/method size thresholds, one-interface-per-class rule, generic repository layer without persistence, or DTO/entity/service proliferation. Prefer composition and focused contracts. Read and mutation contracts must remain separate when Phase 3 arrives; do not put upload validation or CSRF in `S3ReadPort`.

## Português — diretrizes

Identificadores em inglês, com vocabulário do domínio; pacotes minúsculos, tipos UpperCamelCase, métodos/campos lowerCamelCase e constantes UPPER_SNAKE_CASE. O arquivo acompanha o tipo público. Evitar abreviações ambíguas, prefixo `I`, sufixo `Impl` e nomes genéricos que escondem responsabilidade. Os sufixos `Port`, `Adapter`, `Resource`, `ViewMapper`, `PageView` e `Exception` identificam papéis existentes, não justificam criar camadas vazias.

Métodos descrevem ações específicas. Accessors de records continuam substantivos. Documentar I/O e transferência da responsabilidade por streams. Rotas HTTP, nomes de templates e traduções são contratos separados e devem ser preservados em refatorações.

Manter um módulo Maven e os pacotes atuais. Aplicação contém contratos/dados/erros sem framework; AWS traduz SDK; recursos web tratam entrada/saída HTTP; mapper/contexto montam apresentação sem I/O; helpers de download tratam transmissão/cabeçalhos. Uma classe pode ter vários métodos relacionados, mas não deve reunir motivos de mudança independentes. Injeção de colaboradores por construtor, campos privados finais e testes sem alterar campos internos; configuração por campo é exceção explícita.

Criar use cases/domain apenas para regras reais, sem arquitetura especulativa. Separar leituras e escritas na Fase 3. CSRF, cabeçalhos e interpretação HTTP pertencem à borda web; validações de negócio pertencem à aplicação/domínio quando existirem. Registrar decisões de expansão em ADR.

## Review checklist / Checklist de revisão

1. Can the name and package explain the responsibility? / Nome e pacote explicam a responsabilidade?
2. Are dependencies explicit and pointing inward? / Dependências explícitas e orientadas aos contratos?
3. Is the operation read-only or a mutation, and who owns streams/files? / Leitura ou escrita, e quem encerra recursos?
4. Is data application data or formatted presentation? / Dado de aplicação ou apresentação formatada?
5. Are validation, error translation and security at the correct boundary? / Validação, erros e segurança na borda correta?
6. Are routes/templates preserved, tests meaningful and docs bilingual? / Rotas/templates preservados, testes relevantes e docs bilíngues?

## Enforcement and limits / Automação e limites

Scoped preview lifecycle exception: [ADR-0018](../decisions/ADR-0018-bounded-text-preview.md) derives an adapter-owned bounded-transport client from the public configuration of the Quarkiverse-managed client. Close only the owned client on destruction; never mutate/close the ordinary client or explicitly close its credentials provider. No SDK/lifecycle dependency crosses into application/web; advanced configuration compatibility remains unverified. / Exceção delimitada: preview deriva cliente próprio da configuração pública do gerenciado, fechando só o próprio sem alterar/fechar cliente comum/provider. Sem SDK/lifecycle nas camadas aplicação/web; configurações avançadas não verificadas.

Spotless formats Java, not semantic responsibility. `ArchitectureTest` checks application/web/SDK dependency boundaries, AWS-to-web isolation, package/type/method naming shape, role placement and absence of service field injection. Those checks do not prove cohesion, SOLID compliance, full Google Style compliance or absence of security defects; the checklist and review remain necessary. Templates are checked during Quarkus build. Tests must be updated with behavior, not weakened to allow a forbidden dependency.

Spotless não valida responsabilidade semântica. Os testes verificam limites e um subconjunto de convenções, sem certificar SOLID ou Google Style integral. Revisão humana e testes de comportamento continuam obrigatórios.

## References / Referências

- [Google Java Style — naming](https://google.github.io/styleguide/javaguide.html#s5-naming): identifier conventions; not an architecture prescription.
- [Quarkus 3.40 CDI](https://quarkus.io/version/3.40/guides/cdi-reference/): injection and normal-scope mechanics.
- [ArchUnit guide](https://www.archunit.org/userguide/html/000_Index.html): dependency rules and executable architecture tests.
