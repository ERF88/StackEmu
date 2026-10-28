# Como contribuir com StackEmu

[English](CONTRIBUTING.md)

Para alterações de UI, seguir o [padrão de feedback das interações](docs/design/interaction-feedback.md): reutilizar controlador comum, abrir conteúdo verificado sem janelas vazias, preservar foco válido em diálogos e distinguir feedback tardio de leitura do estado imediato de mutações. Incluir na revisão checks relevantes de resposta rápida/lenta, falha, teclado, repetição de clique e fallback nativo.

Antes de alterar código, siga o [guia de nomenclatura e responsabilidades](docs/development/naming-and-responsibilities.md) e seu checklist de revisão. O baseline atual inclui a leitura da Fase 2.

O StackEmu tem a fundação da Fase 1. Consulte as evidências registradas antes de afirmar implementação ou compatibilidade.

1. Leia o [roadmap](docs/product/roadmap.md), os [ADRs](docs/decisions/) aplicáveis e o [Código de Conduta](CODE_OF_CONDUCT.md).
2. Abra issue ou discussão antes de mudanças relevantes de design.
3. Mantenha mudanças pequenas, documente comportamento e limitações e ajuste testes quando houver código.
4. Não adicione credenciais, resultados que você não executou ou alegações de compatibilidade sem evidência reproduzível.
5. Mantenha inglês e pt-BR sincronizados onde o plano exige documentação bilíngue.

Siga as [convenções e comandos](docs/development/code-style.md). Aplique `mvn spotless:apply` e `npm run format`; confira `npm run check` e `mvn clean verify`. O CI verifica formatação, Markdown/CSS, PowerShell, Dockerfile, workflows, Compose, traduções, arquitetura, integração e smoke test. Declare somente validações realmente executadas.
