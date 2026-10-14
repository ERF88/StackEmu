# StackEmu

> **Fundação da Fase 1.** StackEmu é uma interface web open source para emuladores locais AWS, com buckets S3 em leitura, configuração efetiva de conexão, navegação compartilhada e temas claro/escuro. O MVP continua em desenvolvimento.

[English](README.md)

## Status

O nome do produto foi escolhido em 13 de outubro de 2026, mas a validação jurídica e de nome está **pendente**. O repositório do projeto é [ERF88/StackEmu](https://github.com/ERF88/StackEmu). Antes de uma release pública, validar registros de pacotes, Docker Hub, domínio e INPI. StackEmu não possui afiliação com AWS, MiniStack, LocalStack ou Moto.

O alvo do MVP é um vertical slice utilizável de S3. A Fase 0 comprova listagem de buckets, página/fragmento Qute, falhas localizadas e execução no Compose. As outras capacidades do MVP continuam pendentes.

## Stack planejada

Java 25 LTS (Java 21 avaliado como alternativa), Quarkus 3.40 LTS, Qute, htmx, CSS, Maven e AWS SDK for Java 2.x. Versões exatas de extensões, BOM, JDK e emulador exigem spike de implementação e validação em fontes oficiais.

## Início rápido — Fase 0

A GUI conecta ao MiniStack em `http://ministack:4566`, publicada somente em `127.0.0.1:8080`. Requer Docker Compose v2. O comando foi executado localmente em Windows/amd64:

```sh
docker compose up --build -d
```

Abra http://127.0.0.1:8080/?lang=pt-BR. Consulte o [guia da Fase 0](docs/development/phase-0.md) e as [evidências/limitações](docs/spikes/phase-0-results.md). Para parar, use `docker compose down`; os volumes são preservados. A GUI ainda não cria buckets nem envia/baixa objetos.

## Documentação

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

Consulte [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) e [LICENSE](LICENSE). A licença planejada é MIT.
