# Phase 1 validation / Validação da Fase 1

Date: 2026-10-13 (America/Sao_Paulo). Windows + Docker Desktop/WSL2, amd64. Java 25, Quarkus/platform BOM 3.40.1, MiniStack 1.5.25, ArchUnit 1.5.1.

## Acceptance evidence / Evidências

| Item | Result |
|---|---|
| F1.1 Minimal layers | One Maven module with pure application contracts and separate AWS/web adapters; clean build passed |
| F1.2 Effective client config | SDK client uses the configured endpoint in integration tests; test verifies overridden SDK region and view region; extension manages credentials/client lifecycle |
| F1.3 Shared Qute layout | Dashboard, connection and gallery share navigation/layout; gallery renders with unavailable emulator |
| F1.4 Tokens/themes/focus | Semantic CSS tokens, light/dark query preference, wrapping layout and visible focus/skip link |
| F1.5 Connection/error state | Read operation produces connected/unavailable view; diagnostics and next step rendered without SDK exception details |
| F1.6 i18n | English/pt-BR catalogs, Accept-Language negotiation, explicit override, theme/locale retention across navigation; key parity test |
| F1.7 Architecture | ArchUnit prohibits framework/SDK/adapters in application contracts and AWS SDK/adapters in web layer |

`STACKEMU_INTEGRATION=true mvn -B -ntp clean verify`: **11 tests, 0 failures, 0 errors, 0 skips**. Docker GUI build succeeded. Expanded smoke test verified connection configuration, light-theme attribute, Portuguese gallery and original Phase 0 checks. [GitHub CI run 38491580441](https://github.com/ERF88/StackEmu/actions/runs/38491580441) passed the clean build, tests, Linux/amd64 Compose build and expanded smoke test.

The first incremental build after package relocation found an old generated/class artifact; clean verify removed it. Browser inspection exposed cached Phase 0 CSS; the stylesheet URL now includes a version marker so new styles load.

Manual browser inspection confirmed Portuguese connection display, shared navigation, light/dark switching, the component gallery at 390 × 844, and Tab focus on the skip link. With MiniStack stopped, HTTP checks confirmed connection diagnostics and the gallery still rendered; the emulator was restarted afterwards. This is a targeted inspection, not a full accessibility audit.

Computed contrast for text/muted/accent/success/error tokens against canvas/surface in both themes: minimum 6.84:1. This calculation covers these token pairs, not every possible interactive state or complete WCAG compliance.

## Limits / Limites

Package boundaries are enforced within one module; extracting Maven modules is deferred until useful. SDK errors currently share one unavailable state rather than a full error taxonomy. No persistent connection/locale/theme profiles, write operations, CSRF validation for mutations, full accessibility audit or arm64 validation is claimed. Name validation and user interviews remain pending.

O build limpo passou com 11 testes. A estrutura usa camadas em um módulo Maven, layout/traduções compartilhados e configuração efetiva somente leitura. O estado de conexão faz ListBuckets real; detalhes internos e segredos não vão para o HTML. Preferências acompanham links, sem persistência em cookie.
