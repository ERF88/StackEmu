# UX-5B — local raster codec spike / Spike local de codecs raster

Date / Data: 2026-10-16. Status: preparatory experiment, image preview NOT enabled / experimento preparatório, preview de imagens NÃO habilitado.

## Scope / Escopo

`RasterPreviewSpikeTest` is isolated test code, not a production decoder or HTTP endpoint. It generates its own small PNG/JPEG fixtures, reads dimensions before requesting decoded pixels, copies pixels into a fresh ARGB image and writes PNG using the local JDK. No dependency, object download policy, route, template, CSP or browser baseline changes. See [proposed ADR-0019](../decisions/ADR-0019-raster-preview-isolation.md).

Teste isolado, sem decoder de produção nem endpoint. Gera fixtures locais PNG/JPEG, consulta dimensões antes do decode, copia pixels para imagem ARGB nova e grava PNG com JDK local. Sem alteração de dependências, download, rotas, templates, CSP ou baselines.

## Local checks / Verificações locais

Environment: Windows 11, Eclipse Temurin 25.0.4.1, Maven 3.9.9, headless JVM. The focused command uses existing generated CSS resources with `skip.frontend=true`; it is not a clean frontend build or full Maven verification.

Ambiente Windows/JDK/Maven acima, JVM headless. Comando usa CSS gerado já existente; não equivale a build frontend limpo nem verificação Maven completa.

```powershell
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp spotless:apply test '-Dtest=RasterPreviewSpikeTest,ArchitectureTest,TextPreviewPolicyTest' '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true' '-Dskip.frontend=true'
```

Result / Resultado: BUILD SUCCESS; 10 tests, zero failures/errors/skips (four raster spike, four architecture, two existing text-policy tests) / 10 testes, sem falhas/erros/ignorados (quatro do spike raster, quatro de arquitetura, dois existentes da política de texto).

First attempt failed one fixture check because `ImageIO.read(ImageInputStream)` closed that stream and the test attempted a second close. The fixture verification now passes a byte-array input stream instead; the repeated command above passed. This was a test stream-ownership error, not a production preview regression. / Primeira tentativa falhou por fechamento duplicado do stream na fixture; corrigida a verificação usando stream de bytes e comando repetido passou. Erro de ownership no teste, não regressão do preview em produção.

Local `npm run check` passed: Prettier, markdownlint (97 files), Stylelint, ESLint, two contrast tests and 44 palette pairs. Spotless application/check passed in the Maven command above. These are style/contract checks, not proof of decoder safety or full accessibility. / Checks locais de estilo/documentação/contraste passaram, sem certificar segurança do decoder ou acessibilidade integral.

Cases: local PNG/JPEG decode and fresh PNG output; a PNG text metadata marker and an appended script-like suffix absent from fresh output; rejected empty/oversized/SVG/mismatched/truncated/unsupported fixtures; rejected width, height and pixel counts above the proposed limits. These fixtures are not a hostile-image corpus, exhaustive metadata-removal proof or a test of every exact boundary. JPEG color fidelity, EXIF orientation, alpha behavior, ICC profiles and animation semantics remain unverified.

Casos: decode PNG/JPEG e PNG novo; ausência de marcador textual e sufixo na saída; recusas de fixtures vazias/excessivas/SVG/conflitantes/truncadas/não suportadas e de dimensões/pixels excessivos. Não é corpus hostil nem prova exaustiva de metadados/limites. Fidelidade JPEG, orientação EXIF, alpha, ICC e animação não verificados.

## Limitations and next gate / Limitações e próximo gate

The experiment buffers output before checking its size, uses in-process ImageIO and has no per-decode CPU/heap/concurrency isolation. It is deliberately NOT suitable for untrusted request handling. Production needs a bounded output sink and verified worker termination/resource ownership before HTTP activation. Reader format detection is not a complete structural validator. No benchmark or security certification is claimed.

Experimento armazena saída antes de verificar tamanho, usa ImageIO no processo e não isola CPU/heap/concorrência por decode; NÃO serve para requests não confiáveis. Produção precisa de saída limitada e worker com encerramento/recursos verificados antes de ativar HTTP. Detecção do reader não é validação estrutural completa. Sem benchmark/certificação.

No Docker command, image build, full container suite, browser suite or MiniStack integration is required for this isolated experiment. Per user instruction, consolidated testing with a newly built image remains for the end. UX-5A historical counts remain historical and are not new UX-5B evidence. StackEmu legal/name validation remains pending.

Sem comandos Docker/build/suíte completa/navegador/integração MiniStack necessários neste spike. Conforme usuário, imagem nova e testes consolidados ficam para o final. Contagens UX-5A não são evidência nova UX-5B; nome/jurídico pendentes.
