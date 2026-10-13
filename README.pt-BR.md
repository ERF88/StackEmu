# StackEmu

> **Documentação de planejamento — não é um produto implementado.** StackEmu é uma proposta de interface web open source para administrar emuladores locais compatíveis com APIs AWS, começando por S3. Não é o console da AWS, não administra contas AWS e ainda não foi validado contra um emulador.

[English](README.md)

## Status

O nome do produto foi escolhido em 13 de outubro de 2026, mas a validação jurídica e de nome está **pendente**. O repositório do projeto é [ERF88/StackEmu](https://github.com/ERF88/StackEmu). Antes de uma release pública, validar registros de pacotes, Docker Hub, domínio e INPI. StackEmu não possui afiliação com AWS, MiniStack, LocalStack ou Moto.

O alvo do MVP é um vertical slice utilizável de S3: status da conexão, listagem/criação de buckets, navegação por prefixo, upload, download por streaming, falhas compreensíveis, comandos CLI equivalentes e UI em inglês/pt-BR. Nada neste repositório demonstra que essas capacidades já funcionam.

## Stack planejada

Java 25 LTS (Java 21 avaliado como alternativa), Quarkus 3.40 LTS, Qute, htmx, CSS, Maven e AWS SDK for Java 2.x. Versões exatas de extensões, BOM, JDK e emulador exigem spike de implementação e validação em fontes oficiais.

## Início rápido — planejado, não verificado

A topologia de referência coloca o contêiner da GUI conectado ao MiniStack em `http://ministack:4566`, com a GUI publicada somente em `127.0.0.1:8080`. Não há aplicação nem arquivo Compose neste baseline de documentação. Não trate o comando abaixo como testado:

```sh
docker compose up
```

Quando implementado e verificado, o guia cobrirá início do ambiente, criação de bucket, upload/download, parada e remoção segura dos dados locais.

## Documentação

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
