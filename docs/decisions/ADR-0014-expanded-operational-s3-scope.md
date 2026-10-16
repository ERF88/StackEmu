# ADR-0014 — expanded operational S3 delivery / Entrega operacional S3 ampliada

- Date / Data: 2026-10-15.
- Status: ACCEPTED scope decision only; capability implementations and technical contracts remain gated / DECISÃO DE ESCOPO ACEITA; implementações e contratos técnicos permanecem condicionados.

## Context / Contexto

The user explicitly requested preview, deletion, clipboard, presigned URLs and full S3 management in the first current delivery, then clarified operational management rather than advanced bucket configuration. This is direct product prioritization, not a research/market-validation result. The historical ADR-0002 scope was smaller; its executed evidence must not be rewritten.

O usuário pediu explicitamente essas capacidades na primeira entrega atual e confirmou gerenciamento operacional, não configurações avançadas. É priorização direta, não resultado de pesquisa/mercado. O escopo histórico da ADR-0002 era menor; suas evidências permanecem intactas.

## Decision / Decisão

Include the complete operational inventory in the [scope record](../product/s3-management-scope.md): buckets, prefixes/markers, object browsing/metadata, bounded original-file upload/download, safe preview, copy/move, individual and bounded bulk/prefix deletion, explicit bucket empty/delete, clipboard and presigned download links. Deliver incrementally with explicit implementation status. Drawer/clipboard and later capabilities are not implemented merely by accepting this ADR or by completing UX-2A.

Incluir todo o inventário operacional do registro vinculado: buckets, prefixos/markers, objetos/metadados, upload/download original limitado, preview seguro, cópia/movimentação, exclusão individual e limitada em lote/prefixo, esvaziar/excluir bucket com confirmação, clipboard e links assinados de download. Entregar por fatias, sem confundir escopo aceito ou UX-2A com implementação das demais capacidades.

Exclude advanced configuration editors and version-management UI, other services, real AWS administration, internet exposure and generic plugins. Version-state detection is nevertheless mandatory for safe destructive/copy/move semantics. Exact batch sizes, preview limits, signer TTL/endpoint and emulator compatibility remain unresolved until their respective technical work; never assume unsupported behavior works.

Excluir editores avançados/UI de versões, outros serviços, administração AWS real, exposição à internet e plugins genéricos. Detectar estado de versão continua obrigatório para operações seguras. Tamanhos de lote, limites de preview, TTL/endpoint de assinatura e compatibilidade ainda precisam de trabalho técnico e evidência.

## Supersession and safety / Substituição e segurança

Supersede only ADR-0002's scheduling exclusion of operational destructive cleanup/copy-move and ADR-0013/UX-plan labels that place preview/deletion/presigning outside this first delivery. Do not supersede architecture boundaries, CSP/CSRF, original-file attachment policy, bounded I/O, no-overwrite defaults or capability-specific acceptance requirements. Dedicated accepted mutation/preview/signing contracts and exact-target local tests must precede exposing new operations. Original upload basename policy is unchanged; any explicit copy/move destination rules need their own contract.

Substituir somente exclusões de agendamento operacional e rótulos que colocavam essas capacidades fora da primeira entrega. Não substituir arquitetura, CSP/CSRF, arquivo original/anexo, I/O limitado, padrão sem sobrescrita ou gates específicos. Contratos técnicos aceitos e testes locais de alvo exato precedem a exposição de operações novas. O basename original de upload continua; destino de cópia/movimentação exige contrato próprio.

Keep one Maven module and `application`/`adapter.aws`/`adapter.web` boundaries. Do not introduce unused ports/modules for the backlog. This ADR records no new compatibility/security certification, successful user research, release approval or GitHub publication. Legal/name validation remains pending.

Manter módulo único e limites existentes, sem portas/módulos sem consumidores reais. Esta ADR não certifica compatibilidade/segurança, pesquisa, release ou publicação. Validação jurídica/do nome permanece pendente.
