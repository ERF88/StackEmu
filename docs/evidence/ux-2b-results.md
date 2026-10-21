# UX-2B execution / Execução UX-2B

Date / Data: 2026-10-15. Current slice: metadata drawer and bucket/key clipboard, not complete operational S3 management. / Fatia atual: drawer de metadados e clipboard de bucket/chave, não gerenciamento S3 completo.

## Contract and scope / Contrato e escopo

[ADR-0015](../decisions/ADR-0015-object-drawer-clipboard.md) and [explorer contracts](../development/s3-explorer.md) define native modal semantics, transient history, one checked escaped metadata fragment, active-XHR/status/marker guards, abort/timeout/manual retry and clipboard fallback. Keep native full details and attachment downloads. No preview, ARN, S3 endpoint/public/signed URLs, copy/move, deletion or new application/SDK port is introduced.

Documentos vinculados definem modal nativo, histórico transitório, fragmento Qute escapado, recusa de XHR obsoleto/status/marcador inválido, abort/timeout/retry manual e fallback de cópia. Preservar detalhes completos/download de anexo. Sem preview, ARN, URLs S3/assinadas, cópia/movimentação, exclusão ou nova porta de aplicação/SDK.

## Verification log / Registro de verificação

- Initial Maven run: 66 tests, one failed because the new 404 assertion expected a message not used by the catalog. Corrected only the assertion to the existing localized error; subsequent full Java 25/local MiniStack verification passed all 66 tests with no failures/errors/skips. Later source changes require the final run below; do not substitute this intermediate pass.
- Maven inicial: 66 testes, uma falha por mensagem esperada incorreta no novo teste 404. Corrigida a expectativa para o catálogo existente; repetição completa passou 66 sem falhas/erros/ignorados. Mudanças posteriores exigem execução final abaixo; não substituir por resultado intermediário.
- Intermediate frontend/style/contrast checks and local image build passed. First focused drawer run: 11 tests passed, including both languages/themes, eight attached desktop/mobile screenshots, axe, native fallback/history, missing object, network/unmarked response/manual retry, real ten-second XHR timeout and close/reopen race. Clipboard success/denial in that run used API simulations, not real OS permissions.
- Checks/build intermediários passaram. Primeiro navegador focado: 11 testes passaram nos cenários acima, com oito capturas anexadas e simulações da API de clipboard, sem certificar permissões/SO.
- Review found HTML textarea normalization could lose control characters. Final implementation derives exact clipboard values from the encoded canonical URL and explicitly labels percent-encoded manual fallback for such keys. The next run adds real local-emulator CR/LF fixtures and native secure-context Clipboard API checks.
- Revisão encontrou normalização de caracteres de controle em textarea. Implementação final deriva valor exato da URL canônica codificada, identificando fallback manual codificado para esses casos. Próxima execução acrescenta fixture CR/LF e Clipboard API nativa.
- Final Java 25 `mvn verify` with `STACKEMU_INTEGRATION=true`, bounded Maven/test heaps and local MiniStack: 67 tests passed, no failures/errors/skips, including the new exact-key/prefix encoding case. Existing Quarkus module/dynamic-agent and CycloneDX keyword warnings did not fail verification. Image/focused/full browser/style/screenshot results are recorded below.
- Java 25 final com MiniStack local e heaps limitados: 67 testes passaram, sem falhas/erros/ignorados, incluindo chaves/prefixos codificados. Avisos existentes de módulos/agent/CycloneDX não causaram falha. Demais verificações registradas abaixo.

Final-source focused attempt initially passed 12 of 13 tests, including the real Chromium Clipboard API, but timed out locating the CR/LF key. Trace inspection showed the listing href contained `%0A` instead of original `%0D%0A`: XML response normalization preceded the drawer. The AWS adapter now explicitly requests URL-encoded ListObjectsV2 responses (SDK-decoded, no manual second decode); a fresh integration case covers opaque prefixes, CR/LF, Unicode, plus and literal `%20`. This failed attempt is not a successful run, and assertions/timeout were not weakened to accept a different key. / Tentativa focada passou 12/13, incluindo API nativa, mas falhou na chave CR/LF: trace mostrou normalização na listagem antes do drawer. Adapter passa a solicitar resposta codificada URL, sem dupla decodificação; novo caso de integração verifica preservação exata. Não contar falha como sucesso nem relaxar expectativa para outra chave.

## Final gates / Gates finais

