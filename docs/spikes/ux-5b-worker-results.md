# UX-5B — isolated worker foundation / Base do worker isolado

Date / Data: 2026-10-16. Status: implementation/verification slice; no image HTTP route or UI activation / fatia de implementação/validação, sem rota HTTP ou UI de imagens.

## Implemented boundary / Limite implementado

See [ADR-0019](../decisions/ADR-0019-raster-preview-isolation.md). The owner extracts one trusted standalone class, clears the child environment, invokes Java without a shell, owns temporary files and terminates the child before cleanup. Limits are 128 MiB Java heap, five-second wait, one worker per owner, 1 MiB input/output, 4096 per axis and 4,000,000 pixels. These are product policies, not measured capacity. The worker copies fresh pixels and re-encodes PNG, never original bytes inline. No new dependency, endpoint, template, CSS, JavaScript, CSP change or baseline regeneration.

Conforme ADR, owner extrai classe confiável, limpa ambiente, inicia Java sem shell e possui arquivos/encerramento/limpeza. Limites acima são política, não capacidade medida. Reencode de pixels, sem expor original inline. Nenhuma dependência, rota, template, CSS/JS/CSP ou baseline alterado.

## Local verification / Verificação local

Environment: Windows 11, Temurin Java 25.0.4.1, Maven 3.9.9. MiniStack 1.5.25 S3 is the only required Docker service; application and test JVMs run on the host. No image build or container test suite in this slice.

Ambiente Windows/JDK/Maven acima; somente emulador S3 em Docker. Aplicação/testes no host; sem build de imagem ou suíte em contêiner.

```powershell
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
$env:STACKEMU_INTEGRATION='true'
$env:NODE_OPTIONS='--max-old-space-size=192 --max-semi-space-size=4'
mvn -B -ntp spotless:apply verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
```

Result / Resultado: BUILD SUCCESS at 2026-10-16 11:07 -03:00; 90 tests, zero failures/errors/skips, including eight worker lifecycle tests and four earlier codec-spike tests. Local Maven also regenerated frontend resources, checked Spotless, built the fast-jar and produced the SBOM. No new-image or vulnerability scan was run. / BUILD SUCCESS na data/hora indicada; 90 testes, sem falhas/erros/ignorados, incluindo oito de worker e quatro do spike anterior. Maven local regenerou frontend, conferiu Spotless, empacotou fast-jar e gerou SBOM. Sem imagem nova nem novo scan de vulnerabilidades.

The packaged GUI was restarted locally after verification and its Portuguese landing page returned HTTP 200; local MiniStack health remained available. This is startup/read-page evidence, not an execution of the raster worker through an HTTP endpoint. / GUI empacotada reiniciada localmente; página inicial portuguesa HTTP 200 e saúde MiniStack disponível. Evidência de inicialização/leitura, não execução do worker por endpoint HTTP.

Local `npm run check` passed (Prettier, markdownlint on 98 files, Stylelint, ESLint, two contrast tests and 44 palette pairs); `git diff --check` passed. A final process inventory found no remaining worker/test-fixture JVMs. No browser suite was run for this worker-only slice. / Checks locais de estilo/documentação/contraste e whitespace passaram; inventário final sem JVMs de worker/fixture restantes. Sem suíte de navegador nesta fatia exclusiva do worker.

`RasterPreviewProcessTest` covers actual extracted child execution for PNG/JPEG, unchanged input bytes, invalid/oversized/conflicting format and dimension/pixel refusal with recovery, noisy JPEG input whose PNG output exceeds the cap, forced timeout termination verified by child PID, interruption termination, failed executable startup, cleared environment, discarded stderr without blocking, occupied-slot refusal and generated-file cleanup on success/failure. The slot is held deterministically in the test; this is not a race/load test. Timeout/interruption use a sleeping test subprocess, not a deliberately hung real decoder. The earlier codec spike remains a separate experiment.

Testes cobrem execução real do filho extraído, bytes originais intactos, recusas/recuperação, saída excessiva, encerramento por PID em timeout/interrupção, falha ao iniciar, ambiente limpo, stderr descartado, recusa com slot ocupado e limpeza em sucesso/falha. Slot ocupado de forma determinística, não teste de corrida/carga. Timeout/interrupção usam subprocesso de fixture dormindo, não decoder real travado. Spike anterior permanece separado.

First focused attempt: 14 tests, one environment assertion failure because Windows supplied `SystemRoot` after `environment().clear()`. The corrected fixture permits only that OS-required key; it still rejects all other inherited variables. Consulted [JDK ProcessBuilder](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ProcessBuilder.html) on 2026-10-16. No credentials or environment values are printed.

Primeira tentativa focada: 14 testes, uma falha pela variável `SystemRoot` adicionada pelo Windows. Fixture corrigida permite só essa chave obrigatória, recusando demais variáveis herdadas. Referência oficial consultada; sem imprimir credenciais/valores de ambiente.

## Remaining gates / Gates restantes

Image HTTP/UI activation remains pending: bounded S3 contract, filename/MIME/encoding agreement, animation/trailing/malformed-content semantics, local packaged fast-jar execution, localized native refusal/loading/fallback, fixed PNG response/no-store/headers and local browser checks. Worker extraction is tested from the test/build classloader, not yet from a running packaged Quarkus endpoint. Exact 4,000,000-pixel acceptance, EXIF/ICC/alpha/fidelity and exhaustive hostile fixtures remain unverified. No process sandbox, native-memory bound, CPU quota, hard real-time termination, accessibility certification or full emulator compatibility claim. New-image consolidated runtime/Linux browser testing remains reserved for the final stage. StackEmu legal/name validation remains pending.

Ativação HTTP/UI pendente: leitura S3 limitada, acordo nome/MIME/encoding, animação/dados extras/malformados, fast-jar local, estados/fallback bilíngues, PNG fixo/headers/sem cache e navegador local. Extração ainda não comprovada em endpoint Quarkus empacotado. Aceitação exata de 4.000.000 pixels, EXIF/ICC/alpha/fidelidade e corpus hostil completo não verificados. Sem promessas de sandbox, memória nativa, quota CPU, tempo real ou certificação. Imagem nova/validação consolidada ao final; nome/jurídico pendentes.
