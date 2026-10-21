# Project history / Histórico do projeto

Consolidated / Consolidado: 2026-10-16. [Current status](status.md) is authoritative for today's implementation/open gates. Phase names below describe history, not the current product version. / Status vinculado define presente; fases abaixo são histórico, não versão atual.

## Delivery lineage / Evolução das entregas

| Stage / Etapa     | Recorded contribution / Contribuição registrada                                                                      | Evidence / Evidência                                                                           |
| ----------------- | -------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| Phase 0 / Fase 0  | Toolchain, Compose, endpoint and initial rendering / Toolchain, Compose, endpoint e renderização inicial             | [Phase 0](evidence/phase-0-results.md)                                                         |
| Phase 1 / Fase 1  | Adapter boundaries, effective connection and shared UI / Limites de adapters, conexão efetiva e UI                   | [Phase 1](evidence/phase-1-results.md)                                                         |
| Phase 2 / Fase 2  | Reads, pagination/prefixes and bounded streaming/heap proof / Leituras, paginação/prefixos e prova de streaming/heap | [Phase 2](evidence/phase-2-results.md)                                                         |
| Phase 3 / Fase 3  | Confirmed CSRF-protected writes and bounded upload / Escritas confirmadas com CSRF e upload limitado                 | [Phase 3](evidence/phase-3-results.md), [original files](evidence/original-file-results.md)    |
| Phase 4 / Fase 4  | Browser/axe checks and response policy / Navegador/axe e política de respostas                                       | [Phase 4](evidence/phase-4-results.md)                                                         |
| Phase 5 / Fase 5  | Container policy, SBOM/scan and later runtime remediation / Política de contêiner, SBOM/scan e correção posterior    | [Phase 5](evidence/phase-5-results.md), [remediation](evidence/runtime-remediation-results.md) |
| Workspace + E1–E7 | Responsive workspace, tokens/icons/components and redesign checks / Workspace, tokens/ícones/componentes e checks    | [Workspace](evidence/operational-workspace-results.md), [E7](evidence/redesign-e7-results.md)  |
| UX-0–1            | CSS comparison, build-only Tailwind and Slate/blue shell / Comparação CSS, Tailwind no build e shell                 | [UX-0](evidence/ux-0-css-results.md), [UX-1](evidence/ux-1-results.md)                         |
| UX-2A/2B          | Explorer table, metadata drawer and bucket/key clipboard / Tabela, drawer e clipboard                                | [UX-2A](evidence/ux-2a-results.md), [UX-2B](evidence/ux-2b-results.md)                         |
| UX-3/4            | Drop/progress and persistent notices / Drop/progresso e avisos persistentes                                          | [UX-3](evidence/ux-3-results.md), [UX-4](evidence/ux-4-results.md)                             |
| UX-5A/5B          | Bounded UTF-8 text and isolated static PNG/JPEG preview / Previews limitados de texto e imagens                      | [UX-5A](evidence/ux-5a-results.md), [UX-5B](evidence/ux-5b-results.md)                         |

Counts, versions, failed attempts and limits belong to each run record. Earlier container/scan success is not evidence for a later candidate. `486f807` was pushed with `[skip ci]`; new-image consolidated validation remains for the final stage. / Contagens/versões/falhas/limites permanecem nos registros; candidato posterior exige evidência nova. Push não executou CI completo.

## Documentation consolidation / Consolidação documental

The supplied adjustment analyzed `1f622f5`; implementation subsequently reached `486f807`. Approved scope on 2026-10-16: documentation items 1–4 only (READMEs, grouped index, evidence relocation, status/maturity corrections). D1–D6 and new V0–V6 visual implementation are not implicitly approved. / Plano analisou commit anterior; escopo aprovado é documental 1–4, sem aprovar decisões ou novo redesenho.

All 33 files formerly under `docs/spikes/` were moved with `git mv` to `docs/evidence/`, retaining original bytes. The former `README.md` becomes `phase-0-spike-index.md`: its “Pending” rows are an archived checklist, not current status. Current guide/ADR/changelog links use the new paths. Literal old paths or stale labels inside evidence remain historical text. / Todos os arquivos movidos com conteúdo preservado; índice inicial renomeado e tratado como histórico. Links atuais atualizados; textos internos de evidências não reescritos.

Git history retains old paths for old commits; no alias records or duplicated evidence copies are introduced. The [index](README.md) lists evidence separately from guidance. Link/index/byte-preservation checks are documentation validation, not new Java/browser/container results. / Histórico preservado, sem duplicar evidências; checks documentais não são resultados novos de runtime.
