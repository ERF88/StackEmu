# V6 consolidated validation / Validação consolidada V6

Date / Data: 2026-10-17 (America/Sao_Paulo). The maintainer reported V4 validated and authorized continuation to this final visual-cycle verification. V5 self-hosted fonts stays deferred under D1. / Mantenedor validou V4 e autorizou esta etapa final do ciclo visual; V5 segue adiada.

## Scope and identity / Escopo e identidade

Validate the current local working tree, based on committed `486f807` with uncommitted documentation, V1–V4 and preview-transport changes. This is not evidence for an immutable candidate commit or a published release, and not completion of all operational S3 features. Preserve emulator volumes and unrelated data; fixtures own exact UUID-named targets only. No commit/tag/push/publication is authorized by this verification. / Validar árvore local com alterações não commitadas; não equivale a candidato imutável, release ou entrega S3 operacional completa. Preservar volumes/dados alheios e limpar só fixtures próprias; sem commit/tag/push/publicação.

The previous local image is retained as `stackemu:pre-v6-20261017`. The 80 existing PNG baselines were copied unchanged to ignored `work/v6-baselines-before-20261017` before any comparison. A fresh no-cache build uses existing pinned Dockerfile/base/package decisions. No tolerance, assertion deadline or vulnerability suppression may be relaxed. Human V4 visual acceptance does not automatically certify changed Linux images; inspect differences before any deliberate baseline reconciliation. / Imagem anterior e 80 baselines preservadas; build sem cache usa decisões fixadas existentes. Sem relaxar tolerâncias/prazos/scan; aceite V4 não certifica automaticamente capturas Linux.

## Execution record / Registro de execução

Clean host Java 25 verification passed at 07:02:22: 107 tests, zero failures/errors/skips (`work/v6-maven-clean.log`), including Spotless, checked Qute templates, local MiniStack integration and SBOM/fast-jar generation. This was Windows-host Java, not a Linux-container JUnit run. Hadolint (both Dockerfiles), actionlint, PowerShell analysis and Compose configuration checks passed. / Verify limpo no host passou 107 testes sem falhas/erros/ignorados; não equivale a JUnit Linux. Checks de Dockerfiles/workflow/PowerShell/Compose passaram.

Fresh no-cache image: `sha256:22d5080a5cc7d14a0b4779004cfd07d016a49a808ace28b374e0ead0e5e12c97`, Linux/amd64, 357164059 bytes, created `2026-10-17T10:00:39.991639681Z` (`work/v6-image-build.log`). `verify-container.ps1`, `smoke.ps1` and `verify-writes.ps1` passed against it: UID 10001, read-only root, dropped capabilities, no escalation/socket, loopback, temporary/memory/PID limits, licenses/no Node runtime, localized shell/connection and real bucket/marker/upload/download SHA-256/CSRF refusal. These checks do not certify all resource/concurrency or native-memory behavior. / Imagem nova identificada; scripts passaram com controles e operações reais, sem certificar capacidade/concorrência/memória nativa.

### Browser sequence and reviewed baselines / Navegador e baselines revisadas

Pinned container `mcr.microsoft.com/playwright:v1.64.1-noble` ran Chromium **156.0.8078.4**, not host Chrome 155. Loopback test helper preserved CSP and enabled native clipboard/held-response upload cases. First full comparison, without updates, ended **67 failed, 85 passed, 5 not run (11.7m)** (`work/v6-browser-first.log`). It is not a passing run: 64 visual-scene failures compared V1–V4 against preceding references; density setup failed against container-local 127.0.0.1:4566, preventing five remaining density cases; two shell cases still expected two selected preferences after density added a third.

Container fixado executou Chromium 156, distinto do Chrome do host. Primeira rodada não passou: 67 falhas, 85 aprovados, cinco não executados. Sessenta e quatro falhas visuais frente a referências anteriores; fixture de densidade apontava para loopback interno e impediu cinco casos; dois casos de shell esperavam só duas preferências.

Only test contracts changed: density fixture now uses the configured emulator endpoint with the same local-only allowlist as other specs. Shell asserts exactly one current selection in each theme/language/density group and the comfortable default, instead of an obsolete aggregate count. No product code, HTTP/security contract, deadline or screenshot tolerance was weakened; these test-only changes do not change the built application image.

Mudanças limitadas aos testes: endpoint configurado com allowlist local e assert de uma seleção por grupo/padrão confortável. Sem alterar runtime, contratos, prazos ou tolerâncias; imagem da aplicação continua a mesma.

