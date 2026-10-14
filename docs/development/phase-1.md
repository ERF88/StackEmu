# Phase 1 / Fase 1

## Delivered behavior / Comportamento entregue

- `/`: read-only bucket dashboard, connection status and next action.
- `/connection`: effective S3 endpoint/region, hidden credentials, environment/restart guidance and safe ListBuckets connection test.
- `/components`: component gallery independent of emulator availability.
- All pages use one Qute layout, English/pt-BR message catalogs, semantic CSS tokens, light/dark themes, visible keyboard focus and a skip link.
- Locale is negotiated with `Accept-Language` unless explicitly overridden by `lang=en` or `lang=pt-BR`. Theme is selected using `theme=light` or `theme=dark`. Navigation retains these values; preferences are not persisted across unrelated visits.

As páginas compartilham layout, traduções e tokens. A tela de conexão mostra a configuração efetiva sem expor credenciais e explica que mudanças exigem reinício. A galeria funciona sem o emulador. Idioma/tema acompanham os links, mas ainda não são persistidos em cookie.

## Structure / Estrutura

One Maven module remains sufficient. `application` contains framework-free immutable contracts. `adapter.aws` translates SDK failures into a connection result. `adapter.web` owns routes/view models and locale selection. Client lifecycle/configuration remains managed by the existing Quarkiverse extension; no duplicate factory or plugin discovery layer is introduced.

ArchUnit checks prohibit framework/SDK/adapter dependencies in application contracts, and AWS SDK/AWS adapter imports in the web adapter. The translation test compares the two physical catalogs to detect missing keys without bundle fallback hiding omissions.

## Configuration / Configuração

The same `quarkus.s3.endpoint-override` and `quarkus.s3.aws.region` effective properties used by the extension are read by the view. `STACKEMU_ENDPOINT`, `STACKEMU_REGION`, `STACKEMU_ACCESS_KEY` and `STACKEMU_SECRET_KEY` map to these properties in `application.properties`; normal Quarkus override precedence still applies.

Secrets are never passed to templates. Embedded endpoint user-info, query and fragment are removed from the display. Test connection checks a read operation; an HTTP connection alone is insufficient. A failed SDK operation is shown as unavailable; detailed failure taxonomy is a future improvement.

## Run and verify / Executar e verificar

Use `docker compose up --build -d` and open `/connection?lang=pt-BR&theme=light`.
For a clean local build on JDK 25, run `mvn -B -ntp clean verify` with `STACKEMU_INTEGRATION=true` and MiniStack running. See [Phase 0](phase-0.md) for platform-specific environment syntax and stop/data handling.

## Limits / Limites

No persistent profiles, user authentication, endpoint editing, mutations or full S3 operations. Automated HTML tests and manual visual/keyboard inspection do not establish full WCAG compliance. Theme/locale persistence and a more detailed connection-error taxonomy are deferred.
