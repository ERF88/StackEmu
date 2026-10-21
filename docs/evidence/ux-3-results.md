# UX-3 execution / Execução UX-3

Date / Data: 2026-10-15. Scope: single-file selection/drop and transport progress; not multi-upload, resume, preview, deletion or complete operational S3 delivery. / Escopo: seleção/drop individual e progresso, não entrega S3 completa.

## Real transport spike / Spike real de transporte

Before runtime changes, the pinned Linux Playwright/Chromium container, bundled htmx 2.0.8 and local packaged GUI/MiniStack ran upload-transport.spec.mjs: one test passed. A real 1 MiB file with a Unicode/plus/percent basename was posted with confirmation/CSRF, then downloaded with matching SHA-256 and text/plain metadata. Observed native upload and response progress both forward under htmx:xhr:progress; a one-shot native-upload sample registered in beforeRequest correlated exactly with forwarded upload events. Response samples were distinct. Multipart sent bytes exceeded file size, as expected for framing. This verifies the narrow event contract, not upload throughput or production compatibility.

Antes das mudanças de runtime, um teste real passou com container fixado, GUI e MiniStack locais. Arquivo de 1 MiB com nome Unicode/+/%, confirmação/CSRF, hash e MIME preservados. Eventos de envio/resposta usam o mesmo nome htmx; amostra nativa de envio correlacionou exatamente com encaminhamento, sem confundir resposta. Não é benchmark/certificação.

## Implementation and verification / Implementação e verificação

[ADR-0016](../decisions/ADR-0016-single-file-upload-progress.md) and [development contract](../development/s3-upload.md) define selected/sending/processing/server-result states, controls/duplicate guards, native fallback and checked workspace/error recovery. No new application/SDK mutation port or production dependency is introduced. The browser never assigns a manual object key or reports success from 100% transport alone.

Contratos vinculados preservam fallback, confirmação, limites e arquivo original, sem nova porta/dependência. Navegador não cria chave manual nem confirma sucesso só por 100% enviado.

Initial style checks identified selector order, a redundant variable initialization and test formatting; corrected without weakening rules. Fresh Java 25 Maven verification against local MiniStack passed: 68 tests, zero failures/errors/skips. The final GUI image built successfully. Container hardening/license checks, smoke and native write/download/hash/CSRF scripts passed. Prettier, Markdown (91 files), CSS/JavaScript lint, two contrast tests/44 palette pairs, PowerShell analysis, Hadolint (both Dockerfiles), actionlint, Compose configuration and diff whitespace checks passed.

Checks iniciais corrigidos sem relaxar regras. Nova verificação Maven Java 25/MiniStack local: 68 testes, zero falhas/erros/skips; imagem final construída. Passaram checks do container/licenças, smoke e escrita/download/hash/CSRF nativos; Prettier, Markdown (91 arquivos), CSS/JS, dois testes de contraste/44 pares, PowerShell, Hadolint, actionlint, Compose e whitespace do diff.

Focused browser verification passed all ten tests. Earlier attempts exposed two fixture problems: overriding a single DataTransferItem did not survive browser wrapper iteration, and Playwright route interception suppressed native upload progress while holding the response. The directory fixture now temporarily overrides the browser entry API synchronously; it is not a real OS directory drag. The fixed-loopback Linux test proxy now streams the real request and holds only its upstream response, with token-validated local control endpoints, at most eight holds and a 20-second safety release. Its test header is stripped before forwarding; no helper route/header is added to the application. This verifies processing/duplicate guards before server confirmation, not emulator processing latency. Docker was temporarily unavailable; after restarting the local environment, focused verification and container/native scripts passed.

Os dez testes focados passaram. Tentativas anteriores expuseram problemas nas fixtures: override de item não persistia na iteração do navegador; interceptação Playwright suprimia progresso nativo enquanto segurava resposta. Fixture de pasta simula API de entrada, não drag real do sistema. Proxy de teste loopback transmite requisição real e retém somente resposta, com token, máximo oito retenções e liberação de segurança em 20 segundos; remove cabeçalho antes da GUI. Nenhuma rota/cabeçalho auxiliar entra na aplicação. Não mede latência do emulador. Docker indisponível temporariamente foi reiniciado; verificações locais passaram após retomada.

Coverage includes both languages/themes at 390/1440 pixels, keyboard picker, single/multiple/text/simulated-directory drop, no automatic submission, original Unicode/plus/percent filename and extension/MIME/hash, locked controls and duplicate submission, processing-not-success, real server 422/409 and CSRF 400, unknown/unmarked/network/413 responses, explicit retry and native no-JavaScript upload. Progress edge cases and the 40 ms shortened timeout are simulated fault fixtures; the configured 120000 ms policy is asserted, not waited out. Automated axe checks found no violations in the eight upload viewports. Full browser regression passed: 107 tests, zero failures/skips, including 80 screenshot comparisons. All eight new upload captures were visually reviewed: long filenames wrap, progress/recovery text remains readable and no horizontal viewport overflow was detected. This is agent review, not human acceptance or assistive-technology certification.

Cobertura inclui idiomas/temas/390–1440px, teclado, drops, ausência de envio automático, nome/extensão/MIME/hash originais, bloqueio de duplicatas, processamento sem falso sucesso, erros reais 422/409/CSRF400, falhas de transporte, retry explícito e fallback sem JS. Eventos-limite e timeout reduzido de 40ms são simulações; política de 120000ms é conferida, sem aguardar dois minutos. Axe sem violações nos oito viewports. Regressão completa: 107 testes aprovados, zero falhas/skips, incluindo 80 comparações visuais. O agente revisou as oito novas capturas: nomes longos quebram linha e textos permanecem legíveis, sem overflow horizontal detectado. Não substitui aceite humano/tecnologia assistiva.

The 80 baseline PNGs were not updated in this slice. SHA-256 over ascending filenames concatenated with their bytes remained `8b6aabf658292b9deed76a9254498ce31bf046785028b84ed5b58558e78550a3`. Browser reports/captures are local ignored artifacts under work/, not a published release. The workflow now uses the same helper in fixed loopback host-network mode; no GitHub-hosted execution is claimed.

As 80 referências PNG não foram atualizadas nesta fatia; hash agregado acima permaneceu igual. Relatórios/capturas locais em work/ são ignorados, não release publicado. Workflow usa helper em modo loopback de rede do host, sem afirmar execução no GitHub.

## Reproduction / Reprodução

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'
docker compose up -d --build gui
docker run --rm --ipc=host --memory=768m -v "${PWD}:/workspace" -w /workspace -e STACKEMU_BROWSER_EMULATOR=http://host.docker.internal:4566 mcr.microsoft.com/playwright:v1.64.1-noble node scripts/run-browser-local.mjs upload.spec.mjs upload-transport.spec.mjs
```

For the complete regression omit both spec arguments; no snapshot-update flag belongs in normal verification. Preserve failures/skips and separate controlled response holds, simulated progress and shortened timeout from real transport/hash/validation/security checks. / Para suíte completa omitir specs; sem atualizar referências na verificação normal. Distinguir simulações e evidência real, sem ocultar falhas/skips.

## Remaining acceptance / Aceite restante

No new vulnerability scan, load/capacity claim, all-emulator/browser/OS certification, complete WCAG/security certification, human assistive-technology approval, publication or release approval is claimed. Client timeout/navigation does not prove server cancellation or rollback. Complete expanded operational scope and legal/name validation remain pending.

Sem novo scan/carga/certificação ampla/aceite humano/publicação/release. Timeout/navegação não prova cancelar servidor/rollback. Demais capacidades e validação jurídica/do nome continuam pendentes.