- Final local Docker build passed after the URL-encoded listing correction; its package stage skips tests and does not replace the full 67-test Java run. Container policy/smoke/write scripts passed, including non-root/read-only/loopback/licenses, localized fragments and original-file SHA-256/CSRF rejection.
- Imagem Docker final passou após correção; empacotamento não substitui testes. Scripts de política/smoke/escrita passaram, incluindo container endurecido, fragmentos localizados e integridade/CSRF.
- Final focused run through the trusted-loopback helper: 13 passed, zero skips/failures. Includes native Chromium clipboard write/read for exact Unicode/HTML-like key and bucket, simulated denial/absence, CR/LF encoded manual fallback, real ten-second timeout/no automatic retry, exact-target cleanup, native no-JS fallback, ordinary history/direct links and stale-XHR identity rejection independent of abort. Delayed-network and synthetic-stale-event cases are intentionally separate.
- Execução focada final via loopback: 13 passaram, sem skips/falhas. Inclui clipboard nativo Chromium, simulações de falha, CR/LF, timeout real, cleanup exato, fallback/histórico e recusa de resposta obsoleta independente de abort. Casos simulados não são certificação.
- Final frontend checks passed: Prettier, 88 Markdown files, Stylelint/ESLint, two contrast reference tests and 44 palette pairs. PowerShell analyzer, Hadolint for both Dockerfiles, actionlint, Compose and whitespace checks passed.
- Checks finais passaram: formatação, 88 Markdown, lint CSS/JS, dois testes/44 pares de contraste, PowerShell, Hadolint, actionlint, Compose e espaços.
- Reviewed four final drawer captures: English desktop/light-dark and Portuguese mobile/light-dark, from the eight captured variants. The scrollable modal uses its full viewport width on mobile; long hostile-looking keys remain text and response policy is distinct from metadata. Human assistive-technology acceptance remains separate.
- Revistas quatro capturas finais, desktop en e mobile pt-BR nos dois temas, das oito variantes. Modal rolável ocupa largura mobile; chaves hostis são texto, política separada de metadados. Aceite humano continua separado.
- Final normal full suite: 97 passed in 3.6 minutes, zero failures/skips/flaky results, including native Chromium clipboard and all 80 visual comparisons. Report `work/browser-report.json` records 97 expected, 0 unexpected/skipped/flaky. Before/after hashes of the sorted 80 baseline filenames/bytes match: `8b6aabf658292b9deed76a9254498ce31bf046785028b84ed5b58558e78550a3`. No baseline updates, tolerance/retry/mask changes were made for UX-2B.
- Suíte completa final: 97 passaram em 3,6 minutos, sem falhas/ignorados/flaky, incluindo clipboard nativo e 80 comparações visuais. Relatório local confirma contagens; hashes antes/depois idênticos acima. Nenhuma referência/tolerância/retry/máscara alterada na UX-2B.
- Generated/authored final assets matched the packaged GUI byte for byte: CSS 20,138 raw / 4,737 gzip bytes, SHA-256 `f1be4a434b331b1a1a510cf68714c1d471990478efb9e2e77719e61822f1ae2d`; JavaScript 7,378 raw / 2,094 gzip bytes, SHA-256 `bf5e321580b9e0062ce18b7cea56866d6b59c6e5d22485e579df64a8087c0bb4`. These are artifact sizes/integrity checks, not a speed/throughput improvement claim.
- Assets finais idênticos aos servidos na imagem; tamanhos e hashes acima são medidas do artefato/integridade, não alegação de desempenho.

## Browser environment / Ambiente do navegador

Docker's `host.docker.internal` HTTP origin is not a trusted loopback origin for Clipboard API. `scripts/run-browser-local.mjs` forwards only `127.0.0.1:8080` inside the pinned Linux test container to the fixed host GUI on `host.docker.internal:8080`, streams the original response headers/body, runs Playwright and closes connections on exit. It is test tooling, never shipped runtime, not an open proxy or a browser-security bypass. It does not grant inline/eval or change CSP. It permits testing the actual Clipboard API in Chromium's normal trusted-loopback context. Timing samples through this proxy are not performance benchmarks. API denial/absence and stale-identity guard simulations remain explicitly separate from native behavior.

HTTP host.docker.internal não é loopback confiável para clipboard. Helper vinculado encaminha apenas loopback interno do container para GUI fixa do host, preserva resposta por streaming, executa Playwright e fecha conexões. Ferramenta de teste, não runtime/proxy aberto/bypass de segurança, sem mudar CSP. Permite API real em loopback normalmente confiável; tempos via proxy não são benchmark. Simulações continuam distintas de comportamento nativo.

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'
docker compose up -d --build gui
docker run --rm --ipc=host --memory=768m -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node scripts/run-browser-local.mjs
```

## Remaining acceptance / Aceite restante

No new vulnerability scan, load test, full compatibility/WCAG/security certification, human assistive-technology acceptance, GitHub publication or release approval is claimed. Browser timeout/abort does not guarantee SDK cancellation. Clipboard behavior on other browsers/OS and human review remain separate. Expanded first-delivery capabilities and legal/name validation remain pending; StackEmu is not AWS affiliated.

Sem novo scan, carga, certificação ampla, aceite humano de tecnologia assistiva, publicação/release. Timeout/abort não garante cancelar SDK. Outros navegadores/SO e revisão humana continuam separados. Demais capacidades da primeira entrega e validação jurídica/do nome seguem pendentes, sem afiliação AWS.
