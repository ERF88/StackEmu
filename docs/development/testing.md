# Testing strategy / Estratégia de testes

Phase 2 coverage/commands: [guide](phase-2.md), [evidence](../spikes/phase-2-results.md). / Cobertura e comandos da Fase 2 nos documentos vinculados.

## Current local workflow / Fluxo local atual

User direction on 2026-10-16: run only MiniStack S3 in Docker (`docker compose up -d --no-deps ministack`); keep the GUI container stopped. Develop and test the application on the host with Java 25, Maven and local browser/style tooling. The host GUI uses `http://127.0.0.1:8080` and the S3 endpoint uses `http://localhost:4566`. Start a verified packaged build with `java -jar target/quarkus-app/quarkus-run.jar`; rebuild locally after production changes. No application-image rebuilds or container test suites during iterations. Reserve a new image and consolidated Docker validation for the final validation stage. Existing Linux-container browser instructions below describe that deferred workflow, not the daily default. The Linux-only browser helper is not a Windows launcher; adapting its upload-response fixtures for host execution requires separate implementation/verification. Do not silently skip dependent checks or regenerate Windows snapshots over Linux references.

Direção do usuário em 2026-10-16: Docker somente para MiniStack S3; GUI em contêiner parada. Desenvolvimento/testes no host com Java 25, Maven e ferramentas locais. GUI e endpoint S3 nos endereços acima; executar JAR verificado, recompilando localmente após alterações de produção. Sem rebuild de imagem ou suítes em contêiner nas iterações; imagem nova e validação consolidada somente ao final. Instruções históricas Linux abaixo não são o padrão diário. Helper Linux não inicia no Windows; adaptar fixtures de retenção de resposta exige implementação/verificação própria. Não ignorar checks dependentes nem substituir baselines Linux por capturas Windows.

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

## Drawer and clipboard / Drawer e clipboard

UX-2B contracts/evidence: [explorer](s3-explorer.md), [execution](../spikes/ux-2b-results.md). In the pinned Linux browser container, `node scripts/run-browser-local.mjs` runs the normal suite through a fixed test-only loopback forwarder to the host GUI. Add `drawer.spec.mjs` for focused checks. This preserves response headers/CSP without insecure-origin flags and allows the real Clipboard API test; the ordinary host.docker.internal origin is not trusted loopback. Without a secure context, only the native clipboard test is explicitly skipped, while denial/absence/manual fallback remain tested. API stubs and stale-event simulations are labelled separately. Never present a skipped native case as a pass or timing via the forwarder as a performance benchmark.

Contratos/evidências nos links. No container Linux fixado, o helper acima executa suíte via loopback de teste para GUI local do host; arquivo opcional restringe à fatia. Preserva CSP/cabeçalhos, sem flags de origem insegura. Sem contexto seguro, teste nativo é ignorado explicitamente, mantendo testes de falha/ausência/fallback; não confundir simulações, skips e sucessos reais, nem tempos com benchmark.

## Single-file upload / Upload individual

Upload UI tests require the loopback helper, not direct `npm run test:browser`, because processing assertions hold only the real upstream response through test-only control endpoints. The CI host-network mode (`STACKEMU_BROWSER_HOST_NETWORK=true`) binds the proxy to 127.0.0.2:8080 and forwards to the GUI at 127.0.0.1:8080; the default Docker Desktop mode forwards to host.docker.internal. Both targets are fixed, with no arbitrary forwarding. CI workflow changes are not evidence of a GitHub-hosted run. / Testes de upload exigem helper para reter resposta real sem interceptar envio. Modo CI usa loopbacks distintos; modo Desktop usa host.docker.internal. Alvos fixos, sem proxy arbitrário; mudança de workflow não comprova execução no GitHub.

