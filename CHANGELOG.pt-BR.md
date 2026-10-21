# Changelog

[English](CHANGELOG.md)

Todas as mudanças relevantes serão registradas neste arquivo. O projeto pretende seguir os princípios de Keep a Changelog e versionamento semântico após a primeira release.

## [Não lançado]

- Substituir expansores de Ações/Preferências por ações diretas e Preferências nativas não modais; padronizar botões com ícone/rótulo e retornos contextuais antes dos títulos. Preservar arquivo original, fallback e contratos de segurança. [Decisão](docs/decisions/ADR-0024-direct-actions-navigation.md) · [Checks locais](docs/evidence/direct-controls-results.md).

- Iteração coesa de fluidez HIG pela ADR-0023: superfícies neutras sólidas, separadores discretos, navegação mobile compacta, breadcrumbs contínuos, metadados agrupados e feedback 100/200ms com redução de movimento. Substitui decoração V1 congelada; [checks novos e limites](docs/evidence/hig-fluidity-results.md), sem dispensar segurança ou alegar certificação.

- Refinamento HIG do explorer: ajuda recolhível nativa, vazio acionável sem tabela sem linhas, ações existentes do drawer antes do conteúdo auxiliar e feedback sólido de carga/erro. Segurança, fallback e status HTTP preservados; [escopo e checks](docs/design/apple-hig-guidelines.md#explorer-and-state-slice--fatia-de-explorer-e-estados).

- Primeira fatia HIG: botões de ação cápsula e variante destrutiva explícita na confirmação nativa de exclusão. Alvos, foco, rotas e gates preservados; [auditoria delimitada e verificação](docs/design/apple-hig-guidelines.md#first-controls-slice--primeira-fatia-de-controles), sem certificação HIG ou aceite do mantenedor.

- Adotar a skill apple-hig-designer solicitada como padrão visual/UX, com adaptações para Qute/CSS, acessibilidade e segurança; instalação local sem dependência nova na GUI. [ADR-0022](docs/decisions/ADR-0022-apple-hig-design-baseline.md). É adoção de diretrizes, não migração visual concluída ou certificação HIG.

- Exclusão individual exata com revisão nativa, confirmação limitada de uso único/cinco minutos, CSRF, recusa de versionamento e ETag condicional. Cliente SDK isolado e DELETE vazio de comprimento fixo impedem retries nos fixtures executados de 503/resposta perdida; incerteza permanece explícita. [Contrato e limites](docs/decisions/ADR-0021-single-object-deletion.md) · [Evidência local](docs/evidence/single-object-deletion-results.md).

- Preferências nativas recolhidas, densidade nas listagens e ações por recurso; remoção de legendas/avisos repetidos. Spike local de condições de exclusão/versionamento, sem ativar operações destrutivas na GUI.

- Início orientado a serviços sem consultas ao emulador; buckets em `/s3`, diagnóstico/status e atualização contextuais; galeria fora do menu de produto, ainda em `/components`. ADR-0020 preserva contratos de operações/segurança.

### Adicionado

- Aceite visual V4 pelo mantenedor e validação local consolidada V6: imagem Linux/amd64 nova, 107 testes Java limpos no host, 157 casos completos Linux, 80 PNGs revisados/reconciliados deliberadamente, checks de contêiner/S3/CSRF, streaming 128 MiB/heap 64 MiB e zero achados reportados no scan de todas as severidades com inventários OS/Java. Fixtures de endpoint de densidade e seleção do shell corrigidas sem relaxar política/tolerâncias. [Execuções, comparação inicial com falhas e gates de release restantes](docs/evidence/visual-v6-results.md).

- V4 com entrada opcional de cards/transições CSS nativas entre documentos da mesma origem em 180ms, redução de movimento e feedback de clipboard confirmado com texto/borda persistentes em 120ms; navegação, skeletons estáticos, CSP e arquivos originais preservados. [Verificação nova e aceite pendente](docs/evidence/visual-v4-results.md).

- Correção do transporte de preview: leitura/cleanup limitado sob um único owner no adaptador, socket 1s, regressões repetidas de corpo parado/lento e XML de erro incompleto, recusa de metadados e ciclo de vida do cliente comum; políticas de bytes/prazo/segurança preservadas. [Evidência nova e gates finais pendentes](docs/evidence/preview-transport-correction.md).

- V3 com densidade confortável/compacta nativa por valores permitidos de URL/form, preservada na navegação e nos resultados; tokens de espaçamento, cabeçalhos/ícones mantidos, ações sempre visíveis com destaque no foco e regras de chave original/alvos 44px preservadas. [Verificação e aceite pendente](docs/evidence/visual-v3-results.md).

- V2 com hierarquia do dashboard, diagnóstico nativo separado de Atualizar e skeleton estático do contador ligado ao estado real de carga. Fallback/avisos preservados, sem contagens inventadas ou API nova de saúde. [Execução e aceite pendente](docs/evidence/visual-v2-results.md).

- V0 aceita com fontes do sistema/CSP atual; V1 com profundidade no canvas, brilho decorativo restrito, fallback de preferências acessíveis e moldura única de card/tabela. Checks locais e revisão Linux/aceite humano pendentes no [registro V1](docs/evidence/visual-v1-results.md). Densidade/movimento, D2–D6 e gates de release continuam pendentes.

- Consolidação documental: READMEs pareados datados com capturas ilustrativas e início via JVM local, índice em quatro grupos, status/gates atuais, histórico e inventário de maturidade. Evidências movidas sem alterar conteúdo para `docs/evidence/`; checks locais de links/índice adicionados. Sem mudar runtime, política de idioma, decisões visuais ou aprovação de release.

- UX-5B: preview S3 nativo bilíngue de PNG/JPEG estáticos limitados, validações/recusas e saída PNG nova em worker isolado; anexo original intacto, sem formatos ativos ou relaxar CSP. [Validação local e gates finais de imagem](docs/evidence/ux-5b-results.md).

- UX-5B: base de worker raster JDK isolado, entrada/saída limitadas, política de heap/espera e testes locais de ciclo de vida; sem ativar rota/UI de preview de imagens. [Contrato](docs/decisions/ADR-0019-raster-preview-isolation.md), [evidência](docs/evidence/ux-5b-worker-results.md).

- UX-5A: preview separado de texto UTF-8/JSON-como-texto limitado, bytes estritos, página nativa escapada e fallback de anexo; sem conteúdo ativo/descompressão/imagens. [Contrato](docs/decisions/ADR-0018-bounded-text-preview.md), [verificação](docs/evidence/ux-5a-results.md).

- UX-4: avisos dispensáveis no fluxo, sem expiração, para resultados confirmados de bucket/pasta nativos e upload melhorado; fallback persistente sem JS e foco/live sem duplicidade. Contrato de diálogo destrutivo documentado, sem ativar exclusão/modal. [Verificação](docs/evidence/ux-4-results.md).

- UX-3: drop/seletor individual e progresso nativo rotulado, separando transferência da confirmação do servidor; workspace verificado, proteção contra duplicidade e recuperação explícita sem retry automático do navegador. Preservados multipart nativo, CSRF, arquivo original e não sobrescrita. [Validação/aceite restante](docs/evidence/ux-3-results.md).

- UX-2B: drawer modal nativo de metadados, fragmento Qute escapado/verificado, proteção contra resposta obsoleta, timeout/retry manual e clipboard de bucket/chave exatos com fallback selecionável. Sem preview, ARN, assinatura ou mutação implícitos; validação registrada separadamente.
- Escopo operacional da primeira entrega ampliado pela ADR-0014, sem configurações avançadas. UX-2A implementa tamanhos IEC com bytes exatos, indícios de tipo pelo nome e download nativo; aprovação de escopo não comprova implementação de todas as capacidades.

- UX-1: Tailwind fixado somente no build Maven/Docker, licença MIT empacotada, temas Slate/azul e utilitários delimitados no shell; sidebar desktop baixa acessível por teclado e preenchimento mobile restaurado. [Registro de validação/aceite](docs/evidence/ux-1-results.md); sem nova capacidade S3 ou publicação.

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
