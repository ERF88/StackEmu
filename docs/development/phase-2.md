# Phase 2 — S3 reading / Fase 2 — Leitura S3

## English

Open the dashboard and select a bucket. The bucket page shows the configured client region, optional emulator-reported region and tags, common prefixes, and one object page. Follow prefixes or breadcrumbs, then select an object to view size, content type, ETag and last modification in UTC. Missing metadata is shown as a dash. Prefixes are key conventions, not filesystem directories.

Routes are `/s3/bucket?bucket=…&prefix=…&token=…`, `/s3/object?bucket=…&key=…` and `/s3/download?bucket=…&key=…`. Language/theme links preserve navigation context. Keys are URL-encoded without interpreting `+`, percent signs or Unicode as path syntax. Download uses a backend stream, attachment disposition, `nosniff` and `no-store`; object HTML is not rendered.

`STACKEMU_PAGE_SIZE` maps to `stackemu.page-size`, defaults to 50 and is clamped to 1–1000. Each listing uses one `ListObjectsV2` request with `delimiter=/`. “Next page” carries the opaque continuation token; “First page” resets it. There is no global count, automatic full listing, or invented previous-page cursor.

Run Maven verification with Java 25 and `STACKEMU_INTEGRATION=true` against the Compose emulator. The integration suite creates and cleans its own uniquely named fixtures. Do not use production endpoints or credentials.

The test-only heap proof uses an additional Compose file:

```powershell
docker compose -f compose.yaml -f compose.streaming-test.yaml up -d --no-deps gui
try { ./scripts/verify-streaming.ps1 } finally { docker compose up -d --no-deps gui }
```

The script creates and removes its own bucket/object and temporary files. It requires PowerShell 7, a running built GUI and MiniStack. It verifies a 128 MiB transfer with a 64 MiB Java heap, size and SHA-256 equality. This is not an RSS/native-memory benchmark or throughput baseline. Restore the normal GUI even when the test fails.

Before response headers, errors use localized safe messages and appropriate HTTP statuses. A failure after streaming starts terminates the transfer without appending an HTML error. The client must treat an interrupted transfer as incomplete. No range/resume or version selection is implemented. GUI writes, upload and deletion remain outside this phase.

## Português (Brasil)

Abra o dashboard e selecione um bucket. A página mostra a região configurada do cliente, região informada pelo emulador e tags quando disponíveis, prefixos e uma página de objetos. Navegue pelos prefixos/breadcrumbs e selecione um objeto para ver tamanho, tipo, ETag e última alteração em UTC. Dados ausentes aparecem como traço. Prefixos são convenções de chaves, não diretórios de um sistema de arquivos.

As rotas e o procedimento reproduzível acima valem para ambos os idiomas. Trocar idioma/tema preserva o contexto. As chaves são codificadas em URL preservando Unicode e caracteres especiais. O download passa pelo backend com streaming e cabeçalhos de anexo; HTML armazenado não é executado.

`STACKEMU_PAGE_SIZE` tem padrão 50 e limite defensivo de 1 a 1000. Cada página corresponde a uma chamada `ListObjectsV2`, com `delimiter=/`; o botão seguinte transporta o token opaco e a primeira página o remove. Não há contagem global nem carregamento integral da coleção.

Execute a verificação Maven com Java 25 e `STACKEMU_INTEGRATION=true`. As fixtures usam nomes únicos e são removidas pelos testes. A prova adicional acima baixa 128 MiB com heap Java de 64 MiB e compara tamanho/SHA-256; não mede toda a memória do processo nem estabelece benchmark. Restaure o Compose normal no `finally`.

Falhas antes dos cabeçalhos recebem mensagem segura localizada; falhas durante a transmissão interrompem o download sem acrescentar HTML. Range/retomada, seleção de versões e escritas pela GUI ainda não existem. Consulte as [evidências](../evidence/phase-2-results.md).
