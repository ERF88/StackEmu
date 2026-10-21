# Service catalog — local verification / Central de serviços — verificação local

Date / Data: 2026-10-20 (America/Sao_Paulo). Scope: local uncommitted tree under [ADR-0020](../decisions/ADR-0020-service-catalog-navigation.md), not an immutable release candidate. / Escopo: árvore local sem commit, não candidato imutável de release.

## Implementation / Implementação

Home has a single implemented-service card and “S3 · Not verified” diagnostic link. It contains no buckets, resource metrics, creation/refresh controls or service port. `/s3` owns the bucket workspace; `/buckets` remains a read-only fragment through a narrowly scoped web resource. `/connection` explicitly checks S3 with the existing bucket listing and explains failure/health limits. The contributor gallery remains directly accessible at `/components`, without a product-navigation link; this is not access control.

Início tem cartão do serviço implementado e link de diagnóstico “S3 · Não verificado”, sem buckets, métricas, controles operacionais ou port de serviço. `/s3` contém a área de buckets; `/buckets` mantém fragmento de leitura em recurso web delimitado. `/connection` verifica a listagem S3 sob demanda e explica limites de falha/saúde. Galeria continua diretamente em `/components`, fora do menu; isso não é controle de acesso.

Header context, sidebar, bucket breadcrumbs and form/error returns now distinguish Home from S3. Preferences remain URL-only. No new framework, generic registry, service module, polling/cache/storage, SDK policy, CSP/CSRF relaxation or original-file behavior change. Old `/#bucket-panel` opens Home; `/s3` is the new bucket-list address. Existing first-delivery S3 capability gaps and legal/name/release gates remain.

Cabeçalho, sidebar, breadcrumbs e retornos distinguem Início/S3. Preferências por URL preservadas; sem framework, catálogo genérico, módulo, polling/cache/storage, política SDK ou relaxamento de segurança novo. Arquivo original preservado; favorito antigo abre Início, lista em `/s3`. Lacunas operacionais S3 e gates de nome/jurídico/release continuam.

## Environment and commands / Ambiente e comandos

