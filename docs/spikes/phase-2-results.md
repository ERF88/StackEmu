# Phase 2 evidence / Evidências da Fase 2

## Executed locally / Executado localmente

On 13 October 2026, Windows/amd64, Java 25, Quarkus 3.40.1 and MiniStack 1.5.25:

- 28 Maven tests passed, zero failures: boundaries/localization, optional metadata/tags, pagination/common prefixes, three-level navigation, special/Unicode keys, escaped HTML, safe errors and stream closure/cancellation.
- A real 32 MiB integration download preserved size and SHA-256.
- Compose downloaded 128 MiB with `JAVA_TOOL_OPTIONS=-Xmx64m`. Size/hash matched: `C751E6B12D0372A88B1AB63D89A7E16149705772759EC31F3F4E7E8CF98CF785`. One transfer took about 2.55 seconds; this is not a performance baseline. Heap is not total process memory.
- A generated 128 MiB stream verified reads at most 64 KiB and closure on success/cancellation.
- Browser navigation traversed three prefixes to `leia-me + ação.txt`. Language switching retained the object; a 390 × 844 viewport showed wrapping and download/back actions.
- Compose smoke and code/style checks passed. CI publication is recorded separately; local success is not CI evidence.

Os 28 testes locais passaram. Downloads reais de 32 MiB e 128 MiB preservaram tamanho/hash; a segunda prova limitou heap a 64 MiB. Navegação, caracteres especiais, idioma e tela estreita foram inspecionados. Isso não certifica S3 completo, memória total ou desempenho.

## Remaining / Pendente

LocalStack, Moto, arm64, full S3 semantics, range/resume, versions and production load are unverified. Cancellation has a deterministic stream test, not network chaos. GUI mutations/security belong to subsequent phases. Legal name validation remains pending.

LocalStack, Moto, arm64, compatibilidade completa, retomada, versões e carga permanecem não verificados. Escritas/segurança pertencem às próximas fases; a validação jurídica do nome continua pendente.
