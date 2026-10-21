# V1 — depth and tokens / Profundidade e tokens

Date / Data: 2026-10-16. V0 accepted by the maintainer in chat; [ADR-0013](../decisions/ADR-0013-visual-ux-evolution.md) records scope. / V0 aceita pelo mantenedor; ADR registra escopo.

## Implementation / Implementação

Decorative radial canvas gradient uses existing surface/canvas palette endpoints. Three solid content surfaces and their text colors remain unchanged. Glow tokens are limited to focus, primary actions and the existing connected-status sample/confirmed status; disabled submissions have no glow. Increased contrast, forced colors and reduced transparency remove decoration. Native focus outlines remain visible without color-mix support. Table frames inside summary/resource cards are removed without removing scroll semantics or keyboard focus. System fonts, CSP, JS, routes and S3 boundaries are unchanged.

Gradiente decorativo usa extremos existentes da paleta. Três superfícies sólidas/cores de texto preservadas; brilho restrito a foco, ação primária e status conectado confirmado/amostra existente; envio desabilitado sem brilho. Contraste aumentado, cores forçadas e transparência reduzida removem decoração. Contorno nativo preservado sem color-mix. Moldura interna removida sem perder scroll/foco. Fontes, CSP, JS, rotas e limites S3 preservados.

## Verification / Verificação

Executed on the host with Java 25.0.4.1 and local MiniStack only / Executado no host com Java 25.0.4.1 e MiniStack local:

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true'
$env:PATH="$PWD\target\frontend\node;$PWD\target\frontend\node\node_modules\npm\bin;$env:PATH"
node target/frontend/node/node_modules/npm/bin/npm-cli.js run check
node node_modules/@playwright/test/cli.js test -c playwright.visual-local.config.mjs
```

Maven returned BUILD SUCCESS at 17:08:06 -03:00: 102 tests, zero failures/errors/skips; Spotless passed and CSS/Qute packaging completed. The first attempt failed (9 failures, 21 errors) when MiniStack was unavailable, including a local transport timing failure; no tests/timeouts were weakened. Docker Desktop was started, only `docker compose up -d --no-deps ministack --wait` was run, and the complete local Maven command then passed. GUI runs as a separate host JVM, not a container. / Maven passou às 17:08:06: 102 testes sem falhas/erros/ignorados, Spotless/build CSS/Qute aprovados. Tentativa inicial falhou com MiniStack indisponível e uma falha de prazo no transporte local; sem alterar testes/timeouts. Após subir somente MiniStack, comando completo passou. GUI em JVM separada no host.

`npm run check` passed: Prettier, markdownlint (103 documents), five documentation-checker tests, 419 local file/heading links after the final record update, Stylelint, ESLint, two contrast tests and 44 existing palette-pair checks. Initial Stylelint selector-order and ESLint unqualified-global findings were corrected without disabling rules. Palette checks cover solid surface endpoints, not a complete composited-pixel accessibility proof. / Checks passaram com essas contagens; ordem CSS e global JS corrigidos sem relaxar regras. Medição da paleta não equivale à prova de todos os pixels compostos.

Chrome host diagnostics passed all 10 tests in 32.5 seconds, with no failures/skips: eight language/theme/390-or-1440px combinations with dashboard/gallery axe scans, gradient/frame/focus/overflow checks and increased-contrast/forced-colors preferences; two no-JavaScript gallery cases. Sixteen diagnostic screenshots were generated, not compared with Linux baselines. Agent inspection covered English dark desktop dashboard and English light mobile gallery; not human acceptance. Browser checks do not claim reduced-transparency emulation, bucket-object coverage, full WCAG compliance or a benchmark. Logs/captures are ignored local artifacts in `work/`.

Chrome local: dez testes passaram em 32,5 s, sem falhas/ignorados: oito combinações de idioma/tema/largura com axe/foco/layout/preferências, dois fallbacks sem JS. Dezesseis capturas diagnósticas, sem comparação Linux. Agente revisou dashboard escuro desktop e galeria clara mobile em inglês; não é aceite humano. Sem alegar emulação de transparência reduzida, cobertura de objetos, conformidade WCAG completa ou benchmark. Logs/capturas locais ignorados em `work/`.

## Remaining acceptance / Aceite restante

Pinned Linux image-by-image baseline review, full container/new-image validation and human accessibility/design acceptance remain pending. Existing screenshots are intentionally unchanged; CI screenshot comparisons may differ until deliberate final review. No tolerances are relaxed. V2 does not start while V1 acceptance is open. Legal/name validation and D2–D6 remain pending.

Revisão individual das baselines Linux, validação completa com imagem nova e aceite humano continuam pendentes. Capturas de referência preservadas; comparação visual CI pode divergir até revisão final deliberada. Sem relaxar tolerâncias. V2 aguarda aceite V1. Nome/jurídico e D2–D6 pendentes.