Run `upload-transport.spec.mjs` for the real 1 MiB htmx/native progress-direction spike, and `upload.spec.mjs` for UI drop/picker/progress/recovery contracts. The [UX-3 record](../spikes/ux-3-results.md) distinguishes real transfers/hash/MIME/CSRF/file limits from held-response, shortened-timeout and synthetic-progress fixtures. Never count 100% transport as server-confirmed storage, silently retry a mutation or replace a failed original-key assertion with another key. / Specs/registro vinculados separam transferência real e simulações; 100% não confirma gravação, sem retry automático ou expectativa relaxada para outra chave.

## Evidence discipline / Disciplina de evidência

UX-5B image integration follows [ADR-0019](../decisions/ADR-0019-raster-preview-isolation.md) and [local execution evidence](../spikes/ux-5b-results.md). Run host preview slices with `node node_modules/@playwright/test/cli.js test -c playwright.preview-local.config.mjs`: that config uses installed host Chrome and only text/image preview specs, not Linux upload-proxy or snapshot suites. Host browser version is recorded in output; it is not the pinned Linux reference browser. Do not update Linux baselines from this run. Java checks cover metadata/byte bounds, abort, real stalled/trickling image transport, structural refusals, sanitized PNG, original attachment and status/header behavior. Browser checks exercise the locally packaged app, hence the extracted worker from its packaged classloader. Held-image loading is explicitly a network-gating fixture. / Integração UX-5B tem contrato/evidência próprios. Config local usa Chrome instalado e somente previews, não helpers Linux ou snapshots; registrar versão e não atualizar baselines. Java cobre leitura/recusas/transporte/headers/original; navegador usa pacote local e seu classloader. Loading retido é fixture de rede, não decoder lento real.

Historical UX-5B worker-only slice: `RasterPreviewProcessTest` executes an extracted JDK-only worker and tests refusal/output/resource lifecycle with labelled process fixtures for timeout/interruption/environment/diagnostics. Its deterministic occupied-slot test is not a concurrency benchmark. [Worker evidence](../spikes/ux-5b-worker-results.md) predates HTTP activation; the integration record above owns current status. / Fatia histórica do worker testa extração/ciclo de vida e fixtures explícitas, sem benchmark de concorrência. Registro anterior precede HTTP; evidência de integração acima define estado atual.

UX-5A: `preview.spec.mjs` exercises real native text/JSON-as-text preview against local MiniStack, hostile key/body escaping, exact attachment hash, no-JS, refusal/missing-object behavior, themes/languages and keyboard/axe. Java tests independently cover strict UTF-8/metadata/byte bounds, abort/no-drain and real loopback stalled/trickling transport deadlines. Run through the normal loopback helper; only explicitly reviewed detail-link baselines may be updated, never unrelated snapshots. [Contract](../decisions/ADR-0018-bounded-text-preview.md), [evidence](../spikes/ux-5a-results.md). / Preview testa conteúdo real/hostil, download idêntico, fallback/recusas/erros/temas/idiomas/teclado/axe; Java cobre bytes/abort e transporte loopback lento. Distinguir fixture de transporte e compatibilidade real; atualizar somente referências deliberadamente revisadas.

UX-4: run `notifications.spec.mjs` through the same helper for real bucket/folder/upload success, native no-JS, focus/live-region and keyboard dismissal checks in both languages/themes. Existing upload fault tests also assert no success notice during processing, validation, conflict, network/body-limit or CSRF failure. Lifecycle events are simulated and screen-reader announcements require human review. [Contract](../decisions/ADR-0017-operation-notifications.md), [fresh evidence](../spikes/ux-4-results.md). / UX-4 testa operações reais, sem JS, idiomas/temas, foco/live e fechar por teclado; falhas não exibem sucesso. Eventos de ciclo de vida são simulados, anúncios exigem revisão humana.

Final redesign commands, checklist coverage, screenshots and outstanding acceptance limits: [E7 evidence](../spikes/redesign-e7-results.md). / Comandos finais do redesenho, cobertura do checklist, capturas e limites de aceite: evidência E7 vinculada.

Pin emulator versions. Record test environment, operations, skipped tests with reason, and limitations in the compatibility matrix. A passing connection test is not an emulator certification. Automated accessibility checks complement rather than replace manual keyboard/focus review.
