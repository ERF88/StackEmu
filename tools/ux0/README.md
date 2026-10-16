# UX-0 CSS decision spike / Spike de decisão CSS

This remains isolated comparison tooling, not an application module or production theme. Tailwind CSS/CLI 4.3.3 and Pico 2.1.1 have their own exact manifest/lockfile here; DaisyUI is not installed. The product separately adopts Tailwind through [UX-1 build rules](../../docs/development/frontend-build.md). Rerunning this spike uses current authored CSS; generated measurements must not overwrite the historical UX-0 evidence. / Ferramentas de comparação isoladas, com manifesto/lockfile próprios, não módulo/tema de produção. O produto adota Tailwind separadamente pela UX-1; executar o spike usa o CSS autoral atual, sem substituir as evidências históricas UX-0.

## Reproduction / Reprodução

Use Java 25 and the existing local MiniStack integration environment. The pure `CssPrototypeTest` also runs without an emulator when selected individually. PowerShell from repository root / Na raiz:

```powershell
docker compose up -d ministack --wait
$env:STACKEMU_INTEGRATION='true'
mvn -B -ntp verify
docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run spike:css
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 mcr.microsoft.com/playwright:v1.64.1-noble node tools/ux0/check.mjs
docker build -f tools/ux0/Dockerfile --target css-spike -t stackemu-css-spike:local .
```

The browser command requires the existing packaged GUI at loopback port 8080 and root Playwright/axe development dependencies. It reads the actual `/components` CSP, then serves isolated fixtures/assets through test routing; it does not add `/ux0` production endpoints or use emulator mutations. Do not disable CSP/browser security flags. Linux Playwright is required for repeatable captures. The Dockerfile proves the separate Maven fixture → Node CSS build chain, not a new GUI runtime image. CI is configured to run the same spike after Java fixtures and the ordinary browser suite; configured steps are not evidence of a remote CI execution.

O comando de navegador exige GUI local empacotada na porta 8080 e dependências de teste da raiz. Lê CSP real e entrega fixtures/assets pelo roteador de teste, sem criar endpoints `/ux0` ou mutações. Não desligar segurança. Capturas usam Playwright Linux; Dockerfile prova cadeia isolada, não nova imagem de produção. Configuração no CI não comprova execução remota.

## Source and output ownership / Fontes e gerados

- `src/test/resources/ux0/prototype.html`: real Qute gallery/table/form with bilingual fixtures; no credentials, mutation or production binding. / Qute real, sem credenciais/mutações/bindings de produção.
- `CssPrototypeTest`: renders 18 escaped fixtures to ignored `work/ux0-fixtures/`. / Gera 18 fixtures escapadas e ignoradas.
- `input.css`: Tailwind source only. The existing app reset/tokens remain authoritative; no Preflight. Both explicit dark and auto/system-dark variants are exercised. Complete literal classes in conditional branches are extractable; `p-{spacing}` is a deliberately unsupported negative control, never a production pattern. / Fonte sem Preflight; variantes explícita/automática e controle negativo de classe fragmentada.
- `build.mjs`: compiles twice and compares hashes; rejects missing literal selectors and accidental Preflight. Writes candidate CSS, fingerprinted fixture links, raw/gzip counts and copied direct-package MIT texts/hashes to ignored `work/ux0/`. / Compara dois builds, valida seletores/reset e gera CSS, hashes, tamanhos e licenças apenas em `work/`.
- `check.mjs`: current, additive Tailwind and additive Pico × en/pt-BR × dark/light/auto-dark/auto-light = 24 cases, each at 360/390/1440/1920px; 48 captures at 390/1440px. Current/Tailwind must pass overflow, theme and axe/CSP checks; Pico findings remain diagnostic so a rejected candidate does not masquerade as accepted or prevent comparing it. Native table focus/checkbox/disclosure are checked. / 24 casos; Pico é diagnóstico, não aprovação.

The comparison keeps the existing stylesheet in every candidate: it measures incremental adoption cost, NOT standalone framework sizes or a completed removal of old CSS. Intercepted DOMContentLoaded samples are diagnostics only; they do not measure production navigation, backend performance or user satisfaction. No numeric performance budget or ADR-0012 P2 policy is activated. / Comparação aditiva, não tamanho isolado dos frameworks nem migração concluída. Timings interceptados não são benchmark do produto; sem orçamento P2.

## Migration guardrails / Diretrizes de migração

Candidate axe/keyboard checks run at the last width (1920px); overflow/theme checks cover all four widths. The existing full application browser suite remains separate. `diagnose-gallery.mjs` reproduces first-capture layout diagnostics using actual Qute gallery fixtures/current CSP; diagnostic PNGs are local only. / Axe/teclado das candidatas usam 1920px, enquanto overflow/tema cobrem quatro larguras. Suíte do produto é separada; helper diagnóstico usa galeria Qute/CSP reais e gera apenas PNGs locais.

Keep root lint rules. Only this input file whitelists Tailwind directives; generated CSS is not authored component source. Real migration requires a scoped source/output path, local Maven/Docker/CI generation before packaging and integration with `WebAssetCatalog` content hashes. Never copy generated CSS into source by hand. Keep semantic token aliases until their consumers migrate; remove obsolete rules per component only after browser/contrast review. Use full literal utilities and reusable Qute fragments rather than building class names from emulator input. Keep en/pt-BR, native fallbacks, CSP, original-file/CSRF and adapter boundaries intact. Preview/delete/presigning remain separate gates. Legal/name validation remains pending.

Manter lint global; exceções de diretivas limitadas à entrada do spike. Migração real exige fonte/saída definidas e geração antes do empacotamento, com hash no catálogo, sem copiar gerados manualmente. Remover regras obsoletas por componente após revisão; não construir classes com dados do emulador. Preservar contratos/idiomas/segurança. Capacidades novas e validação jurídica seguem separadas.
