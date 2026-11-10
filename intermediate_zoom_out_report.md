# Stage 8: intermediate zoom-out review

## Residual findings

* **Major:** Remove the unbounded repair command or promote it to a fully
  specified public contract; v1 should prefer reset retry.
* **Major:** Replace lease/fencing coordination with a conservative local lock
  tied to Docker/container lifecycle.
* **Major:** Do not pre-commit lifecycle types or a custom HealthResource; use
  responsibilities and SmallRye checks.
* **Major:** Reuse JdbcOperationStoreAdapter by pointing its existing path and
  retention settings at project state.
* **Nit:** Move exact version/retention/error constants to configuration and
  compatibility documents; retain only observable guarantees here.