Separately generated 80 proposed PNGs through the pinned Linux visual spec in ignored work: **64 tests passed (2.0m)** (`work/v6-capture.log`), including gallery hover/focus and actual asset-hash/overflow/keyboard guards. A temporary capture-path token was literal rather than expanded; the generated directory was normalized inside work after exact-path/count validation, without changing PNG bytes. Versioned baselines were untouched during capture/review. The agent inspected all 80 labelled before/proposed layout pairs on ten offline contact sheets (`work/v6-review/sheet-01.png` through `sheet-10.png`) and directly inspected eleven proposed PNGs across all eight scene types plus hover/focus. This is agent layout review with spot checks, not exhaustive glyph-by-glyph review, human accessibility acceptance or a new maintainer Linux-image review.

Propostas geradas separadamente: 64 testes/80 PNGs passaram. Token temporário de caminho ficou literal; diretório normalizado em work após validação, sem mudar bytes. Baselines versionadas intactas até revisão. Agente inspecionou 80 pares em dez pranchas e onze PNGs diretamente, cobrindo oito cenas e hover/foco; não é revisão exaustiva de glifos nem aceite humano/Linux pelo mantenedor.

| Reviewed group / Grupo | PNGs | Accepted intended difference / Diferença intencional aceita                                                                            |
| ---------------------- | ---- | -------------------------------------------------------------------------------------------------------------------------------------- |
| bucket                 | 8    | Density controls, spacing and single table frame / Densidade, espaçamento e moldura única                                              |
| connection             | 8    | Separate test action, dominant connection hierarchy and density header / Ação separada, hierarquia e densidade                         |
| error                  | 8    | Density header and canvas depth; error/navigation retained / Header/canvas, erro e links preservados                                   |
| form                   | 8    | Density header and canvas/primary glow; fields/confirmation/CLI retained / Header/canvas/brilho, formulário preservado                 |
| gallery                | 8    | Density header, single table frame and canvas / Header, tabela e canvas                                                                |
| gallery-focus          | 8    | Restricted primary/focus glow, visible outline retained / Brilho restrito e contorno mantido                                           |
| gallery-hover          | 8    | Restricted primary glow, text/disabled state retained / Brilho restrito, texto/desabilitado mantidos                                   |
| object                 | 8    | Density header/canvas/primary glow; metadata and original attachment retained / Header/canvas/brilho, metadados/anexo mantidos         |
| overview               | 8    | Dominant connection card, separate test action, density header and single table frame / Hierarquia, ação separada, densidade e moldura |
| unavailable            | 8    | Same hierarchy/density changes, unknown count and error retained / Hierarquia/densidade com desconhecido/erro mantidos                 |

All 80 originals still matched their preserved backup before replacement. After review, all 80 versioned PNGs were deliberately reconciled and verified byte-for-byte against the proposals; `threshold=0.1` and `maxDiffPixelRatio=0.001` remain unchanged. The ordinary full Linux suite is now rerunning **without** an update flag. / Originais conferidos contra backup antes de substituir; 80 novos conferidos contra propostas após revisão. Tolerâncias preservadas; rodada completa sem flag de atualização em andamento.

### Image scan / Scan da imagem

`scan-image.ps1 -RequireClean` passed on the exact image above at `2026-10-17T10:10:27Z`: **0 findings**, inventories **44 OS / 152 Java packages**, no severity/status suppression (`work/v6-scan.log`, `work/release/scan-summary.json`, `work/release/vulnerabilities.json`). Trivy 0.76.0 downloaded the vulnerability DB on 2026-10-17 (published 2026-10-16T01:13:54Z) and Java DB on 2026-10-17 (published 2026-10-17T01:07:50Z). The available vulnerability DB's advertised NextUpdate was already past when downloaded; the result is limited to that available database snapshot, not proof of latest coverage or no vulnerabilities. No skip-update/ignore policy was used. / Gate passou: zero achados, inventários OS/Java completos e sem supressões. Datas das bases registradas; NextUpdate da base disponível já estava vencido no download. Resultado limitado a esse snapshot, sem garantir cobertura mais recente/ausência de vulnerabilidades; sem ignorar updates/achados.

### Final results and restored workflow / Resultados finais e fluxo restaurado

