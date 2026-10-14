# Security design / Projeto de segurança

## Trust boundary / Limite de confiança

The GUI has network access to the configured endpoint and can alter emulator data. Anyone who can access it can therefore affect development data. The MVP is for a trusted local environment, not multi-user or internet exposure.

## Required controls / Controles obrigatórios

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

## SSRF risk / Risco SSRF

Phase 4 adds a REST response CSP without inline/eval permissions, frame denial, no-referrer and nosniff. htmx uses local assets, with eval/injected styles disabled. This policy does not cover every static/management endpoint and does not add authentication or signed CSRF. See [ADR-0009](decisions/ADR-0009-browser-quality-security.md) and [quality guide](development/phase-4.md).

A Fase 4 adiciona CSP REST sem inline/eval, bloqueio de frames, no-referrer e nosniff, com htmx local sem eval/estilos injetados. Não cobre todos os endpoints estáticos/de gerenciamento nem adiciona autenticação ou assinatura CSRF.

An unrestricted endpoint can reach internal services or cloud metadata if StackEmu is exposed/misconfigured. In MVP, configuration comes from environment and cannot be changed per request. A future dynamic endpoint must have an allow-destination policy, reject unexpected schemes, and not rely on textual URL validation alone.

## Credentials and emulator semantics / Credenciais

Example `test` credentials are development placeholders. Any lack of SigV4 validation by an emulator must never be described as secure authentication. IAM/isolation claims require version-specific evidence.
