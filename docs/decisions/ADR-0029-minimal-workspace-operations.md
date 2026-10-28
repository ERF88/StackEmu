# ADR-0029 — Minimal workspace and durable S3 operations / Workspace mínimo e operações S3 persistentes

- Date / Data: 2026-10-22.
- Status / Estado: accepted implementation direction; verification recorded separately / direção de implementação aceita; verificações registradas separadamente.

## Decision / Decisão

The maintainer authorizes a platform-wide HIG-guided simplification and operational S3 expansion. Dialogs contain one title, exact target, short consequence and explicit action. Remove technical disclosures and per-item reports; successful actions close automatically and return concise dismissible feedback. Long operations are acknowledged as started, never as completed. Technical results belong to `/operations` and its detail pages. Preserve accessible names, keyboard/focus, localized messages, native canonical routes, CSP and CSRF.

O mantenedor autoriza simplificação HIG em toda a plataforma e ampliação operacional S3. Diálogos têm título único, alvo exato, consequência curta e ação explícita. Remover expansores técnicos e resultados por item; sucesso fecha automaticamente com aviso curto dispensável. Operações longas são iniciadas, não apresentadas como concluídas. Resultados técnicos ficam em `/operations` e páginas próprias. Preservar acessibilidade, teclado/foco, traduções, rotas canônicas, CSP e CSRF.

## Supersession / Substituição

This decision supersedes ADR-0028's 25-entry synchronous cleanup and mandatory five-minute review, ADR-0027's empty-marker-only folder UX, and ADR-0021/0026's blanket versioned-resource exclusion **as their tested replacements are delivered**. Explicit typed bucket name remains for bucket deletion only. Ordinary versioned deletion creates a delete marker; deleting a specific version and purging all bucket/prefix versions are distinct operations. Server inventories remain immutable after review; new objects are not silently adopted. Retries of ambiguous writes remain prohibited. Historical adapters and evidence do not define the new routes' capabilities.

Substitui limpeza síncrona de 25 entradas/validade obrigatória de cinco minutos, UX limitada a pasta vazia e bloqueio geral de versões **à medida que os substitutos forem entregues e testados**. Nome digitado permanece apenas para buckets. Exclusão comum versionada cria delete marker; exclusão de versão específica e limpeza de todas as versões são distintas. Inventários revisados não incorporam alvos novos; escritas incertas não são repetidas automaticamente. Preservar evidência histórica.

## Persistence and scope / Persistência e escopo

Use an embedded file H2 JDBC adapter, with application-owned records/ports and a bounded worker. One instance and one mutation per bucket; durable inventory/results, explicit interruption/resumption and 30-day configurable terminal retention. Persist intent before remote mutation. Restart leaves interrupted/uncertain work for reconciliation, never automatic replay. Closing the browser does not cancel work. Poll local records only while visible and active. Storage, pagination, transport and transfer limits remain configurable technical boundaries. Never store credentials or signed URLs in operation records.

Usar adaptador JDBC H2 em arquivo, records/ports da aplicação e worker limitado. Instância única, uma mutação por bucket, inventário/resultados persistentes, interrupção/retomada explícitas e retenção terminal configurável de 30 dias. Persistir intenção antes da mutação remota; reinício não repete escritas automaticamente. Fechar navegador não cancela. Consultar registros locais somente enquanto visíveis/ativos. Limites técnicos continuam configuráveis; não armazenar credenciais nem URLs assinadas.

Copy/move initially addresses individual objects within the configured endpoint. Move is copy followed by source removal, with explicit partial results. Conflicts require explicit replacement or another destination. Preserve file bytes, content type and extension. Presigned downloads default to 15 minutes, at most one hour and no longer than known credential lifetime; no signed uploads. Existing preview isolation remains. H2 persistence justifies `adapter.persistence`; it does not introduce generic modules or expose JDBC in application/web.

Cópia/movimentação de objetos individuais no endpoint configurado; mover é copiar e remover origem, com falhas parciais explícitas. Conflitos exigem substituição explícita ou outro destino; preservar bytes, tipo e extensão. Download assinado padrão 15 minutos, máximo uma hora/validade conhecida das credenciais; sem upload assinado. Isolamento de preview permanece. Persistência justifica `adapter.persistence`, sem JDBC em aplicação/web ou módulos genéricos.

## Delivery and validation / Entrega e validação

Deliver shared minimal UI, durable operations, version/prefix cleanup, transfer/signing and integrated workspace review in that order. Validate on host Java 25 and local MiniStack, including protocol capability checks, restart/duplicate/concurrency/partial outcomes, both languages/themes, keyboard, reduced motion and 390/1366/1440px. New container image validation stays a separate final stage. No production endpoint, public hosting, new service, publication, certification or legal-name approval is implied.

Entregar UI mínima, operações persistentes, limpeza de versões/prefixos, transferência/assinatura e revisão integrada nessa ordem. Validar Java 25 local e MiniStack, protocolo, reinício/duplicação/concorrência/falhas parciais, idiomas/temas/teclado/movimento e larguras previstas. Imagem nova fica para etapa final; sem produção, hospedagem pública, serviço novo, publicação, certificação ou validação jurídica do nome implícitas.
