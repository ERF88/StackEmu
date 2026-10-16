# UX-5A execution / Execução UX-5A

Implementation started / Implementação iniciada: 2026-10-15. Final validation resumed / Validação final retomada: 2026-10-16. Bounded text and JSON-as-text, not complete UX-5/raster preview. / Texto e JSON-como-texto limitado, não UX-5/imagens completos. Contract / Contrato: [ADR-0018](../decisions/ADR-0018-bounded-text-preview.md).

## Implementation / Implementação

A separate read port/SDK adapter and GET resource preview complete UTF-8 objects up to 64 KiB. Managed body reading consumes at most 65537 bytes and aborts on all exits. Unsupported/encoded/advertised oversized content is rejected before reading. Strict byte validation plus Qute escaping prevent interpretation of binary controls/markup; no JSON parsing, decompression, iframe/image/embed or remote resources. Existing CSP, no-store, attachment downloads and mutations remain. Native details link to the full preview page with encoded key/language/theme, also without JS. Refusal is explicit with original download retained.

Porta/adapter/GET separados para UTF-8 completo até 64 KiB; ler no máximo 65537 bytes e abortar todas as saídas. Recusar metadados/encoding/tamanho incompatíveis antes de ler. Validar bytes e escapar Qute, sem interpretar controles/markup/JSON, descomprimir ou carregar mídia/embeds/recursos remotos. Preservar CSP/no-store/download/mutações. Link nativo codifica chave/idioma/tema e funciona sem JS. Recusa explícita mantém download.

## Verification / Verificação

Fresh Java 25 Maven verification on 2026-10-15 passed 78 tests with zero failures/errors/skips, including real local MiniStack integration and loopback stalled/trickling HTTP-body fixtures. The first compile found a fixture field-name collision; corrected without relaxing rules. Initial SDK-only timeout failed the seven-second stalled-body guard; the named-builder attempt failed CDI resolution. Final adapter-owned transport uses the managed client's public configuration, explicit socket/connect limits and monotonic chunk deadline. Both stalled/trickling tests passed with unchanged seven-second test guards. These are controlled loopback transport tests, not emulator latency benchmarks or a hard real-time guarantee.

Nova verificação Java 25/Maven em 2026-10-15: 78 testes, zero falhas/erros/skips, incluindo MiniStack real local e fixtures HTTP loopback de bloqueio/gotejamento. Corrigido conflito de nome na fixture. Timeout só de API falhou no limite de sete segundos; builder nomeado falhou CDI. Transporte final próprio reutiliza configuração pública do gerenciado com limites de socket/conexão e prazo entre blocos. Bloqueio/gotejamento passaram sem relaxar o limite de sete segundos dos testes; não é benchmark de latência nem tempo real rígido.

The final GUI image built successfully. Container/license checks, smoke, native writes/download/SHA-256 and CSRF rejection passed, and passed again after restarting Docker on 2026-10-16. Six focused Chromium tests passed: both themes/languages, escaped hostile key/body, keyboard navigation, no-JS JSON verbatim, explicit format/size/encoding refusals and missing objects. Sixteen actual screenshots (detail-link and preview, 390/1440 pixels) were agent-reviewed; preview axe checks found no violations and no horizontal overflow was detected. No human assistive-technology certification is implied.

Imagem final construída; checks de container/licenças, smoke, escrita/download/SHA-256/CSRF passaram e foram repetidos após reiniciar Docker em 2026-10-16. Seis testes focados Chromium passaram: temas/idiomas, chave/corpo hostis escapados, teclado, JSON verbatim sem JS, recusas e objeto ausente. Agente revisou 16 capturas reais de link/preview em 390/1440px; axe sem violações no preview e sem overflow horizontal detectado. Não certifica tecnologia assistiva.

Only the eight `object-*.png` baselines changed for the added native preview link, after reviewing actual UI; all eight revised references were inspected. Individual SHA-256 comparisons confirmed the other 72 references unchanged. Aggregate SHA-256 over sorted filenames concatenated with bytes changed from `8b6aabf658292b9deed76a9254498ce31bf046785028b84ed5b58558e78550a3` to `7a815f28bcd7d67cb6a83020150fbc525617ccd7cfd62c98ed34dd43e6dab27b`. The scoped update ran eight tests. Normal full pinned Linux Chromium regression on 2026-10-16 passed 118 tests, including 80 screenshot comparisons, with zero failures/skips/flaky results and without snapshot-update flags. Local ignored report/captures are under work/browser-report.json and work/browser-results/.

Somente oito referências `object-*.png` mudaram pelo link nativo, após revisão da UI real e das oito referências resultantes. Hashes individuais confirmam outras 72 intactas; hash agregado antes/depois acima. Atualização delimitada executou oito testes. Regressão completa normal Chromium/Linux fixado em 2026-10-16: 118 testes aprovados, incluindo 80 comparações visuais, zero falhas/skips/flaky, sem flags para atualizar snapshots. Relatório/capturas locais ignorados em work/browser-report.json e work/browser-results/.

On 2026-10-16, Prettier, Markdown (95 files), CSS/JavaScript lint, two contrast tests/44 palette pairs, PowerShell analysis, Hadolint (both Dockerfiles), actionlint, Compose and diff whitespace checks passed. An earlier approval-service usage limit interrupted the snapshot command before execution; no references changed in that attempt. This is local verification, not GitHub-hosted CI, publication or a new vulnerability scan.

Em 2026-10-16 passaram Prettier, Markdown (95 arquivos), CSS/JS, dois testes/44 pares de contraste, PowerShell, Hadolint, actionlint, Compose e whitespace. Limite anterior do serviço de aprovação interrompeu comando de snapshots antes da execução, sem alterar referências naquela tentativa. Verificação local, não CI GitHub/publicação/scan novo.

## Reproduction / Reprodução

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'
docker compose up -d --build gui
docker run --rm --ipc=host --memory=768m -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node scripts/run-browser-local.mjs preview.spec.mjs
```

Omit spec for normal full regression, without snapshot update. Timeout is a policy, not measured capacity/hard real time. / Omitir spec para regressão normal sem atualizar snapshots. Timeout é política, não capacidade/tempo real rígido.

## Remaining / Restante

Raster validation/re-encoding, compressed-byte/dimension/pixel/output/CPU limits and hostile-image/runtime-codec tests remain UX-5B, still required in the first delivery. No HTML/SVG/PDF/archives/active content, JSON syntax validator, highlighting, truncation or remote resolution. Wider compatibility, human assistive-technology acceptance, full accessibility/security certification, new vulnerability scan, production capacity, legal/name validation and publication remain unverified/not approved.

Imagens/limites/reencode/codecs/conteúdo hostil seguem na UX-5B, ainda na primeira entrega. Sem HTML/SVG/PDF/compactados/ativo, validar sintaxe JSON, highlighting, truncamento ou recursos remotos. Demais compatibilidades/aceites/certificações/scan/capacidade/nome/jurídico/publicação não comprovados/aprovados.
