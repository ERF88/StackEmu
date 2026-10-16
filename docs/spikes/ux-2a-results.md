# UX-2A validation / Validação UX-2A

Date / Data: 2026-10-15. Scope: native object table only, not the complete expanded operational delivery. / Escopo: tabela nativa, não a entrega operacional ampliada completa.

## Changes / Alterações

Localized IEC sizes retain exact bytes; UTC dates retain the existing policy. Filename-type badges are unverified presentation hints, not MIME validation. Native per-object attachment links preserve opaque keys, language/theme and full-page details without JavaScript. No new routes, SDK requests, mutations or drawer were introduced. See [contracts](../development/s3-explorer.md) and [expanded operational scope](../product/s3-management-scope.md).

Tamanhos IEC localizados preservam bytes exatos, com política UTC existente. Badges são apenas indícios pelo nome, sem validar MIME. Download nativo por objeto preserva chaves, idioma/tema e detalhes completos sem JavaScript. Sem novas rotas, consultas SDK, mutações ou drawer.

## Execution evidence / Evidência de execução

- Java 25 verification with local MiniStack and `STACKEMU_INTEGRATION=true`: 63 tests, zero failures/errors/skips. New mapper cases cover IEC boundaries, exact localized bytes, long maximum, suffix hints and encoded Unicode keys. Existing integration tests do not prove future preview, deletion or signing safety.
- Verificação Java 25 com MiniStack local: 63 testes, sem falhas/erros/ignorados. Casos novos cobrem limites IEC, localização, valor long máximo, sufixos e chaves Unicode. Não comprovam segurança das funcionalidades futuras.
- Frontend checks initially rejected CSS selector order; source order was corrected without disabling the rule. Final `npm run check` passed: formatting, 86 Markdown files, CSS/JavaScript lint, two contrast reference tests and 44 palette pairs. PowerShell analyzer, Hadolint (both Dockerfiles), actionlint, Compose configuration and whitespace checks passed.
- Checks iniciais rejeitaram ordem dos seletores CSS; correção sem desabilitar regra. Checks finais passaram: formatação, 86 Markdown, lint CSS/JS, dois testes e 44 pares de contraste, análise PowerShell, Hadolint, actionlint, Compose e espaços.
- First Docker rebuild failed while resolving Maven plugin dependencies; retry and final-source rebuild passed. Docker packaging skips tests and does not replace Maven verification. Final local smoke/container-policy/write scripts passed, including original-file SHA-256 and CSRF rejection. Generated and served CSS matched: 18,910 raw bytes, 4,505 gzip bytes, SHA-256 `fe96ba6bd0f5a7ca79e3ddfc43e8ac6088fafa8ea1583af4e2c94537a9306a48`; artifact size, not performance evidence.
- Primeiro rebuild Docker falhou na resolução Maven; repetição e rebuild final passaram. Scripts locais de smoke/container/escrita passaram, incluindo SHA-256/CSRF. CSS gerado e servido idêntico, medidas acima sem alegação de desempenho.
- Initial focused browser run failed because a broad test selector matched both the file and marker-object download. Changed it to the exact accessible name/key; the repeated focused test passed, including original basename/extension/hash, en/pt-BR, light/dark, 360/390/1440/1920px and native download/detail navigation without JavaScript.
- Teste focado inicial falhou por seletor amplo que correspondia ao arquivo e ao objeto marker. Seleção por chave/nome acessível exatos corrigida; repetição passou, incluindo arquivo original, idiomas/temas/larguras e navegação/download sem JS.
- Deliberate Linux update of only eight bucket screenshots passed (8 tests); prior baselines remain in ignored `work/ux2a-baselines-before/`. Hash comparison confirmed only these eight changed. Reviewed desktop English and mobile Portuguese examples in both themes. Final full regression without update: 84 tests passed in 3.2 minutes, zero failures/skips/flaky results, including all 80 screenshot comparisons. No tolerance, retry or mask was loosened. Local report: `work/browser-report.json`.
- Atualização deliberada só das oito capturas de bucket passou; cópia anterior preservada no diretório ignorado. Comparação de hashes confirmou apenas essas oito mudanças. Revistos exemplos desktop en/mobile pt-BR nos dois temas. Regressão final sem atualização: 84 testes passaram em 3,2 minutos, sem falhas/ignorados/flaky, incluindo 80 comparações visuais; sem relaxar tolerâncias/retries/máscaras. Relatório local acima.

Reproduction: Java 25 `mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'` with `STACKEMU_INTEGRATION=true` and `MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'`, then `docker compose up -d --build gui`. Browser command: `docker run --rm --ipc=host --memory=768m -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test`. No snapshot-update flag belongs in normal verification. / Reprodução pelos comandos acima, somente GUI/MiniStack locais, sem flag de atualização na verificação normal.

## Remaining acceptance / Aceite restante

UX-2B drawer/clipboard, drop/progress, preview, signed downloads, copy/move and deletion remain required but not implemented by this slice. Their technical/security gates remain mandatory. No new vulnerability scan, load test, production compatibility, publication, human acceptance or legal/name validation is claimed. StackEmu name validation remains pending.

Drawer/clipboard, drop/progresso, preview, downloads assinados, cópia/movimentação e exclusão continuam obrigatórios, mas não implementados nesta fatia. Gates técnicos/de segurança permanecem. Sem alegação de novo scan, carga, compatibilidade de produção, publicação, aceite humano ou validação jurídica/do nome; nome StackEmu ainda pendente.
