# ADR-0012 — proposed browser follow-ups / Propostas de evolução

- Date / Data: 2026-10-15.
- Status: PROPOSED, NOT APPROVED, NOT IMPLEMENTED / PROPOSTA, NÃO APROVADA, NÃO IMPLEMENTADA.

## Boundary / Limite

E6 records P2 proposals only. Explicit user approval and new evidence are required before implementation. Existing HTTP headers, URL preferences, runtime dependencies and contribution workflow remain in force. / E6 apenas registra P2. Exigir aprovação explícita e novas evidências; preservar controles atuais.

## Candidates and acceptance / Candidatas e aceitação

1. Evaluate `Permissions-Policy` restricting camera/microphone/geolocation, `Cross-Origin-Opener-Policy: same-origin` and `Cross-Origin-Resource-Policy: same-origin`. These are candidate settings, not a deployment recommendation for StackEmu yet. Before enabling: verify current browser support and effects on native attachment streaming, new-tab navigation, same-origin assets, failure responses and all browser/security tests. Impact remains unverified. [OWASP Secure Headers](https://owasp.github.io/www-project-secure-headers/response-headers/) provides background, not project-specific approval.
2. Consider a non-sensitive `SameSite=Lax` locale/theme cookie fallback with explicit query parameters taking precedence. Decide lifetime/path, localhost HTTP versus HTTPS flags, malformed values, cache behavior and CSRF separation before implementation. No preference cookie is added by this ADR.
3. Establish measurement-based performance review: compare CSS/JS/font bytes and repeatable local navigation samples before/after changes. Document environment, variability and justification; do not invent numeric budgets or infer capacity from warm local timings. E6 does not enforce a performance budget.
4. Propose Conventional Commits and one PR per stage, with reviewed captures, accessibility checklist and commands actually run. Do not rewrite existing history or create/publish a PR without authorization. Existing contributor guidance remains authoritative until approved.

## Português

1. Avaliar os três cabeçalhos acima, sem ativá-los. Validar suporte atual, streaming de anexos, nova aba, assets locais e falhas antes de aprovar. Impacto no StackEmu permanece não verificado; referência OWASP não substitui evidência do projeto.
2. Considerar cookie não sensível `SameSite=Lax` como fallback de idioma/tema, com query prioritária. Decidir duração/path, HTTP/HTTPS, valores inválidos, cache e separação de CSRF. Nenhum cookie novo nesta ADR.
3. Comparar bytes e amostras locais reproduzíveis, registrando ambiente/variabilidade e justificativas. Sem metas inventadas, promessa de capacidade ou orçamento imposto na E6.
4. Propor Conventional Commits e uma PR por etapa com capturas/checklist/evidências. Não reescrever histórico nem publicar sem autorização. Regras atuais continuam até aprovação.

## Other deferred choices / Outras escolhas adiadas

System font stacks stay. Web fonts, authentication, new services and changes to upload/download policies are not approved here. Legal validation of the StackEmu name remains pending. / Fontes de sistema permanecem. Fontes web, autenticação, novos serviços e mudanças de upload/download não são aprovados. Validação jurídica do nome continua pendente.
