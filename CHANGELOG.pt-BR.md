# Changelog

[English](CHANGELOG.md)

Todas as mudanças relevantes serão registradas neste arquivo. O projeto pretende seguir os princípios de Keep a Changelog e versionamento semântico após a primeira release.

## [Não lançado]

### Adicionado

- UX-5B: preview S3 nativo bilíngue de PNG/JPEG estáticos limitados, validações/recusas e saída PNG nova em worker isolado; anexo original intacto, sem formatos ativos ou relaxar CSP. [Validação local e gates finais de imagem](docs/spikes/ux-5b-results.md).

- UX-5B: base de worker raster JDK isolado, entrada/saída limitadas, política de heap/espera e testes locais de ciclo de vida; sem ativar rota/UI de preview de imagens. [Contrato](docs/decisions/ADR-0019-raster-preview-isolation.md), [evidência](docs/spikes/ux-5b-worker-results.md).

- UX-5A: preview separado de texto UTF-8/JSON-como-texto limitado, bytes estritos, página nativa escapada e fallback de anexo; sem conteúdo ativo/descompressão/imagens. [Contrato](docs/decisions/ADR-0018-bounded-text-preview.md), [verificação](docs/spikes/ux-5a-results.md).

- UX-4: avisos dispensáveis no fluxo, sem expiração, para resultados confirmados de bucket/pasta nativos e upload melhorado; fallback persistente sem JS e foco/live sem duplicidade. Contrato de diálogo destrutivo documentado, sem ativar exclusão/modal. [Verificação](docs/spikes/ux-4-results.md).

- UX-3: drop/seletor individual e progresso nativo rotulado, separando transferência da confirmação do servidor; workspace verificado, proteção contra duplicidade e recuperação explícita sem retry automático do navegador. Preservados multipart nativo, CSRF, arquivo original e não sobrescrita. [Validação/aceite restante](docs/spikes/ux-3-results.md).

- UX-2B: drawer modal nativo de metadados, fragmento Qute escapado/verificado, proteção contra resposta obsoleta, timeout/retry manual e clipboard de bucket/chave exatos com fallback selecionável. Sem preview, ARN, assinatura ou mutação implícitos; validação registrada separadamente.
- Escopo operacional da primeira entrega ampliado pela ADR-0014, sem configurações avançadas. UX-2A implementa tamanhos IEC com bytes exatos, indícios de tipo pelo nome e download nativo; aprovação de escopo não comprova implementação de todas as capacidades.

- UX-1: Tailwind fixado somente no build Maven/Docker, licença MIT empacotada, temas Slate/azul e utilitários delimitados no shell; sidebar desktop baixa acessível por teclado e preenchimento mobile restaurado. [Registro de validação/aceite](docs/spikes/ux-1-results.md); sem nova capacidade S3 ou publicação.

- UX-0: comparação isolada Tailwind/Pico com lockfile, fixtures Qute escapadas, build determinístico/relatórios e diagnóstico Chromium; Tailwind/CLI 4.3.3 sem Preflight selecionado para UX-1, sem alterar assets de produção.

- Plano documental UX-0–7 e ADR-0013 proposta: avaliação CSS no build, explorer/drawer, drop/progresso, notificações e gates separados para preview/exclusão; diretrizes de contribuição/segurança atualizadas, sem mudanças de runtime.

- E7: registro de validação final do redesenho, comandos reproduzíveis, referências de capturas e limites de revisão humana/release; sem ativação P2 ou publicação.

- E6: galeria/contratos bilíngues, fixtures Qute determinísticas, axe em indisponibilidade/erro e snapshots no container; P2 registrado apenas como proposta não aprovada na ADR-0012.

- E5: formulários nativos padronizados e feedback local progressivo com status traduzidos, `aria-busy` real, bloqueio apenas do botão de envio e restauração ao voltar; políticas CSRF/upload/download preservadas.

- E4: tabelas semânticas roláveis por teclado, cabeçalhos fixos, breadcrumb/chips de contexto e painéis responsivos de metadados/download; contratos S3 preservados.

- E3: visão geral responsiva com configuração efetiva, contagem da última listagem e próxima ação contextual; atualização htmx atômica e contagem desconhecida em falhas.

- E2: shell com SVGs locais, controles explícitos de tema, títulos localizados por página e favicon SVG próprio.
- URLs com hash SHA-256 dos assets e atribuição/inventário coolicons v4.1 CC BY 4.0; CSP preservada.

- E1: CSS em camadas, tokens OKLCH semânticos com fallback/aliases, tema automático e preferências de acessibilidade.
- Gate de contraste sem dependências, testes de referência e verificações de tema/responsividade no browser; sem mudanças em templates ou CSP.

- Inventário completo pacote/CVE e gate CI para qualquer severidade, com checagem de inventários OS/Java.

- SBOM Maven CycloneDX, scan Trivy isolado da imagem exportada e relatórios retidos no CI.
- Compose reforçado, digests de build/runtime fixados, checks do container e licença/notices empacotados.

- Ferramentas Chromium/axe da Fase 4, navegação bilíngue/temas/móvel e fluxo S3 isolado na GUI.
- Cabeçalhos de segurança REST e erros associados aos campos; ESLint fixado para JavaScript de testes.

- Fase 3: criação de buckets, upload individual, marcadores de pasta, validação por campo e confirmação.
- Formulários CSRF, limites de arquivo/requisição, limpeza e comandos POSIX ilustrativos.
- Port/adapter de escrita separado e testes do fluxo completo/segurança.

- Diretrizes de nomenclatura/responsabilidade, inventário de classes, ADR-0006 e instruções para colaboradores/agentes.
- Testes de papéis, identificadores, injeção e isolamento AWS/web.

- Fase 2: detalhes/tags de buckets, objetos paginados, prefixos/breadcrumbs, metadados e downloads seguros por streaming.
- Testes reais MiniStack, fechamento/cancelamento de streams e prova de download de 128 MiB com heap Java de 64 MiB.
- Guia, evidências e ADR de streaming bilíngues.

- Convenções por tipo de arquivo, EditorConfig, atributos Git LF, checagem de formatação Maven e validadores de desenvolvimento.
- CI para Markdown, CSS, PowerShell, Dockerfile, workflows e Compose; Java/XML são verificados no validate Maven.

- Tela de conexão efetiva da Fase 1, layout/navegação Qute compartilhados, tokens claro/escuro e galeria de componentes.
- Negociação de idioma, catálogos en/pt-BR, verificação de chaves e limites de arquitetura com ArchUnit.

- Tela S3 de leitura da Fase 0 com Java 25, Quarkus 3.40.1, Qute e htmx local.
- Compose MiniStack 1.5.25, imagem JVM não-root, quatro testes e CI Linux/amd64 aprovado.
- Guia reproduzível e evidências reais dos spikes, com validações restantes explícitas.

- Documentação fundamental de planejamento em inglês e português do Brasil.
- Registros iniciais de arquitetura, escopo, segurança, configuração, testes, release e ADRs.

### Alterado

- Runtime Temurin 25/Alpine com PNG/zlib corrigidas fixadas, sem supressão de vulnerabilidades.

- Upload usa prefixo e nome original automaticamente; MIME válido e nome/extensão no download são preservados, com fallbacks seguros.

- Nomes explícitos por papel, recursos de conexão/componentes separados e helpers de download na camada web. Rotas HTTP preservadas.

### Ainda não lançado ou verificado

- Compatibilidade completa, benchmarks, arm64 e revisão ampla de segurança continuam pendentes.
