# Changelog

[English](CHANGELOG.md)

Todas as mudanças relevantes serão registradas neste arquivo. O projeto pretende seguir os princípios de Keep a Changelog e versionamento semântico após a primeira release.

## [Não lançado]

### Adicionado

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

- Upload usa prefixo e nome original automaticamente; MIME válido e nome/extensão no download são preservados, com fallbacks seguros.

- Nomes explícitos por papel, recursos de conexão/componentes separados e helpers de download na camada web. Rotas HTTP preservadas.

### Ainda não lançado ou verificado

- Compatibilidade completa, benchmarks, arm64 e revisão ampla de segurança continuam pendentes.
