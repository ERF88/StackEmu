# E3 — overview and connection / Visão geral e conexão

Date / Data: 2026-10-14.

## Scope / Escopo

Responsive bento cards show the last bucket listing result, effective redacted endpoint/region, returned bucket count and contextual next action. The existing `/`, `/connection` and `/buckets` routes remain. The checked-template names remain; dashboard/partial parameters now also receive the existing `ConnectionSettingsView` through constructor injection, with no extra SDK operation. One atomic `#bucket-panel` fragment contains status, count and list. The refresh trigger remains outside the swapped fragment.

Cards responsivos apresentam resultado da última listagem, endpoint/região efetivos sem dados sensíveis, quantidade retornada e próxima ação contextual. Rotas e nomes dos templates permanecem. Os parâmetros de dashboard/fragmento passam a receber `ConnectionSettingsView` existente por injeção no construtor, sem nova operação SDK. Estado, contagem e lista compartilham `#bucket-panel`; o controle de atualização permanece fora do fragmento substituído.

Unavailable means an unknown count, not zero. This is not continuous monitoring or proof of general emulator compatibility. There are no object totals, storage estimates, connection editing, new services, runtime dependencies or first-party runtime scripts. Credentials remain hidden; configuration changes require restart.

Indisponibilidade significa quantidade desconhecida, não zero. Não é monitoramento contínuo nem prova de compatibilidade geral. Não há totais de objetos, estimativas de armazenamento, edição da conexão, novos serviços, dependências de runtime ou scripts próprios no navegador. Credenciais ficam ocultas; alterações de configuração exigem reinicialização.

## Verification / Verificação

Actual local commands and results / Comandos locais executados e resultados:

- `mvn -B -ntp spotless:apply`: passed / aprovado.
- `STACKEMU_INTEGRATION=true` with `mvn -B -ntp verify` on Java 25: BUILD SUCCESS, 57 tests, zero failures/errors/skips, local MiniStack only. / 57 testes, sem falhas, erros ou testes ignorados, apenas MiniStack local.
- `docker compose up -d --build gui`: passed; rebuilt GUI running locally. / aprovado; GUI reconstruída em execução local.
- `npm run format` and `npm run check` through `node:24.10.0`: passed, including Markdown/CSS/JavaScript checks, two contrast reference tests and 44 contrast rows. / aprovados, incluindo duas verificações de referência e 44 linhas de contraste.
- `./scripts/check-style.ps1` and `git diff --check`: passed / aprovados.
- Full Playwright suite through `mcr.microsoft.com/playwright:v1.64.1-noble`: 13 passed. Includes English/Portuguese, light/dark, axe audits, retained keyboard refresh focus, real returned-count/list consistency, responsive/theme preference checks and local S3 write/download fixtures. / 13 aprovados, incluindo idiomas, temas, axe, foco após atualização, correspondência contagem/lista, responsividade e fixtures S3 locais.

The HTTP unavailable profile verifies unknown count rather than zero, effective endpoint/region and absence of its test secret. Desktop dark and mobile light screenshots were visually inspected; artifacts are under `work/browser-results/` (ignored/generated). / O perfil HTTP indisponível verifica contagem desconhecida, endpoint/região e ausência do segredo de teste. Capturas desktop escura e mobile clara foram inspecionadas; artefatos gerados ficam em `work/browser-results/`.

## Limits / Limites

Browser/axe checks are partial evidence, not full WCAG certification. No vulnerability scan, load/capacity test or separate bounded-heap download test is claimed for E3. Existing E1/E2 evidence remains historical. No GitHub publication was performed in this stage. Legal validation of the StackEmu name is still pending.

Testes de navegador/axe são evidência parcial, não certificação WCAG. E3 não declara novo scan de vulnerabilidades, teste de carga/capacidade nem teste separado de download com heap limitado. Evidências E1/E2 permanecem históricas. Esta etapa não publicou no GitHub. A validação jurídica do nome StackEmu continua pendente.