The ordinary full Linux run, **without snapshot updates**, passed **157/157 tests in 5.9 minutes**, zero failed/skipped/flaky (`work/v6-browser-final.log`, `work/browser-report.json`). It includes all six density cases, both corrected shell cases, 64 visual-scene tests/80 PNG comparisons, native secure-context clipboard, real htmx upload/progress and held-response recovery, original attachments, text/static PNG/JPEG previews through the Alpine image's extracted worker, themes/languages, responsive/keyboard/axe and no-JS. Do not add the separate proposal capture or earlier focused runs to this count. / Suíte Linux completa sem atualizar snapshots passou 157/157, sem falhas/ignorados/flaky, incluindo todas as fixtures corrigidas e 80 comparações; sem somar capturas/rodadas anteriores.

`verify-streaming.ps1` passed on the same image with the test-only `-Xmx64m` Compose override: **134217728 bytes**, SHA-256 **C751E6B12D0372A88B1AB63D89A7E16149705772759EC31F3F4E7E8CF98CF785**, transfer timer **1.819718s** (`work/v6-streaming.log`). Original byte count/hash and live health were checked; exact UUID fixtures and temporary files were removed by the script. This single local transfer is not a throughput, concurrent-capacity, RSS/native-memory or arbitrary-size guarantee. / Streaming de 128 MiB com heap 64 MiB passou com tamanho/hash/saúde e limpeza; tempo único não comprova capacidade/concorrência/memória nativa/tamanho arbitrário.

Established npm checks passed (`work/v6-style.log`): Prettier, markdownlint (108 Markdown files), five documentation-checker tests/local link-index checks, Stylelint, ESLint, two contrast tests and 44 palette pairs. Final result-only documentation edits are checked again before handoff. No rule/tolerance/deadline was weakened. / Checks passaram; edição final só de resultados recebe nova checagem antes da entrega. Sem relaxar regras/tolerâncias/prazos.

The GUI container was stopped after consolidated testing. The clean-verified host fast-jar was restored on loopback port 8080, PID **42724** (`work/v6-host-gui.log`); its connection page returned 200/Connected and the host endpoint `http://localhost:4566`. Compose lists **only MiniStack** running. The saved image remains local for reproducibility, not publication. / GUI em contêiner parada; JVM local restaurada e conectada ao emulador, somente MiniStack no Docker. Imagem permanece local, sem publicação.

V6 automated checks passed for this **local uncommitted tree / Linux-amd64 image**, with scoped agent baseline review. This completes the authorized technical verification, not every release gate or operational S3 feature. Historical failures above remain part of the record. / Checks automáticos V6 aprovados para árvore local não commitada/imagem Linux-amd64, com revisão de baselines pelo agente. Não conclui gates de release ou todo escopo S3; falhas históricas preservadas.

### Reproduction commands / Comandos executados

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp clean verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
docker compose build --no-cache gui
docker compose up -d --no-deps --wait gui
./scripts/verify-container.ps1
./scripts/smoke.ps1
./scripts/verify-writes.ps1
./scripts/scan-image.ps1 -RequireClean
docker run --rm --ipc=host -v "${PWD}:/repo" -w /repo -e NODE_OPTIONS='--max-old-space-size=256 --max-semi-space-size=4' -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node scripts/run-browser-local.mjs --output work/v6-browser-final
docker compose -f compose.yaml -f compose.streaming-test.yaml up -d --no-deps gui
./scripts/verify-streaming.ps1
docker compose stop gui
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
./scripts/check-style.ps1
docker run --rm -v "${PWD}:/repo:ro" -w /repo hadolint/hadolint:v2.12.0 hadolint Dockerfile tools/ux0/Dockerfile
docker run --rm -v "${PWD}:/repo:ro" -w /repo rhysd/actionlint:1.7.7
docker compose config --quiet
git diff --check
```

The proposal-capture config/review helper and before/proposed/review directories are ignored QA artifacts under work; the ordinary final command above does not update references. HTML presentation fixtures/traces may contain ephemeral development CSRF tokens: do not commit or distribute them. Only reviewed PNG baselines are versionable. / Helpers/capturas em work são artefatos QA ignorados; comando final não atualiza referências. Não distribuir HTML/traces com tokens efêmeros; somente PNGs revisados são versionáveis.

## Remaining gates / Gates restantes

Human assistive-technology/zoom acceptance, independent-user quick start, legal/name validation (including INPI and intended registry/domain), D2–D6, approved version/destination and immutable candidate-commit verification remain pending unless separately evidenced. No ARM64/native-image/provenance/signing/load-capacity certification. A clean scan would mean no findings reported by that database, not proof of no vulnerabilities. / Gates humanos/jurídicos/decisões/versão/destino/candidato imutável pendentes; sem certificar plataformas/capacidade. Scan limpo não comprova ausência de vulnerabilidades.
