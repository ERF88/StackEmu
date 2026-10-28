# Third-party assets

## Stackport interaction reference / Referência de interação Stackport

The maintainer selected [Stackport](https://github.com/DaviReisVieira/stackport), inspected at commit `f428f12ec95fb73b270f593695db056af195bfbe`, as a UX reference for S3 controls. No Stackport source, screenshot, logo or dependency is bundled. Search, move and share icon fragments are separate first-party MIT geometric drawings. / Stackport foi inspecionado como referência de UX; sem distribuição de código, capturas, marca ou dependências. Novos ícones de busca, mover e compartilhar são desenhos próprios MIT.

## Local console typography / Tipografia local do console

Space Grotesk variable TTF is distributed unchanged from [Google Fonts](https://github.com/google/fonts/tree/e7db6ca603fd4862010383ff17ffd6cc4ffac1c0/ofl/spacegrotesk), commit `e7db6ca603fd4862010383ff17ffd6cc4ffac1c0`. IBM Plex Sans Regular and SemiBold WOFF2 are distributed unchanged from [IBM Plex](https://github.com/IBM/plex/tree/71f558992315a12ca1f06a2215d57962a1992162/packages/plex-sans/fonts/complete/woff2), commit `71f558992315a12ca1f06a2215d57962a1992162`. Full copyright and SIL Open Font License 1.1 texts accompany the fonts under `src/main/resources/META-INF/licenses/space-grotesk-OFL.txt` and `ibm-plex-OFL.txt`. These fonts are not relicensed under the project's MIT license; preserve their notices when distributing the application. The history SVG is first-party MIT artwork.

Space Grotesk TTF variável e IBM Plex Sans Regular/SemiBold WOFF2 são distribuídas sem alterações, dos commits acima. Textos completos de copyright e SIL OFL 1.1 acompanham o artefato em `META-INF/licenses/`; preservar na distribuição. Fontes não são relicenciadas pela MIT do projeto. O SVG de histórico é desenho próprio MIT. Não há consulta a CDN de fontes em runtime.

## UX-0 isolated CSS tooling / Ferramentas CSS isoladas

The non-production `tools/ux0/package-lock.json` pins Tailwind CSS and CLI 4.3.3 (Tailwind Labs, MIT) and Pico CSS 2.1.1 (Pico, MIT). Neither stylesheet is shipped by the GUI in UX-0. Original license texts remain in installed packages; the spike copies the three direct-package licenses with hashes to ignored `work/ux0/` reports. Preserve the upstream copyright/permission notices if distributing their generated CSS. This entry does not certify all transitive tooling licenses; inspect the lockfile/package notices when distributing tooling. DaisyUI is not installed or bundled.

O lockfile isolado fixa Tailwind/CLI 4.3.3 e Pico 2.1.1, MIT, sem entregar seus estilos na GUI. Licenças originais ficam nos pacotes e são copiadas com hashes para os relatórios locais; preservar os avisos ao distribuir CSS gerado. Este registro não certifica todas as licenças transitivas. DaisyUI não foi instalado.

Sources / Fontes: [Tailwind](https://github.com/tailwindlabs/tailwindcss/blob/main/LICENSE), [Pico](https://github.com/picocss/pico/blob/main/LICENSE.md). The exact installed versions and license hashes, rather than moving branch URLs, identify the inspected texts. / Versões instaladas e hashes identificam os textos inspecionados.

## coolicons SVG subset / Subconjunto SVG coolicons

The subset below remains twenty third-party files. New `settings`, `close`, `back`, `delete` and `copy` Qute fragments are separate first-party MIT geometric drawings; no Apple font/SF Symbols assets are included. / Subconjunto externo abaixo permanece de vinte arquivos; cinco fragmentos novos são desenhos geométricos próprios MIT, sem assets Apple.

© Kryston Schwarze. coolicons v4.1, Line icons, licensed under [Creative Commons Attribution 4.0 International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/). Source: [official coolicons repository, tag v4.1](https://github.com/krystonschwarze/coolicons/tree/v4.1), pinned commit `42d70477683a0c44cd1732b32a9def8a94469c5a`; author/site: [coolicons.cool](https://coolicons.cool/). License declaration verified at [official legal disclosure](https://coolicons.cool/impress); terms reviewed at [CC BY 4.0 legal code](https://creativecommons.org/licenses/by/4.0/legalcode.en). The icons are provided without warranties under that license; this attribution does not imply endorsement.

Twenty selected SVGs are distributed as `src/main/resources/templates/icons/*.html`. Changes: root class/accessibility attributes added, repeated SVG group IDs removed, whitespace/indentation adapted for Qute. Original path geometry and currentColor strokes retained. No webfont, external sprite or CDN runtime request. The original StackEmu favicon is separate first-party MIT artwork. [Source mapping](docs/design/icon-sources.md) identifies each original filename. Keep this notice and license link when redistributing the subset; these third-party icons are not relicensed as MIT.

© Kryston Schwarze. coolicons v4.1 Line sob CC BY 4.0, com declaração e texto da licença nos links acima. Vinte SVGs selecionados, adaptados como fragmentos Qute: classe/atributos de acessibilidade adicionados, IDs repetidos removidos e indentação ajustada. Geometria e strokes currentColor preservados. Sem webfont, sprite externo ou CDN em runtime. O favicon próprio é arte MIT separada. Preservar atribuição e link da licença na redistribuição; os ícones de terceiros não são relicenciados como MIT. A atribuição não representa endosso e a licença inclui exclusão de garantias.

## Visual baseline captures / Capturas de baseline

Visual baseline screenshots under `tests/browser/baselines/` reproduce the existing UI, including the attributed coolicons subset above. Preserve this attribution when distributing those captures. E6 adds no third-party icon, font or runtime library. / Capturas de baseline reproduzem a UI e os coolicons acima; preservar atribuição ao distribuí-las. E6 não adiciona ícone, fonte ou biblioteca externa.

## Tailwind CSS product build / Build do produto

UX-1 compiles Tailwind CSS/CLI 4.3.3 into the local stylesheet. Tailwind CSS is MIT licensed, copyright Tailwind Labs, Inc.; the complete upstream license is copied into `META-INF/licenses/tailwindcss-LICENSE.txt` in the Java artifact and `/licenses/Tailwind-CSS-LICENSE` in the runtime image. Node/npm and the CLI are build tools, not runtime servers. Preserve the license on redistribution. / UX-1 compila Tailwind no CSS local; licença MIT integral acompanha artefato e imagem nos caminhos acima. Node/npm/CLI são ferramentas de build; preservar licença na redistribuição.

## htmx

`src/main/resources/META-INF/resources/vendor/htmx.min.js` is htmx 2.0.8,
downloaded from <https://unpkg.com/htmx.org@2.0.8/dist/htmx.min.js>.
Copyright (c) 2020 Big Sky Software. Licensed under the Zero-Clause BSD license:

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

## H2 Database Engine

The unmodified `com.h2database:h2:2.4.240` runtime dependency is dual-licensed under MPL 2.0 or EPL 1.0, as declared by its versioned Maven POM and [upstream licensing notice](https://h2database.com/html/license.html). H2 is provided without warranties or contributor/distributor liability under those terms. Corresponding source is available as the same-version `sources` artifact on Maven Central. The packaged notice is `META-INF/licenses/h2-NOTICE.txt`, also copied to `/licenses/H2-NOTICE` by the runtime-image recipe. This dependency is not relicensed as MIT. / Dependência H2 sem modificações, sob MPL 2.0 ou EPL 1.0; aviso e fonte da mesma versão disponíveis, sem garantia/responsabilidade conforme termos originais. Aviso acompanha artefato/receita de imagem; H2 não é relicenciado como MIT.
