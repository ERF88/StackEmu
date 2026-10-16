# StackEmu

> **Fase 5: preparação de entrega.** StackEmu oferece leitura/escrita S3, testes de navegador, container JVM local reforçado e ferramentas SBOM/scan. Release público continua condicionado aos gates; MVP em desenvolvimento.

[English](README.md)

## Status

A primeira entrega funcional atual passa a incluir [gerenciamento operacional completo S3](docs/product/s3-management-scope.md), com preview, exclusão, cópia/movimentação, clipboard e links assinados de download. É escopo confirmado, não declaração de implementação; configurações avançadas de buckets ficam fora.

O nome do produto foi escolhido em 13 de outubro de 2026, mas a validação jurídica e de nome está **pendente**. O repositório do projeto é [ERF88/StackEmu](https://github.com/ERF88/StackEmu). Antes de uma release pública, validar registros de pacotes, Docker Hub, domínio e INPI. StackEmu não possui afiliação com AWS, MiniStack, LocalStack ou Moto.

A Fase 3 adiciona formulários com confirmação/CSRF e upload limitado contra MiniStack 1.5.25. Consulte o [guia da Fase 3](docs/development/phase-3.md) e as [evidências](docs/spikes/phase-3-results.md). Não há UI de exclusão, sobrescrita ou retomada; use apenas em desenvolvimento local confiável.

## Stack implementada

A UX-1 implementa temas Slate/azul e shell responsivo com Tailwind compilado localmente, sem Preflight; Node é exclusivo do build. Consulte [regras do build CSS](docs/development/frontend-build.md) e [status de validação e aceite](docs/spikes/ux-1-results.md). Etapas UX pendentes não são capacidades S3 implementadas.

Os SVGs da interface usam [coolicons v4.1](https://coolicons.cool/), de Kryston Schwarze, sob [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), não sob a licença MIT da aplicação. Ícones selecionados são adaptados em fragmentos Qute locais; atribuição, revisão de origem e alterações estão nos [notices de terceiros](THIRD_PARTY_NOTICES.md).

Java 25, Quarkus 3.40.1, Qute, htmx local, CSS, Maven e AWS SDK for Java 2.x via BOM da plataforma. MiniStack 1.5.25 em amd64 é o baseline testado; outras plataformas/emuladores não são certificados.

## Início rápido — desenvolvimento local

A GUI conecta ao MiniStack em `http://ministack:4566`, publicada somente em `127.0.0.1:8080`. Requer Docker Compose v2. O comando foi executado localmente em Windows/amd64:

```sh
docker compose up --build -d --wait
```

Abra <http://127.0.0.1:8080/?lang=pt-BR>. Para parar, use `docker compose down`; os volumes são preservados. A GUI permite criação, upload, navegação e download.

## Documentação

- [Evidências da Fase 4](docs/spikes/phase-4-results.md) e [checklist humano](docs/security-review.md)
- [Container/SBOM/scan da Fase 5](docs/development/phase-5.md) e [gates de release](docs/development/release.md)

- [Diretrizes de nomenclatura e responsabilidades](docs/development/naming-and-responsibilities.md) e [revisão das classes](docs/architecture/naming-review.md)

- [Guia da Fase 1](docs/development/phase-1.md) e [validação](docs/spikes/phase-1-results.md)

- [Inventário](docs/DOCUMENTATION_INVENTORY.md)
- [Roadmap](docs/product/roadmap.md) e [hipóteses de pesquisa](docs/product/user-research.md)
- [Visão de arquitetura](docs/architecture/overview.md), [módulos](docs/architecture/modules.md) e [compatibilidade](docs/architecture/emulator-compatibility.md)
- [Configuração](docs/configuration.md), [segurança](docs/security.md), [limitações](docs/known-limitations.md) e [escopo S3](docs/services/s3.md)
- [Direção do design system](docs/design-system.md)
- [Testes](docs/development/testing.md), [release](docs/development/release.md) e [ADRs](docs/decisions/)

## Segurança

O MVP planejado não tem autenticação de usuários e destina-se apenas a ambiente local confiável. Vincule portas publicadas a loopback por padrão; nunca exponha a ferramenta à internet. Leia [SECURITY.md](SECURITY.md) antes de comunicar uma vulnerabilidade.

## Contribuição e licença

Consulte [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) e [LICENSE](LICENSE). A licença é MIT; licenças de dependências são separadas.
