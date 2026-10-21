# E7 — final redesign validation / Validação final do redesenho

Date / Data: 2026-10-15.

## Outcome and scope / Resultado e escopo

Technical validation of E1–E6 passed against the packaged local GUI and MiniStack. E7 changes documentation only: this record, design-system/testing references and both changelogs. It does not enable P2 proposals, change runtime contracts, approve a release or publish to GitHub. Earlier stage evidence is unchanged.

A validação técnica E1–E6 passou com GUI empacotada e MiniStack locais. A E7 altera apenas documentação: este registro, referências de design/testes e os dois changelogs. Não ativa P2, altera contratos de runtime, aprova release ou publica no GitHub. Evidências anteriores permanecem intactas.

Environment / Ambiente: Windows host, Temurin Java 25.0.4.1, Maven 3.9.9, Node container `node:24.10.0`, Chromium in `mcr.microsoft.com/playwright:v1.64.1-noble`, MiniStack 1.5.25. GUI/emulator ports bind to loopback. Integration targeted local MiniStack only; no production resources.

## Executed verification / Verificação executada

- `docker compose up -d ministack --wait`: healthy before integration. / Saudável antes da integração.
- `$env:STACKEMU_INTEGRATION='true'; mvn -B -ntp clean verify`: BUILD SUCCESS; 58 tests, zero failures/errors/skips. Includes Spotless, checked Qute compilation, translations, architecture and local integration. CycloneDX XML/JSON SBOM generated with 152 components. / 58 testes aprovados, sem falhas/erros/ignorados; SBOM gerado.
- `docker compose up -d --build gui`: passed; validated rebuilt local image. / GUI reconstruída e validada localmente.
- `docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.10.0 npm run check`: passed; Prettier, markdownlint, Stylelint, ESLint, two contrast reference tests and 44 contrast rows. Minimum text ratio 6.24:1; border 3.32:1; focus 7.42:1. These opaque token pairs are not a full-page WCAG certification. / Checks aprovados; razões dos tokens não certificam WCAG da página inteira.
- `./scripts/check-style.ps1`, `git diff --check`, `docker compose config --quiet`: passed. / Aprovados.
- `docker run --rm -v "${PWD}:/repo" -w /repo hadolint/hadolint:v2.12.0 hadolint Dockerfile` and `docker run --rm -v "${PWD}:/repo" -w /repo rhysd/actionlint:1.7.7`: passed. / Aprovados.
- `./scripts/verify-container.ps1`: passed non-root UID, read-only root, bounded temporary storage/memory/PIDs, dropped capabilities, no privilege escalation/socket, loopback and license checks. / Política do container aprovada no escopo do script.
- `./scripts/smoke.ps1`: passed GUI, localized htmx fragment, process health, connection configuration and gallery. / Smoke aprovado.

Full browser command, WITHOUT baseline updates / Comando completo de navegador, SEM atualizar baselines:

```powershell
docker run --rm --ipc=host -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_URL=http://host.docker.internal:8080 -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node node_modules/@playwright/test/cli.js test
```

Result / Resultado: **82 passed (6.6m)**. Includes 64 visual-scene tests/80 PNG comparisons, workspace, axe navigation/gallery/unavailable/error audits, local S3 writes/download, shell/assets, preferences, progressive feedback and no-JavaScript validation. No snapshot regeneration or assertion/tolerance relaxation was needed. / Suíte aprovada sem regenerar imagens nem relaxar asserções/tolerâncias.

Maven emitted native-access/Unsafe and CycloneDX schema-keyword warnings; verification still completed successfully. No new vulnerability scan is inferred from SBOM generation. / Houve avisos de native-access/Unsafe e palavras-chave do esquema CycloneDX; build concluído. Gerar SBOM não equivale a executar scan.

## Final checklist / Checklist final

