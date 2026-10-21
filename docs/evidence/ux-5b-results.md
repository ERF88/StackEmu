# UX-5B — bounded S3 image preview / Preview S3 de imagens limitado

Date / Data: 2026-10-16. Status: local implementation and verification; final new-image validation deferred / implementação e verificação locais; validação final com imagem nova adiada.

## Implemented scope / Escopo implementado

[ADR-0019](../decisions/ADR-0019-raster-preview-isolation.md) defines separate read-only image contracts, bounded AWS transport, singleton isolated worker, native shell and one same-origin image request. PNG/JPEG extension/MIME/actual format must agree; no compressed encoding. Static images only, PNG CRC/animation/trailer guards, JPEG framing/trailer checks and decoder-warning refusal. Fresh PNG output does not copy source metadata; original attachment bytes/name/type remain unchanged. Fixed PNG/inline preview filename, no-store, existing CSP/nosniff/no-referrer/frame denial, stable error statuses. Shell 200 is not existence or preview confirmation; native fallback remains without JavaScript. No mutation, presigning, token cache, version-consistent snapshot, active-document preview or automatic body fetch from listing/detail/drawer.

Contrato vinculado define leitura própria, transporte limitado, worker singleton, shell nativo e um GET da imagem. Extensão/MIME/formato coerentes, sem compressão/animação; guardas estruturais e recusa de warnings. PNG novo sem copiar metadados e anexo original intacto. Headers/status/fallback preservados; shell 200 não confirma existência/sucesso. Sem mutação/assinatura/cache/snapshot de versão/documentos ativos/leitura automática na lista/detalhe/drawer.

The earlier [codec spike](ux-5b-raster-spike.md) and [worker-only record](ux-5b-worker-results.md) remain historical evidence. This record, not their old “no image endpoint” labels, owns integration status. Guards are not exhaustive PNG/JPEG conformance, malware detection, security sandbox, native-memory/CPU quota or hard real-time termination guarantees. Product byte/pixel limits are chosen policy, not performance measurements. EXIF orientation/color fidelity and human assistive-technology acceptance remain limited/unverified.

Spikes anteriores são históricos; este registro define integração atual. Guardas não certificam formatos/antivírus/sandbox/memória nativa/CPU/tempo real. Limites são política, não benchmark; EXIF/fidelidade e aceite humano continuam limitados/não verificados.

## Local execution / Execução local

Windows 11, Temurin Java 25.0.4.1, Maven 3.9.9, Node 24.10.0/npm 11.6.1; MiniStack 1.5.25 S3 alone in Docker. Application, Maven and browser checks run on the host. No Docker application build/test suite, image scan, GitHub publication or baseline update.

Ambiente acima; somente S3 em Docker, aplicação/Maven/navegador no host. Sem build/testes de aplicação em contêiner, scan/publicação ou atualização de baseline.

```powershell
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
$env:STACKEMU_INTEGRATION='true'
$env:NODE_OPTIONS='--max-old-space-size=192 --max-semi-space-size=4'
mvn -B -ntp spotless:apply verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
$env:PATH="$PWD\target\frontend\node;$PWD\target\frontend\node\node_modules\npm\bin;$env:PATH"
node node_modules/@playwright/test/cli.js test -c playwright.preview-local.config.mjs
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
```

Completed local verification: the first full Maven run succeeded at 11:38 -03:00; after the final text-refusal translation update, `mvn ... verify` again returned BUILD SUCCESS at 11:47:56 -03:00, with 102 tests and zero failures/errors/skips. The preview-only browser run on the final local package passed all 13 tests (seven image, six text), no skips/failures, on host Chrome 155.0.8059.80. This exercises the real worker extracted from the locally packaged Quarkus application. Test counts describe the suite, not sums of repeated runs. / Maven completo passou inicialmente às 11:38 e novamente após tradução final às 11:47:56; 102 testes sem falhas/erros/ignorados. Chrome local 155.0.8059.80 sobre pacote final: 13 testes passaram (sete imagem, seis texto), sem skips/falhas, usando worker extraído do pacote Quarkus real. Contagens não somam repetições.

Style verification passed through the explicitly provisioned npm CLI: Prettier, markdownlint (99 files), Stylelint, ESLint, two contrast tests and 44 palette pairs. One initial command failed before checks because npm's bin directory preceded its Node root on PATH; the explicit CLI invocation above resolved it. ESLint then identified an undeclared `console` in the new test logger; using the existing allowed Node `process.stdout` fixed it without changing lint rules or product code. / Checks de estilo/documentação/contraste passaram. Primeira chamada falhou por ordem do PATH antes dos checks; CLI explícito resolveu. ESLint apontou `console` não declarado no logger do teste, corrigido com `process.stdout`, sem relaxar regras ou alterar produto.

Eight new image diagnostic screenshots were generated (two languages, two themes, mobile/desktop). Agent visual review covered Portuguese mobile and English desktop in both themes; this is not human design/accessibility acceptance. No baseline update flag or snapshot replacement was used. / Oito capturas novas de imagem geradas; agente revisou mobile português e desktop inglês nos dois temas, sem equivaler a aceite humano. Sem flag de atualizar baselines ou substituir snapshots.

Java integration fixtures own UUID buckets and remove only their keys/bucket. They exercise real PNG/JPEG S3 reads, a PNG text marker stripped by the real worker, attachment byte identity, hostile Unicode keys/encoded links, no-store/security headers, unsupported/gzip/generic metadata, excessive input, empty/mismatched/truncated/CRC-corrupt/APNG/trailing data and excessive width, missing/invalid targets. Separate tests cover bounded unknown-length reads, metadata refusal before body access, abort/mismatched length, actual loopback stalled/trickling transport and web error mapping. Existing worker lifecycle fixtures retain their limitations: deterministic occupied semaphore and sleeping subprocess for kill tests, not real decoder hangs or race/load proof.

Fixtures Java com buckets próprios e limpeza delimitada cobrem operações reais, marcador removido, original idêntico, chaves/links hostis, headers, recusas estruturais/metadados/bytes/dimensões e erros. Testes separados cobrem abort/leitura limitada/transporte loopback lento e status. Fixtures de semáforo/processo dormindo não são prova de corrida/carga/decoder travado real.

Host browser checks use the installed Chrome through the preview-only config, not the pinned Linux browser. Screenshots are diagnostic captures under ignored `work/browser-results/`, never updated Linux references. Both languages/themes, 390/1440 widths, keyboard, axe, one automatic image request, metadata marker removal, original download hash, malformed/missing image and no-JS fallback are covered by the specs; held-request loading is a labelled network simulation. Passing source tests alone does not establish that these browser cases ran; see completed results above.

Chrome instalado/config somente de previews, não navegador Linux fixado. Capturas diagnósticas em work ignorado, sem substituir referências. Specs cobrem idiomas/temas/larguras/teclado/axe/GET único/metadados/hash/falhas/sem JS; loading retido é simulação de rede. Ter specs não comprova execução; resultados acima são autoritativos.

## Remaining acceptance / Aceite restante

New-image codec/native-library/container/security and full Linux/browser regression validation remain for the final stage, as requested. Host Chrome results are not baseline acceptance or full WCAG certification. No new throughput, fidelity, hostile-corpus completeness, advanced AWS configuration compatibility or trademark/legal availability claims. StackEmu name/legal validation remains pending.

Validação consolidada com imagem nova/codecs/bibliotecas/contêiner/segurança e regressão completa Linux ao final. Chrome local não aprova baselines nem certifica WCAG. Sem promessas de capacidade/fidelidade/corpus completo/configurações AWS avançadas/disponibilidade jurídica; nome pendente.
