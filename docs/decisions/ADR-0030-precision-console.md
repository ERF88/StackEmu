# ADR-0030 — Precision console / Console de precisão

- Date / Data: 2026-10-23.
- Status / Estado: implementation direction; visual acceptance pending / direção implementada; aceite visual pendente.

## Decision / Decisão

Use a restrained industrial/editorial console: Space Grotesk display typography, IBM Plex Sans body text, compact rectilinear controls and one existing measured blue accent. Bundle fonts locally with their SIL OFL notices; no runtime font CDN, Apple assets, imported reset or framework. This explicitly replaces the system-font-only and capsule-only visual recipes in ADR-0022/0023 and the design guide, not their accessibility/security requirements.

Usar console industrial/editorial contido: Space Grotesk nos títulos, IBM Plex Sans no corpo, controles retangulares compactos e o acento azul já medido. Fontes locais com avisos SIL OFL; sem CDN em runtime, assets Apple, reset ou framework novo. Substitui receitas de fontes de sistema/cápsulas das ADRs anteriores, não acessibilidade ou segurança.

Remove Operations from primary navigation. Durable processing and canonical operation records remain for continuity and honest partial/uncertain results; removing a menu is not deleting the execution infrastructure. Metadata and versions use named local icon controls with keyboard-visible labels, retaining their canonical URLs. No new emulator reads are added to Home.

Remover Operações da navegação principal. Processamento persistente e registros canônicos permanecem para continuidade e resultados parciais/incertos reais; retirar menu não elimina a infraestrutura. Metadados e versões usam ícones locais nomeados, com identificação por teclado e URLs preservadas. Início não ganha consultas ao emulador.

## Constraints / Restrições

Keep 44px minimum targets, visible focus, reduced motion, both languages/themes, exact opaque keys, CSRF and restrictive CSP. CSP permits only same-origin fonts. Local verification is not HIG certification, user research, production release or legal approval of the StackEmu name. New-image validation remains separate.

Preservar alvos mínimos de 44px, foco visível, movimento reduzido, idiomas/temas, chaves exatas, CSRF e CSP restritiva. Fontes apenas da própria origem. Verificação local não é certificação HIG, pesquisa, release ou aprovação jurídica do nome; imagem nova fica separada.

## Local verification / Verificação local

- Java 25/Maven `verify`: BUILD SUCCESS; 184 discovered, 126 executed, 58 integration-gated skips, zero failures/errors. Log: `work/precision-console-maven.log`. A separate `STACKEMU_INTEGRATION=true` run of BucketsResourceTest, S3ReadIntegrationTest and S3OperationsIntegrationTest passed 33/33 with local MiniStack; log: `work/precision-console-integration.log`. / Build aprovado; 58 testes condicionais não executados na primeira rodada e 33 testes direcionados aprovados na rodada local com integração habilitada, sem alegar suíte integral de integração.
- Chrome local: 24/24 browser cases passed, covering shared details, native/JS controls, preferences and typography at 390/1366/1440px. Font loading, same-origin requests, CSP, page overflow and axe checks passed in the new console cases. Log: `work/precision-console-browser.log`; screenshots: `work/precision-console-browser/`. / 24 cenários aprovados, com fontes locais, idiomas/temas, foco, fallback e larguras verificadas; axe não substitui revisão humana.
- Compared representative dark desktop object drawer and S3 list plus light mobile list screenshots. Versioned baselines were not regenerated. / Comparadas capturas representativas de detalhes/listagem no desktop escuro e listagem mobile clara; baselines versionadas preservadas.

The GUI runs on the host JVM; no application image, container suite, new vulnerability scan, publication or release was performed. Maintainer acceptance remains pending. / GUI no JVM local, sem imagem nova, suíte em contêiner, scan novo ou publicação; aceite do mantenedor pendente.
