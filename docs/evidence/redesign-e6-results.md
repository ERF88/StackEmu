# E6 — gallery, accessibility and visual contracts / Galeria e contratos

Date / Data: 2026-10-15.

## Scope / Escopo

The bilingual gallery demonstrates actions, native disabled/readonly/invalid fields, file selection, confirmation, preview, status/error/warning/info, loading, empty, breadcrumb, scrollable table and native disclosure. On-page keyboard/ARIA contracts supplement the development guide. Hover/focus are actual interactions. No new runtime library, icon, font, route or application/SDK contract is added. Existing icon attribution covers screenshots too.

A galeria bilíngue demonstra ações, campos/estados nativos, seleção de arquivo, confirmação, prévia, status/erro/aviso/info, carregamento, vazio, breadcrumb, tabela e painel expansível. Contratos junto aos exemplos complementam o guia. Hover/foco são reais. Sem nova biblioteca, ícone, fonte, rota ou contrato de aplicação/SDK. Atribuição existente também acompanha capturas.

Java tests generate 32 ignored HTML documents for eight scenes, two languages and two themes. Connected overview/connection/bucket/object use deterministic presentation records, not emulator facts. Gallery/form and unavailable/error are local HTTP captures; failures use the genuine unavailable profile, without stopping MiniStack or adding a production test endpoint. Visual tests reuse the packaged GUI assets/current CSP. The baseline matrix has 80 PNG comparisons: 64 full-page scenes plus 16 gallery hover/focus captures, in 1440/390px and both languages/themes. Controls also exercise native checkbox/disclosure keys.

Java gera 32 HTMLs ignorados para oito telas, idiomas e temas. Registros conectados são fixtures de apresentação, não fatos do emulador. Galeria/formulário/indisponibilidade/erro usam HTTP local, com perfil indisponível real sem parar MiniStack ou criar endpoint de teste no produto. Assets/CSP são da GUI local. Matriz de 80 PNGs: 64 telas e 16 estados hover/foco em duas larguras/idiomas/temas; checkbox/disclosure recebem teclado.

The unchanged CI browser command runs the new comparisons; existing artifact retention now includes screenshot/diff/trace outputs. ADR-0012 only proposes P2 headers, preference cookies, measurement review and contribution conventions. None are enabled by E6. / O comando de navegador existente executa as comparações; retenção de artefatos passa a incluir saídas visuais. ADR-0012 apenas propõe P2, sem ativação.

## Verification / Verificação

Executed locally / Executado localmente:

- `mvn -B -ntp spotless:apply`: passed / aprovado.
- `docker compose up -d ministack --wait`, followed by `STACKEMU_INTEGRATION=true` with `mvn -B -ntp verify` on Java 25: BUILD SUCCESS, 58 tests, zero failures/errors/skips; 32 Qute fixture HTML files generated. / MiniStack saudável, 58 testes aprovados e 32 fixtures geradas.
- `docker compose up -d --build gui`: passed; packaged E6 GUI running locally. / aprovado; GUI E6 local em execução.
- `npm run format` and `npm run check` through `node:24.10.0`: passed, including Markdown, Stylelint, ESLint, two contrast reference tests and 44 contrast rows. / aprovados, incluindo dois testes de referência e 44 linhas de contraste.
- `./scripts/check-style.ps1`, `git diff --check` and `rhysd/actionlint:1.7.7`: passed / aprovados.
- Snapshot creation used the documented Playwright container with `test visual.spec.mjs --update-snapshots`; corrections are recorded below. Final normal full suite used `test` WITHOUT updating: 82 passed, zero retries. Includes 64 independent visual scene tests/80 comparisons, native gallery keyboard checks, actual gallery and unavailable/error axe audits, original-file S3 flows, themes, preferences, feedback and no-JavaScript validation. / Suíte final normal SEM atualizar: 82 aprovados, sem retries; 64 casos visuais, 80 comparações e regressões existentes.

Final representative captures inspected visually: dark desktop gallery and focused action, light Portuguese mobile focused action, and light Portuguese mobile unavailable overview. All 80 final references were compared automatically; full human visual approval is not claimed. PNGs are under `tests/browser/baselines/visual.spec.mjs/`; HTML and reports remain ignored under `work/`. / Capturas representativas foram inspecionadas e 80 referências comparadas automaticamente; sem declarar aprovação humana integral. PNGs no diretório indicado; HTML/relatórios em `work/` ignorado.

## Fixture corrections / Correções das fixtures

Initial snapshot generation technically completed but visual inspection rejected the unstyled images. Diagnostics showed actual GUI CSS loaded normally, while intercepted-document asset requests were blocked by Chromium local-network checks. The test router now fulfills real HTTP asset responses too, retains the actual GUI CSP, asserts computed sidebar styling, checks asset byte hashes and fails on blocked/CSP/missing-resource console errors. No application behavior or browser security flag was weakened. The rejected captures were replaced.

A primeira geração terminou tecnicamente, mas a inspeção rejeitou imagens sem estilo. A GUI real carregava CSS normalmente; navegação interceptada teve assets bloqueados por verificações de rede local do Chromium. O roteador entrega respostas reais dos assets, preserva CSP e verifica estilo, hashes e erros de carregamento. Sem relaxar runtime ou segurança; capturas rejeitadas substituídas.

Corrected grouped generation had seven passing groups and one cumulative 60-second timeout after writing captures. The matrix was split into one test per scene without increasing timeouts or visual tolerances. The final 82-test normal comparison passed against the styled references, with no update flag. / Geração corrigida agrupada teve sete aprovações e um timeout acumulado de 60s. A matriz passou a um teste por tela, mantendo limites e tolerância; execução normal final de 82 testes passou contra referências estilizadas, sem atualização.

## Initial environment failure / Falha inicial de ambiente

The first Java verification ran while local MiniStack was stopped: 58 tests, seven failures and nine errors, with connection-refused/503 integration results. The fixture-producing unavailable profile passed. Compose subsequently started MiniStack; health was explicitly confirmed with `docker compose up -d ministack --wait` before a full rerun. No assertion was weakened and no integration was skipped. / A primeira execução encontrou MiniStack parado: sete falhas e nove erros em 58 testes, com conexão recusada/503. Fixtures do perfil indisponível passaram. Serviço iniciado e saúde confirmada antes da repetição completa, sem relaxar asserções ou ignorar integração.

## Limits / Limites

Generated/recompared snapshots are regression evidence, not aesthetic approval or full WCAG certification. Screen-reader review, wider browsers/OS/architecture and complete human baseline approval remain separate. Fixed test records are not user research, metrics, latency or compatibility results. No new vulnerability scan, load/heap proof, GitHub publication or P2 runtime change is claimed. Historical E1–E5 evidence stays unchanged; StackEmu legal/name validation remains pending.

Snapshots gerados/comparados não aprovam estética nem certificam WCAG. Revisão de leitores de tela, outros ambientes e aprovação humana integral são separadas. Dados fixos não são pesquisa, métricas ou compatibilidade. Sem novo scan, prova de carga/heap, publicação ou P2 em runtime. Histórico preservado e validação jurídica/do nome pendente.
