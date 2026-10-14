# Phase 0 / Fase 0

## Run / Executar

Requires Docker Compose v2. For a local Maven build, select JDK 25 in `JAVA_HOME` and install Maven 3.9.9 or newer. The container build uses Maven 3.9.11 and Java 25.

```sh
docker compose up --build -d
```

Open http://127.0.0.1:8080/ (English) or http://127.0.0.1:8080/?lang=pt-BR.
The Phase 0 GUI lists buckets only. It does not create buckets or upload/download objects yet.

To create a bucket using an installed AWS CLI (development credentials only):

Alternatively use the AWS CLI included in the tested MiniStack image, without installing it on the host:

```sh
docker compose exec -T -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test ministack aws --endpoint-url http://localhost:4566 --region us-east-1 s3api create-bucket --bucket stackemu-demo
docker compose exec -T -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test ministack aws --endpoint-url http://localhost:4566 --region us-east-1 s3api list-buckets
```

Host CLI alternative:

```sh
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test aws --endpoint-url http://localhost:4566 --region us-east-1 s3api create-bucket --bucket stackemu-demo
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test aws --endpoint-url http://localhost:4566 --region us-east-1 s3api list-buckets
```

PowerShell users set `$env:AWS_ACCESS_KEY_ID='test'` and `$env:AWS_SECRET_ACCESS_KEY='test'` before invoking the same `aws` commands.

## Tests / Testes

```sh
mvn -B -ntp verify
STACKEMU_INTEGRATION=true mvn -B -ntp verify
```

The default suite tests an unreachable endpoint, localized fragments and process health. The second command also runs a real MiniStack integration test at `http://localhost:4566`: creates a uniquely named empty bucket, verifies full-page/fragment listing, then deletes only that test bucket.
In PowerShell set `$env:STACKEMU_INTEGRATION='true'` first.
After Compose startup, run `powershell -File scripts/smoke.ps1`.

## Stop / Parar

`docker compose stop` stops the services. `docker compose down` removes containers/network and preserves named volumes. `docker compose down -v` permanently removes this Compose project's stored emulator data; use only when that data is disposable.

## Scope / Escopo

One Maven module, checked Qute templates, locally served htmx 2.0.8, bounded GET-only partial updates, SDK endpoint override/path-style, non-root JVM container, loopback published ports, and persistent MiniStack state/object volumes. No Node build pipeline is needed. A future mutation spike must still validate CSRF; this read-only screen does not establish mutation security.