| Item / Item                                                 | Executed evidence / Evidência executada                                                                                                                                                      | Remaining limit / Limite restante                                                                   |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Keyboard and focus / Teclado e foco                         | Automated skip-link, focus retention after refresh, table arrow scrolling, native checkbox/disclosure and visible gallery focus / Skip-link, foco, rolagem e controles nativos automatizados | Not an exhaustive human keyboard/screen-reader session / Não substitui sessão humana integral       |
| Theme/language URL / Tema e idioma por URL                  | en/pt-BR × dark/light, explicit overrides, auto/system and preference media tests / Idiomas, temas, overrides e preferências testados                                                        | Other browser/OS combinations unverified / Outros ambientes não verificados                         |
| Responsive and long names / Responsividade e nomes extensos | Live UUID bucket and long Unicode object; 360/390/1440/1920px overflow checks; original name/extension, MIME, bytes and attachment download checked / Fluxo real e integridade verificados   | Not every valid S3 key or maximum-size name / Não cobre toda chave válida ou nome máximo            |
| Unavailable emulator / Emulador indisponível                | Actual local unavailable-profile HTTP responses, axe, unknown count instead of zero and visual comparisons / Respostas reais do perfil indisponível, axe e imagens                           | Fixtures do not certify all network failure modes / Fixtures não certificam todos os modos de falha |
| Visual review / Revisão visual                              | 80 automatic comparisons; representative inspection listed below / 80 comparações e amostras inspecionadas                                                                                   | Full human aesthetic acceptance pending / Aceite estético humano integral pendente                  |

The automated checklist is not labelled as a completed manual interaction audit. / O checklist automatizado não é apresentado como auditoria manual de interação concluída.

## Screenshots and artifacts / Capturas e artefatos

Current-run captures inspected: [live desktop overview](../../work/browser-results/shell-assets-localized-she-1c884--fingerprinted-assets-pt-BR/e2-shell-pt-BR-dark-1440.png) and [live long-name object on mobile](../../work/browser-results/quality-browser-S3-writes--2bc0b-oad-validation-and-security/e4-object-pt-BR-dark-390.png). No visible clipping/overlap issue was identified in these samples. The local bucket listing is development data, not a product metric. / Capturas atuais inspecionadas sem problema visível de corte/sobreposição nas amostras; listagem local não é métrica de produto.

Existing references also inspected: [dark desktop overview](../../tests/browser/baselines/visual.spec.mjs/overview-pt-BR-dark-1440.png), [light mobile bucket](../../tests/browser/baselines/visual.spec.mjs/bucket-pt-BR-light-390.png), [light mobile unavailable](../../tests/browser/baselines/visual.spec.mjs/unavailable-pt-BR-light-390.png) and [keyboard focus](../../tests/browser/baselines/visual.spec.mjs/gallery-focus-pt-BR-light-390.png). These use the documented fixed presentation fixtures, not live connectivity facts. / Referências inspecionadas usam fixtures documentadas, não fatos de conectividade ao vivo.

All 80 versionable references remain under `tests/browser/baselines/visual.spec.mjs/`. Local screenshots/axe attachments/report remain under ignored `work/browser-results/` and `work/browser-report.json`; subsequent runs may replace them. CI retains browser results through the existing artifact policy. The 32 ignored HTML fixtures may contain ephemeral development CSRF tokens: do not commit or distribute them. / Referências versionáveis e artefatos locais separados; novas execuções podem substituir capturas. Não distribuir HTML com tokens efêmeros.

## Acceptance and next step / Aceite e próximo passo

E7 technical checks passed; final product/visual acceptance is not automatic. Review the running GUI and screenshots, perform the remaining human keyboard/assistive-technology review, then explicitly approve versioning/publication. Keep use restricted to trusted local development. Name/legal validation remains pending. ADR-0012 remains proposed, not approved or implemented.

Checks técnicos E7 aprovados; aceite final do produto/visual não é automático. Revisar GUI/capturas e concluir revisão humana de teclado/tecnologias assistivas antes de aprovar versionamento/publicação. Uso continua restrito ao desenvolvimento local confiável. Validação jurídica/do nome pendente. ADR-0012 continua proposta, sem aprovação ou implementação.

No new vulnerability scan, dedicated 128 MiB/64 MiB streaming proof, load test, cross-browser/ARM64 verification, user research, security/WCAG certification, Git commit, push or release was performed in E7. Existing ordinary download/integration tests are not a replacement for the dedicated heap proof. / Não houve novo scan, prova dedicada de heap/streaming, carga, validação ampliada, pesquisa, certificação, commit, push ou release; testes comuns de download não substituem prova dedicada de heap.
