# Phase 4 security review / Revisão de segurança

Scope: local S3 MVP, source review plus automated tests; not a penetration test or independent audit. / Escopo: MVP S3 local, revisão de código e testes; não é pentest ou auditoria independente.

| Boundary / Limite                           | Current control / Controle                                                                                                                  | Residual risk / Risco residual                                                                                                     |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Access / Acesso                             | Loopback Compose ports, local trust warning / Portas loopback, aviso local                                                                  | No authentication; anyone with access can mutate data / Sem autenticação                                                           |
| Configuration / Configuração                | Endpoint/credentials from startup environment; no per-request editing; redacted view / Ambiente, sem edição por requisição                  | Administrator can misconfigure destination; emulator lacks production IAM assurance / Destino pode ser mal configurado             |
| Mutations / Escritas                        | POST forms, required CSRF and confirmation, separate write port, conditional object creation / Formulários protegidos e criação condicional | Unsigned double-submit baseline; concurrent bucket creation not stress-tested / CSRF sem assinatura e concorrência não certificada |
| Rendering / Renderização                    | Qute escaping, self-only REST CSP, htmx eval disabled, no shell execution / Escape, CSP e CLI somente texto                                 | CSP is defense-in-depth; static/management paths not covered by REST filter / Não é política universal                             |
| Upload / Envio                              | File/body limits, framework-generated temp path, explicit/end-of-request cleanup / Limites e temporário do framework                        | No aggregate disk quota, crash cleanup, malware/content validation / Sem quota total, recuperação de crash ou antivírus            |
| Download / Download                         | Bounded streaming, sanitized attachment names, nosniff, safe MIME metadata / Streaming/anexo/nosniff                                        | Browser/OS name rules differ; stored MIME does not validate bytes / MIME não comprova formato                                      |
| Network/resources / Rede/recursos           | S3 call timeouts and page limits; no Docker socket mount; UID 10001 / Timeouts, paginação, usuário não root                                 | No load/concurrency capacity claim; no public TLS/auth deployment / Sem promessa de capacidade ou exposição pública                |
| Dependencies/release / Dependências/release | Pinned development tools, platform BOM, CI checks / Ferramentas fixadas e CI                                                                | SBOM/scan and legal-name validation remain release gates / SBOM/scan e nome legal pendentes                                        |

Automated checks exercise missing CSRF, conflict preservation, size/temporary cleanup, escaping, hidden secrets in error pages, bounded streams and browser headers. Review results describe the current controls, not proof that all exploits or secret leakage paths are absent. Existing historical evidence remains unchanged.

Os testes exercitam recusa CSRF, conflitos, limites/limpeza, escape, ocultação de segredo em páginas de erro, streams limitados e cabeçalhos. A revisão não prova ausência de todos os exploits ou caminhos de vazamento. Evidências históricas não foram reescritas.

## Human accessibility checklist / Checklist humano

- [ ] Review full create/upload/download flow using keyboard only, including focus visibility and error recovery. / Revisar fluxo completo apenas com teclado, foco e recuperação de erros.
- [ ] Review English and pt-BR labels and messages with a screen reader. / Revisar idiomas com leitor de tela.
- [ ] Review 200%/400% browser zoom and long names on mobile without lost controls. / Revisar zoom e nomes longos sem perder controles.
- [ ] Review live refresh announcements and native file-picker interaction. / Revisar anúncios do refresh e seletor nativo.
- [ ] Review Firefox/WebKit and record browser/OS versions. / Revisar outros engines e registrar versões.

These items intentionally remain unchecked: scripted keyboard/axe tests are not human evidence. / Itens pendentes: testes automáticos não substituem evidência humana.
