# E4 — resource tables and details / Tabelas e detalhes

Date / Data: 2026-10-14.

## Scope / Escopo

Bucket, object and existing read-only tag tables use captions, row/column headers, labelled keyboard-focusable scroll regions and opaque sticky headers. Row hover and keyboard focus share a surface highlight. Resource links have a minimum 24px height. Breadcrumbs distinguish the current location; existing first/next cursor navigation remains, without invented previous pages or counts. Prefix and page size use existing data. Object details separate metadata/actions responsively, highlighting the existing attachment download.

Tabelas de buckets, objetos e tags existentes usam captions, cabeçalhos de linha/coluna e regiões rotuladas, focáveis e roláveis por teclado. Cabeçalhos fixos são opacos; hover/foco destacam a linha. Links de recursos têm altura mínima de 24px. Breadcrumb identifica a localização atual e a paginação mantém os cursores existentes, sem páginas/contagens inventadas. Prefixo/limite usam os dados atuais. Detalhe separa metadados/ações responsivamente e destaca o download como anexo.

No Java, SDK, HTTP route, checked-template signature, upload/download policy, CSP, dependency or third-party asset changes are introduced in E4. Keys remain escaped and verbatim in monospace. No copy button/runtime script is added: CLI examples remain in the existing mutation screens. The duplicate bucket-details heading is removed while preserving its native disclosure.

E4 não altera Java, SDK, rotas, assinaturas Qute, políticas de upload/download, CSP, dependências ou assets externos. Chaves continuam escapadas, integrais e monoespaçadas. Sem botão copiar/script de runtime; exemplos CLI permanecem nas telas de mutação. O título duplicado de detalhes foi removido, preservando o disclosure nativo.

## Verification / Verificação

Executed locally / Executado localmente:

- `mvn -B -ntp spotless:apply`: passed / aprovado.
- `STACKEMU_INTEGRATION=true` with `mvn -B -ntp verify` on Java 25: BUILD SUCCESS; 57 tests, zero failures/errors/skips, local MiniStack only. / 57 testes, sem falhas, erros ou testes ignorados, apenas MiniStack local.
- `docker compose up -d --build gui`: passed; E4 packaged GUI running locally. / aprovado; GUI E4 empacotada em execução local.
- `npm run format` and `npm run check` through `node:24.10.0`: passed; Prettier, Markdown, Stylelint, ESLint, two contrast reference tests and 44 contrast rows. / aprovados, incluindo duas verificações de referência e 44 linhas de contraste.
- `./scripts/check-style.ps1` and `git diff --check`: passed / aprovados.
- Full Playwright suite through `mcr.microsoft.com/playwright:v1.64.1-noble`: 13 passed, with zero retries. The existing isolated S3 browser flow now includes a long Unicode/special-character filename, original download filename/bytes, semantic table checks, current breadcrumb, sticky-header computed style, ArrowRight keyboard scrolling, page overflow at 360/390/1440/1920px and bucket/object axe audits for both languages/themes. / 13 aprovados, sem retries; fluxo S3 isolado ampliado com nome longo/Unicode, download original, semântica, breadcrumb atual, estilo sticky, rolagem por teclado, larguras e axe nos dois idiomas/temas.

Screenshots are generated under `work/browser-results/quality-browser-S3-writes--2bc0b-oad-validation-and-security/`, named `e4-{bucket|object}-{en|pt-BR}-{dark|light}-{390|1440}.png`. The mobile dark bucket and desktop light Portuguese object screenshots were visually inspected. Generated artifacts remain ignored, not visual regression baselines. / Capturas nesses caminhos foram geradas; tabela mobile escura e detalhe desktop claro em português foram inspecionados. São artefatos ignorados, não baselines de regressão visual.

Fixture cleanup deletes only the UUID-owned bucket and exact keys created by that test; no production endpoint is used. / Limpeza restrita ao bucket UUID e às chaves exatas da fixture; sem endpoint de produção.

## Limits / Limites

Automated browser/axe evidence does not certify full WCAG compliance, screen-reader support or all browsers. No new vulnerability scan, load test, emulator certification or bounded-heap streaming proof is claimed. E1–E3 evidence remains historical. No GitHub publication in this stage. StackEmu legal/name validation remains pending.

Testes automáticos de navegador/axe não certificam WCAG integral, leitores de tela ou todos os navegadores. Sem novo scan, teste de carga, certificação de emulador ou prova de streaming com heap limitado. Evidências E1–E3 permanecem históricas. Sem publicação no GitHub nesta etapa. Validação jurídica/do nome StackEmu continua pendente.
