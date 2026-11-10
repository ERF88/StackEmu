# Stage 5: initial zoom-out review

**Target:** docs/designs/stackemu-local-aws-sandbox-v1.md

## Findings

### Lifecycle boundary is not implementable — Blocker

The image cannot derive, create, label, and mount its own project volume from
stackemu.yaml: the host selects mounts before the container starts. An
image-internal start command cannot start its own target container either.
Define one minimal host interface that reads project configuration, selects or
creates the volume, and launches the image before in-container preflight.

### Reset audit destination conflicts with the volume contract — Major

The design requires the reset result outside erased state but defines one
project volume. Specify a durable bounded destination, or limit the audit claim
to Docker logs.

### Health design should reuse SmallRye Health — Major

The project already includes quarkus-smallrye-health. Use its liveness and
readiness endpoints with a focused readiness check instead of a parallel
custom health resource.

### Lifecycle class list is over-specified — Nit

Define config/state preflight, bootstrap/reset, and reporting responsibilities;
do not pre-commit the pilot to ten concrete lifecycle classes until the
host/container boundary is decided.
