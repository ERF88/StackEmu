# CSS build / Build CSS

## English

HTML responses use `Cache-Control: no-store`; versioned public assets retain their immutable cache policy. Keep these concerns separate: fresh HTML must reference the current fingerprints, and each fingerprint must match the actual served bytes. Already open documents still need navigation/reload after a packaged GUI restart. Do not infer a user's stale-client cause merely from a successful fresh-session test.

UX-1 uses Tailwind CSS/CLI 4.3.3 at build time only, without Preflight, CDN, default theme or a browser compiler. The authored semantic stylesheet remains `src/main/resources/META-INF/resources/app.css`; `src/main/frontend/input.css` imports it and scans literal classes in Qute templates. Do not construct utility tokens from emulator data. Utilities currently migrate shell layout only; retain semantic component classes and token-based colors elsewhere.

`mvn verify` provisions Node 24.10.0/npm 11.6.1 under `target/frontend`, installs the root lockfile with scripts disabled, and runs `scripts/build-css.mjs` during `generate-resources`. First builds need network access. Authored CSS is excluded from Maven resources; generated `target/generated-resources/META-INF/resources/app.css` is packaged at the existing URL and fingerprinted by `WebAssetCatalog`. The Tailwind MIT license is packaged under `META-INF/licenses/`. Generated files and toolchains are not committed.

The Docker Node stage performs the same compilation. Maven receives its generated resources with `-Dskip.frontend=true`; that flag is only valid when generated resources already exist. The final non-root Java image contains neither Node nor npm and includes the Tailwind license in `/licenses/`. Java runtime routes and CSP are unchanged. The Maven SBOM describes Java dependencies; the root npm lockfile separately inventories frontend build dependencies.

For direct CSS iteration use Node 24.10.0: `npm ci --ignore-scripts --no-audit --no-fund`, then `npm run build:css`. Rebuild/restart the packaged GUI to see the result; never serve the authored stylesheet as the compiled product. Run `npm run check` and Maven verification. Browser baselines must be reviewed deliberately in the pinned Linux Playwright container, not silently regenerated to hide failures.

## Toolchain concurrency / Concorrência da toolchain

Do not run multiple `npm ci` operations against the same checkout, or style/browser checks while installation replaces `node_modules`. Maven installs root dependencies, so sequence checks after that lifecycle step. On constrained Windows hosts use explicit JVM/Node heap limits and inspect failures; a native-memory crash is not a successful verification. / Não executar instalações concorrentes nem checks enquanto `node_modules` é substituído; Maven instala dependências na raiz. Em hosts limitados, usar limites de heap e verificar falhas, sem tratar crash como sucesso.

Verified Windows invocation with bounded JVM heaps, local MiniStack already healthy / Comando Windows verificado com heaps limitados e MiniStack local saudável:

```powershell
$env:STACKEMU_INTEGRATION='true'
$env:MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2'
mvn -B -ntp verify '-DargLine=-Xmx384m -XX:ActiveProcessorCount=2'
```

These are build/test settings, not a runtime capacity claim. Docker also bounds its Maven build heap; the Java runtime retains its separate Compose policy. / São opções de build/teste, não capacidade de runtime; a imagem limita o build Maven, enquanto o runtime mantém a política Compose separada.

## Português

Respostas HTML usam `Cache-Control: no-store`; assets públicos versionados preservam cache imutável. HTML novo deve apontar aos fingerprints atuais e cada hash deve corresponder aos bytes servidos. Documentos já abertos ainda precisam de navegação/recarga após reinício da GUI empacotada. Teste em sessão nova aprovado não comprova sozinho cache antigo na sessão do usuário.

A UX-1 usa Tailwind CSS/CLI 4.3.3 somente no build, sem Preflight, CDN, tema padrão ou compilador no navegador. O CSS semântico autoral permanece em `src/main/resources/META-INF/resources/app.css`; a entrada `src/main/frontend/input.css` o importa e detecta classes literais nos Qute. Não montar classes com dados do emulador. A migração de utilitários limita-se ao layout do shell; manter componentes semânticos e cores por tokens.

`mvn verify` provisiona Node 24.10.0/npm 11.6.1 em `target/frontend`, instala pelo lockfile com scripts desativados e compila em `generate-resources`. O primeiro build exige rede. O CSS autoral é excluído dos resources Maven; o gerado em `target/generated-resources/META-INF/resources/app.css` é empacotado na URL existente, com hash calculado pelo catálogo. A licença MIT do Tailwind acompanha o artefato em `META-INF/licenses/`. Gerados e toolchain não são versionados.

No Docker, o estágio Node compila; Maven recebe os resources prontos com `-Dskip.frontend=true`, válido somente com gerados presentes. A imagem Java final não inclui Node/npm e mantém a licença em `/licenses/`. Rotas e CSP não mudam. O SBOM Maven cobre Java; o lockfile npm inventaria separadamente dependências de build frontend.

Para iterar, executar os comandos Node acima e reconstruir/reiniciar a GUI empacotada; o CSS autoral não substitui o produto compilado. Rodar checks e Maven; revisar mudanças de baseline no contêiner Linux fixado, sem regenerar para esconder falhas.
