# Modules / Módulos

## Proposed boundaries / Limites propostos

| Boundary         | Responsibility                                       | Notes                                           |
| ---------------- | ---------------------------------------------------- | ----------------------------------------------- |
| `core`           | Pure shared contracts/models                         | No framework or SDK dependencies                |
| `aws-connection` | Effective config, client factory, capability probes  | Environment is source of truth in MVP           |
| `module-s3`      | S3 domain, use cases, web and AWS adapters           | Initial real vertical slice                     |
| `app`            | Quarkus bootstrap, composition, dashboard, packaging | Owns runtime wiring                             |
| `ui-kit`         | Shared layout/components/CSS                         | May start in `app`; extract only when justified |

This five-boundary shape is conditional. Start with the smallest number of Maven modules that preserves meaningful boundaries; do not build an empty generic platform.

## `module-s3` layers / Camadas

- `domain`: value objects such as bucket names/object keys, rules, domain errors.
- `application`: use cases and output ports.
- `adapter.in.web`: REST resources, Qute templates, and view models.
- `adapter.out.aws`: AWS SDK implementations of ports.

## Shared contracts / Contratos compartilhados

Possible contracts are `ServiceModule`, `ResourceCounter`, `EmulatorProfile`, `Page<T>` only where semantics are truly shared, and stable application errors. Continuation tokens and service-specific pagination stay specific when APIs differ.

## Module discovery / Descoberta

Only after a second real module exists, a CDI `ServiceModule` contract may provide menu metadata and counters. Any library module must validate CDI discovery/Jandex indexing and use exclusive template prefixes such as `templates/s3/`. Modules must not register arbitrary routes without a convention and test.