Windows 11 amd64; Temurin Java 25.0.4.1, Maven 3.9.9, build-only Node 24.10.0, Playwright 1.64.1 using host Chrome 155.0.8059.80. Only MiniStack 1.5.25 runs in Docker; GUI uses the host JVM, loopback 8080 and local emulator 4566 with development credentials. / Somente emulador em Docker; GUI na JVM local com credenciais de desenvolvimento, sem recursos de produção.

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp spotless:apply
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'
# With the verified fast-jar running locally / Com o pacote verificado rodando localmente:
node node_modules/@playwright/test/cli.js test -c playwright.services-local.config.mjs
npm run check
./scripts/smoke.ps1
./scripts/check-style.ps1
git diff --check
```

Use the provisioned `target/frontend/node` toolchain on PATH when Node/npm are absent from the host. Sequence checks after Maven's dependency installation. / Usar toolchain provisionada no PATH quando necessário; não executar checks durante instalação Maven/npm.

## Executed checks / Verificações executadas

- Initial Java run: **110 tests, 3 failures, 1 error**, caused by `/s3` root-route selection returning 404; unavailable-fixture generation also failed. Fixed root ownership with `BucketsResource` at `/s3` and `BucketListFragmentResource` at `/buckets`, retaining existing subroutes. No test disabled or tolerance relaxed. Log: `work/services-maven-first.log`. / Primeira rodada falhou por seleção de rota e fixture; corrigida a propriedade das raízes, sem desabilitar teste.
- After routing correction: **110 Java tests passed, zero failures/errors/skips**, Maven verify/build success at 09:20:11 -03:00. Includes checked Qute, translation/architecture checks, actual MiniStack reads/writes/previews/downloads and the three new service-catalog tests. Log: `work/services-maven-final.log`. / Após correção, 110 passaram, incluindo contratos e integração local.
- The controlled HTTP fixture counts **zero backend requests** for Home while listening and after its server is stopped. Separate checks exercise successful empty/populated listings, denied listing without a numeric count, recovery via `/buckets`, and preference links. This is a real local HTTP test double, not evidence of another emulator's compatibility. / Fixture HTTP local comprova zero chamadas e estados/recuperação; não comprova compatibilidade de outro emulador.
- First host-browser run: **88/88 passed, 5.2 minutes**, zero failed/skipped/flaky, including eight Home/S3 preference combinations, no-JS navigation, unavailable presentation, htmx held/error/retry feedback, axe/responsiveness, density, motion, clipboard, original download, real upload transport and bounded text/image previews. Log/report: `work/services-browser-first.log`, `work/services-browser-first-report.json`. This run preceded the final catalog-frame simplification below. / Primeira rodada local aprovada antes do último ajuste de moldura.
- `npm run check` passed: Prettier, Markdown, document links/index and five checker tests, CSS, JavaScript, two contrast tests and 44 token pairs. `smoke.ps1` passed. PowerShell analysis initially rejected the new literal accented status in an ASCII script; replaced only that matcher with its Unicode regex escape, then smoke/PowerShell analysis passed. No analyzer rule was suppressed. / Checks aprovados; aviso de codificação resolvido no matcher, sem suprimir regra.
- All **80 versioned Linux PNG hashes matched** the preserved V6 proposed references. No baselines were updated. The expanded future Linux fixture suite includes the new service scene (nine scenes / 88 intended PNGs); comparison/new-image validation is deferred, not passed or waived. / 80 hashes intactos; suíte futura ampliada não foi executada nesta etapa.

## Visual review and final rerun / Revisão visual e reexecução final

Agent inspected initial pt-BR dark desktop Home/S3 and pt-BR light mobile Home captures. That exposed a duplicated frame inherited by the catalog section; removed the outer border/background/padding only, leaving the service card frame and adding explicit browser assertions. / Capturas iniciais revelaram moldura duplicada; removida só a externa, mantendo card e adicionando assertions.

After the frame correction, fresh Maven verification passed **110 tests, zero failures/errors/skips**, BUILD SUCCESS at **09:28:51 -03:00**, in 2:08. Log: `work/services-maven-reviewed.log`. The final host-browser run passed **88/88, zero failed/skipped/flaky**, 5.6 minutes, starting **12:29:21.128Z**; log/report: `work/services-browser-final.log`, `work/services-browser-final-report.json`. Do not add repeated runs to the unique test totals. / Após ajuste, nova verificação Java e rodada completa local aprovadas: 110 testes Java e 88 cenários, sem somar repetições.

Agent also inspected final captures: pt-BR dark desktop Home, pt-BR light mobile Home/S3, English dark compact mobile Home and English light desktop Home. The catalog has no outer frame, the single card remains framed, status is service-scoped and long bucket names wrap without page overflow in the reviewed mobile capture. Files remain ignored under `work/services-browser-final/`; they are host diagnostics, not Linux references. / Revisão representativa final cobriu idiomas/temas/densidade/mobile/desktop, sem moldura duplicada ou overflow observado; capturas locais não são baselines Linux.

Final smoke/PowerShell analysis passed against the rebuilt GUI. The verified host fast-jar remains running on loopback 8080 (owned Java PID 14104), using local MiniStack; GUI container remains stopped. Documentation-only closing edits are checked with the final style/documentation run before handoff. / Smoke/análise aprovados no pacote final; GUI JVM local permanece disponível, sem contêiner GUI. Edições finais só documentais passam pelo último check antes da entrega.

These are representative agent inspections, not maintainer design acceptance, exhaustive pixel review, user research or human assistive-technology certification. / Inspeção representativa do agente não equivale a aceite do mantenedor, pesquisa ou certificação de acessibilidade humana.

## Remaining gates / Gates restantes

Maintainer validation of the new service hierarchy; human accessibility and independent onboarding; deliberate pinned-Linux baseline reconciliation and new-image validation at the separate final stage; immutable source/release review and legal/name validation. No new application image, container browser suite, streaming-container run, vulnerability scan, commit, push, tag or publication is performed by this iteration. Earlier V6 results remain evidence only for their own prior tree/image. / Aceite do novo fluxo, revisões humanas, reconciliação Linux/imagem final e gates legais/release permanecem. Sem executar imagem, suíte de contêiner, streaming/scan ou publicação nesta iteração; V6 anterior não comprova o código atual.
