# Security design / Projeto de segurança

## Trust boundary / Limite de confiança

UX-5A bounded text preview follows [ADR-0018](decisions/ADR-0018-bounded-text-preview.md): managed SDK body deadline, 64 KiB complete-input cap, strict extension/MIME/UTF-8/control checks, identity encoding only, escaped Qute text and no-store. Its [historical evidence](spikes/ux-5a-results.md) is not new raster verification. / Texto UX-5A mantém bytes/tempo/tipo/UTF-8/controles, escape e no-store; evidência histórica não comprova raster novo.

UX-5B adds separate bounded PNG/JPEG reads and isolated re-encoding under [ADR-0019](decisions/ADR-0019-raster-preview-isolation.md), not raw inline attachment serving. Strict metadata/format checks, 1 MiB input/output, 4096 axes/4,000,000 pixels, child heap/wait limits and cleanup reduce exposure but do not create a process security sandbox, native-memory bound, authentication or malware scanner. PNG animation/CRC/trailing-data guards and JPEG framing/warning checks are not exhaustive conformance validation. Successful binary responses are fixed PNG with no-store and unchanged CSP/headers; errors retain localized plain text and real statuses. Native shell 200 does not mean the separate image request succeeded. [Current local evidence](spikes/ux-5b-results.md); new-image consolidated validation remains deferred. / Imagens seguem contrato/evidência próprios: limites e isolamento sem prometer sandbox/memória nativa/autenticação/antivírus ou conformidade exaustiva. PNG reencodado, nunca anexo bruto inline; headers/CSP/sem cache mantidos e erros textuais/status reais. Shell 200 não confirma imagem; validação com imagem nova ao final.

The GUI has network access to the configured endpoint and can alter emulator data. Anyone who can access it can therefore affect development data. The MVP is for a trusted local environment, not multi-user or internet exposure.

## Required controls / Controles obrigatórios

Future explorer capabilities follow the [visual/UX plan](product/visual-ux-evolution.md), not a blanket authorization to render/delete objects. Preview requires bounded validated content and explicit CSP/isolation review; attachment download remains unchanged. Deletion requires its own accepted ADR, exact-target confirmation, CSRF and verified version semantics. Toasts/modals provide feedback, not authorization. Browser upload progress measures transport to the GUI, not committed emulator storage. Clipboard/presigned URLs must not leak credentials or bearer-like access through logs. These are future acceptance requirements, not newly verified controls.

Capacidades futuras seguem o plano UX, sem autorização genérica de renderização/exclusão. Preview exige conteúdo validado/limitado e revisão CSP/isolamento; download continua anexo. Exclusão exige ADR própria, alvo exato, CSRF e versões definidas. Toast/modal não autoriza operação. Progresso do navegador não confirma gravação no emulador. Clipboard/links assinados não podem vazar credenciais/acesso em logs. São requisitos futuros, não controles novos comprovados.

- Reference Compose mapping: `127.0.0.1:8080:8080`.
- No Docker socket mount, arbitrary shell, or root runtime user.
- Appropriate HTTP methods and CSRF protection for every mutation.
- Default Qute escaping; never render emulator-controlled data unescaped.
- Validate/encode route/query parameters and filenames.
- Never place credentials in logs, HTML, error messages, or CLI examples.
- Generate/clean temporary upload files safely.
- Stream downloads as attachments with `X-Content-Type-Options: nosniff`; do not render uploaded HTML.
- Apply body-size, timeout, and resource limits.
- Publish SBOM and vulnerability scan results as release artifacts.

The list mixes implemented controls and remaining release criteria. Phase 3 introduces CSRF forms, application/HTTP limits and temporary-file cleanup; consult [executed evidence](spikes/phase-3-results.md). SBOM/scanning, signed-token policy and broader threat review are not certified by these tests.

A lista reúne controles atuais e critérios futuros. A Fase 3 adiciona formulários CSRF, limites e limpeza; evidências devem ser consultadas separadamente. Os testes não certificam segurança completa.

## Browser policy / Política de navegador

Phase 5 adds verified container policy, Maven SBOM and image scanning/report retention; [release approval remains separate](development/release.md). Report-only CI does not approve vulnerabilities. / Fase 5 adiciona política do container, SBOM e scan; CI de relatório não aprova vulnerabilidades nem release.

Phase 4 adds a REST response CSP without inline/eval permissions, frame denial, no-referrer and nosniff. htmx uses local assets, with eval/injected styles disabled. This policy does not cover every static/management endpoint and does not add authentication or signed CSRF. See [ADR-0009](decisions/ADR-0009-browser-quality-security.md) and [quality guide](development/phase-4.md).

A Fase 4 adiciona CSP REST sem inline/eval, bloqueio de frames, no-referrer e nosniff, com htmx local sem eval/estilos injetados. Não cobre todos os endpoints estáticos/de gerenciamento nem adiciona autenticação ou assinatura CSRF.

## Object drawer / Drawer de objetos

UX-2B uses only the checked escaped same-origin metadata fragment with no-store/Vary and an active-request/status/marker gate before swapping; htmx script execution is disabled. Modal loading/error/clipboard results never interpret object content as HTML. Exact clipboard values originate from encoded canonical query components, with an explicitly encoded manual fallback for control characters. Native detail/download routes and CSP/CSRF remain unchanged. [ADR-0015](decisions/ADR-0015-object-drawer-clipboard.md) and [fresh evidence](spikes/ux-2b-results.md) do not certify future preview/signing/deletion or other browsers/OS. / UX-2B restringe fragmento local escapado, status/marcador/XHR ativo e scripts desativados; valores exatos vêm da URL codificada, com fallback manual identificado para controles. Sem interpretar corpo do objeto como HTML ou mudar CSP/CSRF; não certifica capacidades futuras/navegadores/SO.

## Upload enhancement / Melhoria de upload

UX-3 keeps native multipart/CSRF, original-file policy, limits and no-overwrite. Only resource-marked checked upload workspaces may be swapped; unmarked CSRF/body-limit/transport responses never become arbitrary HTML. Lock controls after FormData capture, distinguish browser transfer from storage completion and clear confirmation after unknown outcomes. Client timeout/navigation does not prove server rollback; no browser auto-retry is added. [Contract](development/s3-upload.md), [fresh evidence](spikes/ux-3-results.md). / Preservar controles existentes; swap restrito a workspace verificado, sem HTML arbitrário/relaxar CSP. FormData precede bloqueio, transferência não é gravação, falha incerta exige reconfirmar e verificar listagem; sem promessa de rollback ou retry do navegador.

## SSRF risk / Risco SSRF

An unrestricted endpoint can reach internal services or cloud metadata if StackEmu is exposed/misconfigured. In MVP, configuration comes from environment and cannot be changed per request. A future dynamic endpoint must have an allow-destination policy, reject unexpected schemes, and not rely on textual URL validation alone.

## Credentials and emulator semantics / Credenciais

Example `test` credentials are development placeholders. Any lack of SigV4 validation by an emulator must never be described as secure authentication. IAM/isolation claims require version-specific evidence.
