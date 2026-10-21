# Phase 4 — quality and security / Qualidade e segurança

## Scope / Escopo

Keep the existing S3 vertical slice and package boundaries. Browser checks add evidence for real form navigation, original Unicode filenames/download bytes, CSRF rejection, field-error associations, keyboard skip/refresh, mobile overflow and automated accessibility in English/pt-BR and light/dark. Browser security headers are defense-in-depth, not authentication. No production endpoints, deletion UI, new service or release is introduced.

Manter o vertical slice e os limites existentes. Testes de navegador verificam navegação/formulários reais, nome Unicode/bytes do download, recusa CSRF, associação de erros, teclado, viewport móvel e acessibilidade automática nos dois idiomas/temas. Cabeçalhos são defesa adicional, não autenticação. Sem endpoints de produção, UI de exclusão, novo serviço ou release.

## Reproduce / Reproduzir

With Java 25 and local MiniStack, run the established `STACKEMU_INTEGRATION=true` Maven verification and style checks first. Build/start Compose, then use the pinned Playwright image (matching the npm package):

```powershell
docker compose up --build -d --wait
docker run --rm --ipc=host -v "${PWD}:/repo" -w /repo -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble bash -lc "npm ci --ignore-scripts --no-audit --no-fund && npm run test:browser"
```

Linux CI uses `--network host` with the default loopback URLs. URLs are restricted to explicit local hostnames/ports; never change the guard to test AWS. One worker and no retry keep fixture/evidence behavior explicit. Cleanup sends deletes only for exact keys in the UUID-owned bucket; it does not enumerate or remove user resources. MiniStack accepts these unsigned cleanup requests; that is emulator behavior, not an authorization guarantee. Inspect any failed cleanup before rerunning.

O CI Linux usa rede host e URLs loopback. Não remover as restrições de destino para testar AWS. Um worker, sem retry. A limpeza exclui apenas chaves exatas no bucket UUID criado pelo teste; não enumera dados do usuário. Requisições de limpeza sem assinatura são comportamento do MiniStack, não garantia de autorização. Verificar falhas de limpeza antes de repetir.

Reports, axe findings, screenshots on failure and traces are under ignored `work/`; they may contain fixture data and effective local endpoint information. Keep them local or review before sharing. At the original Phase 4 baseline, browser JavaScript was test tooling only; runtime remained Qute/CSS/htmx. Redesign E5 later adds the small progressive-feedback `app.js`, also covered by ESLint/Prettier; see [E5 evidence](../evidence/redesign-e5-results.md).

Relatórios, resultados axe, screenshots de falha e traces ficam em `work/` ignorado. Podem conter dados de fixture/endpoint local: revisar antes de compartilhar. Na Fase 4 original, JavaScript novo existia apenas nas ferramentas de teste. A E5 do redesign adiciona posteriormente `app.js` para feedback progressivo, também com ESLint/Prettier; evidências no documento vinculado.

## Gate and limits / Gate e limites

Automated axe findings must be resolved, not suppressed to pass. Keyboard assertions are scripted checks, not manual assistive-technology validation. Chromium is the initial browser baseline; Firefox/WebKit, screen readers, 200%/400% zoom and a human keyboard/focus review remain explicit follow-ups. A performance sample is not a load benchmark. Do not declare full Phase 4 gate completion until the manual checklist and measurements have evidence.

Resolver achados axe, sem ocultá-los. Asserções de teclado são automáticas, não validação manual com tecnologia assistiva. Chromium é o baseline inicial; outros engines, leitores de tela, zoom 200%/400% e revisão humana permanecem pendentes. Amostras de desempenho não são benchmark de carga. Não declarar o gate completo sem evidências manuais e medições.

References: [Playwright Docker](https://playwright.dev/docs/docker), [axe API](https://www.deque.com/axe/core-documentation/api-documentation/).
