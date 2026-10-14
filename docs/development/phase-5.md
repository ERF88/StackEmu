# Phase 5 — container and release preparation / Container e preparação de release

The automated delivery baseline prepares evidence; it does not publish a version. / O baseline prepara evidências; não publica versão.

## Reproduce / Reproduzir

Use Java 25, local MiniStack and the established integration flag. Maven package generates `target/bom.json` and `target/bom.xml`. Then:

```powershell
docker compose up --build -d --wait
./scripts/verify-container.ps1
./scripts/smoke.ps1
./scripts/verify-writes.ps1
./scripts/scan-image.ps1
./scripts/scan-image.ps1 -Enforce
./scripts/scan-image.ps1 -RequireClean
```

`-RequireClean` blocks every reported finding and requires detected OS/Java package inventories; CI uses this gate. `-Enforce` retains the older HIGH/CRITICAL-only policy. Neither mode hides severities/unfixed findings or uses an ignore list. Scanner errors fail rather than pretending the scan is clean. JSON reports/archive live in ignored `work/release`, database cache in `work/trivy-cache`; image export can consume substantial disk space. These are generated files, not release artifacts published to GHCR. No secrets/source/home/socket are mounted into the scanner.

`-RequireClean` bloqueia qualquer achado e exige inventários OS/Java; é o gate do CI. `-Enforce` mantém a política anterior HIGH/CRITICAL. Nenhum modo oculta achados sem correção. Erro não é resultado limpo. Artefatos locais em `work/release`, cache em `work/trivy-cache`; exportar imagem consome disco. Sem mount de socket, credenciais, home ou fontes no scanner.

## Reference container / Container de referência

UID 10001; root filesystem read-only; `/tmp` tmpfs 64 MiB with noexec/nosuid; no capabilities/escalation; memory 512 MiB and PIDs 256; ports loopback. Uploads remain limited to 10 MiB per file/11M HTTP by default. The aggregate tmpfs quota can reject concurrent uploads; there is no concurrency capacity guarantee or graceful quota-specific error contract. Restart removes temporary files, not emulator volumes. Do not use `docker compose down -v` to stop normally.

UID 10001, root somente leitura, tmpfs de 64 MiB, sem capabilities/escalada, memória/PIDs limitados e loopback. A quota total pode recusar uploads concorrentes; sem garantia de capacidade ou erro específico de quota. Reinício remove temporários, preservando volumes do emulador. Parar normalmente sem `down -v`.

## CI and release gate / CI e gate

The existing CI verifies Java/integration, style, container policy, real GUI writes, Chromium/axe and constrained streaming; then scans and stores selected reports for 14 days. Download reports from the run's artifacts. Image archives/cache and browser traces are not uploaded. Inspect scan results even when verification is green. See [release process](release.md), [ADR-0010](../decisions/ADR-0010-release-evidence.md) and [evidence](../spikes/phase-5-results.md).

CI verifica comportamento e política do container, escaneia e retém relatórios selecionados por 14 dias. Imagem exportada, cache e traces não são enviados. Ler achados mesmo com CI verde. Gate de publicação continua separado.

References: [CycloneDX Maven](https://cyclonedx.github.io/cyclonedx-maven-plugin/), [Trivy image scanning](https://trivy.dev/docs/dev/references/configuration/cli/trivy_image/), [Compose services](https://docs.docker.com/reference/compose-file/services/).
