# Visual regression / Regressão visual

Use the same `mcr.microsoft.com/playwright:v1.64.1-noble` Linux container as CI. Host Windows/macOS fonts and rendering are not baseline environments. Linux alone does not guarantee equivalence: container/browser version, architecture and font changes require review. / Usar o mesmo container Linux do CI. Windows/macOS não são ambientes de geração; versão, arquitetura e fontes exigem revisão.

## Fixture boundary / Limite das fixtures

Java verification writes ignored `work/browser-fixtures/` HTML using actual Qute templates. Overview/connection/bucket/object use fixed presentation records, not emulator measurements. Gallery/form and unavailable/error are captured through local HTTP tests; unavailable/error use the real unavailable endpoint profile. HTML may contain ephemeral development CSRF tokens: do not commit or share these files. Baselines are PNGs under `tests/browser/baselines/visual.spec.mjs/`, with no raw tokens. / Java gera HTML ignorado com templates reais. Registros fixos são dados de apresentação, não medições. Galeria/formulário/indisponibilidade/erro usam HTTP local; erro usa perfil indisponível real. Não versionar HTML com tokens efêmeros; versionar apenas PNGs.

Eight scenes × two languages × two themes × two viewports (1440/390), plus real gallery hover/focus, yield 80 snapshots. Dates, names, endpoint and listings are fixed; no broad data masks hide layout. Actual CSS/JS/favicon come from the packaged local GUI, using its current CSP. Existing live S3 tests remain separate. / Oito telas, idiomas, temas e larguras, mais hover/foco, geram 80 imagens. Dados fixos evitam máscaras amplas. Assets/CSP vêm da GUI local; testes S3 reais permanecem separados.

## Commands / Comandos

PowerShell, Java 25, local MiniStack only / apenas MiniStack local:

```powershell
$env:STACKEMU_INTEGRATION='true'
docker compose up -d ministack --wait
mvn -B -ntp verify
docker compose up -d --build gui
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test
```

Missing fixtures fail the test: rerun Java verification after template/CSS/script changes. Do not replace missing fixtures with invented success HTML. / Fixtures ausentes causam falha; executar Java novamente após mudanças. Não fabricar HTML de sucesso.

Fixture navigation and real HTTP asset responses are fulfilled by the test router: direct local asset loading after intercepted navigation was rejected by Chromium local-network checks in this environment. No browser security flag or application CSP is disabled. A computed sidebar-style guard and browser error checks reject unstyled or blocked captures before comparison. Browser locale is fixed to en-US, timezone UTC and scale 1; first-party labels still follow the explicit UI language. Native file-control text follows the browser locale. / O roteador de teste entrega documentos e respostas reais dos assets porque o Chromium bloqueou a carga local após navegação interceptada. Sem desligar segurança/CSP. Guards rejeitam capturas sem CSS ou com bloqueios. Locale do navegador en-US, UTC e escala 1; rótulos próprios seguem o idioma da UI, e controles nativos seguem o navegador.

To deliberately create/update snapshots / Para criar ou atualizar deliberadamente:

```powershell
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test visual.spec.mjs --update-snapshots
```

Then run the normal command without updating. Review baseline/actual/diff images under `work/browser-results/`; approve changes individually before committing. Never update snapshots automatically to make CI green. `threshold=0.1` and `maxDiffPixelRatio=0.001` allow small rasterization differences, not layout redesign. Animations/carets are suppressed only for screenshot stability; reduced-motion behavior also has independent tests. Initial generation plus self-comparison is not visual design approval. / Reexecutar sem atualizar e revisar imagens/diffs antes de versionar. Não atualizar automaticamente para passar no CI. Tolerância baixa não autoriza redesign. Estabilização visual não substitui testes de movimento; gerar/comparar não equivale à aprovação estética.

For an intentional whole-palette migration such as UX-1, use explicit `--update-snapshots=all` instead of the bare flag: pinned Playwright defaults the bare flag to `changed`, retaining differences below comparison tolerance. Preserve the previous baseline in ignored `work/` for review, then compare normally without any update flag. Do not change tolerances, mask content or treat regeneration as acceptance. / Na migração deliberada de paleta UX-1, usar explicitamente `--update-snapshots=all`: a opção sem valor usa `changed` e pode preservar diferenças abaixo da tolerância. Guardar baseline anterior em `work/`, revisar e comparar normalmente, sem alterar tolerância/mascarar conteúdo ou confundir geração com aceite.
