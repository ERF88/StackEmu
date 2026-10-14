# Phase 3 — S3 writes / Fase 3 — Escritas S3

## English

Open the dashboard, choose **Create bucket**, enter a name and confirm the action. Open the bucket and choose **Upload new object** or **Create folder marker**. Enter the complete destination key/prefix; the filename never becomes a filesystem path or an implicit object key. Review the destination and confirm. The result page links to the created resource and shows its CLI equivalent. Navigate and download using Phase 2.

POST routes: `/s3/write/bucket` and `/s3/write/folder` use URL-encoded forms; `/s3/write/upload` uses multipart. `/s3/write?action=bucket|upload|folder&bucket=…&prefix=…` provides forms. Validation errors use HTTP 422 and field messages; conflicts 409; CSRF rejection 400; HTTP body rejection 413. Invalid/failed submissions do not silently overwrite an object. Upload inputs must be selected again after an error.

The conservative write-name policy accepts 3–63 lowercase letters/digits/hyphens, with alphanumeric ends and no reserved prefixes/suffixes. Dotted/legacy names may still be read but are not write targets in this baseline. Object keys have at most 1024 UTF-8 bytes and no control characters. Marker keys include the final slash in this limit. A marker is a zero-byte S3 object, not a real directory.

`STACKEMU_MAX_UPLOAD` is the file-byte limit, default `10485760` (10 MiB), positive integer. `STACKEMU_HTTP_MAX_BODY` is the whole-request ceiling, default `11M`. Raise both deliberately, allowing multipart overhead; an HTTP ceiling below the file limit can reject the request first. There is no resumable upload or S3 multipart-upload protocol: this is one HTTP multipart form followed by one SDK PutObject from a temporary file. Content type is deliberately stored as `application/octet-stream` rather than trusting the browser's claim.

Quarkus creates the upload filename under `${java.io.tmpdir}/stackemu-uploads`. The handler deletes it in `finally`; framework response-end cleanup covers parser/filter rejections. Cleanup after those rejections is asynchronous and tests wait at most five seconds for an empty test-only directory. These controls do not establish crash recovery or a disk-quota guarantee.

Writes use `S3WritePort`, `AwsS3WriteAdapter`, pure application validation and a separate `S3WriteResource`. Conditional PutObject sends `If-None-Match: *`; existing keys return conflict. Bucket creation probes existence first; concurrent bucket creation is not a transactional guarantee. Other emulators, versions, races and crash behavior need separate evidence.

CLI commands are selectable/copyable POSIX-shell text, not executable buttons. They contain the redacted endpoint, region and quoted targets, never configured credentials. `/path/to/file` must be replaced. The initial form command reflects server-loaded values; the success command reflects the completed action. Editing inputs does not dynamically rewrite it. Credential setup remains the user's responsibility outside the UI.

CSRF uses the Quarkus filter and an HttpOnly cookie with a hidden token. No new first-party JavaScript is required. This is not authentication, and the unsigned default double-submit scheme is not a public deployment security certification. Keep loopback-only trusted development use; stronger token signing and broader threat review remain quality/security work.

## Português (Brasil)

No dashboard, escolha **Criar bucket**, informe o nome e confirme. No bucket, use **Enviar novo objeto** ou **Criar marcador de pasta**; informe o destino completo e confirme. O nome do arquivo não define caminhos no servidor nem automaticamente a chave S3. A página de resultado oferece link para o recurso e comando CLI equivalente. Depois, navegue e baixe pelo fluxo da Fase 2.

As rotas acima retornam erros por campo (422), conflito (409), CSRF (400) ou limite HTTP (413). A política de nomes de escrita é conservadora: letras minúsculas, números e hífens, 3–63 caracteres; nomes com pontos continuam legíveis, mas não são destinos de escrita. Chaves têm limite de 1024 bytes UTF-8 e não aceitam controles. Marcadores são objetos vazios terminados em `/`, não diretórios reais.

O arquivo tem limite padrão de 10 MiB; o corpo HTTP inteiro tem padrão `11M`. Ajuste ambas as variáveis com margem para multipart. O upload não é retomável nem usa o protocolo multipart do S3. O tipo armazenado é `application/octet-stream`. Temporários têm nomes gerados pelo servidor, limpeza no `finally` e limpeza pelo framework em rejeições; estas podem terminar de forma assíncrona. Não há garantia de recuperação após crash nem quota de disco.

Comandos CLI são texto copiável para shell POSIX, nunca executado e sem credenciais configuradas. A prévia não muda enquanto o usuário edita os campos; o resultado mostra o destino concluído. Substitua o caminho ilustrativo de arquivo. Escritas usam port separado e PutObject condicional, sem sobrescrita intencional. Concorrência de criação de buckets, outros emuladores, versões e crashes precisam de evidência própria.

O filtro CSRF usa cookie HttpOnly e token oculto. Não equivale a autenticação nem certifica exposição pública; assinatura de tokens e revisão ampla de ameaças permanecem para qualidade/segurança. Use apenas loopback e ambiente local confiável.

## Reproduction / Reprodução

```powershell
docker compose up -d ministack --wait
$env:STACKEMU_INTEGRATION = 'true'
mvn -B -ntp spotless:apply
mvn -B -ntp clean verify
docker compose up -d --build gui
./scripts/smoke.ps1
./scripts/verify-writes.ps1
```

Use Java 25 and the established style checks. Tests create/remove only uniquely named fixtures; never run against production. See [evidence](../spikes/phase-3-results.md).

References: [Quarkus CSRF](https://quarkus.io/guides/security-csrf-prevention/), [REST uploads](https://quarkus.io/guides/rest/), [S3 conditional writes](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html), [bucket naming](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html).
