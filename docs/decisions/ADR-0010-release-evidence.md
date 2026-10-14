# ADR-0010 — container and release evidence

Status: Accepted / Aceita

## Decision / Decisão

Keep Java 25 JVM delivery, UID 10001 and local-only Compose. Pin build/runtime base-image digests, include MIT/notices and add read-only root, dropped capabilities, no-new-privileges, 512 MiB memory, 256 PIDs and a 64 MiB writable `/tmp` tmpfs. These are reference-development limits to verify, not throughput promises. MiniStack's persistent volumes remain unchanged.

Generate application CycloneDX 1.6 JSON/XML during Maven package with plugin 2.9.3, excluding test dependencies. Scan the final image archive with Trivy 0.76.0, mounting only generated scan/cache directories, never the Docker socket or credentials. CI stores SBOM, scan summary/results and browser JSON for 14 days. Scan execution failures fail verification; findings are reported without suppression. `scan-image.ps1 -Enforce` blocks on every HIGH/CRITICAL finding, including unfixed findings. A green report-only CI is not a release approval.

Manter JVM/UID/Compose local, fixar digests e incluir licença/notices. Root somente leitura, capabilities removidas, sem escalada, memória 512 MiB, 256 PIDs e tmpfs de 64 MiB. Limites não prometem capacidade. Preservar volumes MiniStack. SBOM CycloneDX de dependências Maven sem testes; scan da imagem exportada, sem socket/credenciais. CI retém relatórios por 14 dias. Falha do scanner interrompe verificação; achados não são ocultados. Modo `-Enforce` bloqueia HIGH/CRITICAL, inclusive sem correção. CI verde não aprova release.

## Consequences / Consequências

No tag, GitHub Release or GHCR publication is created until legal-name validation, human accessibility and clean-user quick-start review are complete and vulnerability findings are resolved/reviewed. ARM64, provenance/signing and pull/run of a published GHCR image remain unverified; do not advertise them. SBOM describes Maven dependencies, not every vendored asset or OS package; image scan and third-party notices complement it. Database age/coverage affects scan results. Digest updates require fresh verification and scanning.

Sem tag/release/GHCR até concluir nome legal, revisão humana de acessibilidade, início rápido por novo usuário e achados de vulnerabilidade. ARM64, assinatura/proveniência e execução de imagem publicada seguem não verificados. SBOM Maven não cobre todo vendor/OS; scan/notices complementam. Atualizar digests exige nova validação e scan.
