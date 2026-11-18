# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Software developers use StackEmu on their local workstation to build and verify integrations. A platform team owns the shared product contract and reduces per-project setup drift.

## Product Purpose

StackEmu provides one repeatable local sandbox and web console for developing against a bounded set of AWS services. Success means developers can see whether their sandbox is ready, understand the next useful step, and exercise the supported integration flows without assembling a project-specific environment.

## Positioning

One platform-owned Docker image, one `stackemu.yaml` per project, and one native `stackemu` launcher provide a standard local path for S3, SQS, and SNS development. StackEmu is a focused local emulator experience, not a claim of full AWS compatibility or production equivalence.

## Operating Context

Developers use the console on the same workstation as the sandbox, through loopback endpoints. The primary repeated tasks are checking sandbox health, finding the endpoint and connection settings, and inspecting or exercising local S3, SQS, and SNS resources. A preloaded sandbox must also start without network access.

## Capabilities and Constraints

- The existing console is a Quarkus and Qute server-rendered web application with HTMX progressive enhancement.
- V1 centers on S3, SQS, and SNS, with only the operations in the published SDK and AWS CLI compatibility matrix supported.
- The first console view should make sandbox health and the next useful action clear.
- The UI may reorganize or remove existing flows when that improves the developer task; it must not suggest unsupported service operations.
- The console runs locally and should avoid unnecessary client-side dependencies, network calls, and expensive visual effects.
- Project configuration and diagnostics must not expose secrets.
- A full AWS Console clone, broad service administration, remote shared sandboxes, Kafka, RabbitMQ, and WireMock are outside v1.

## Evidence on Hand

- Existing S3, SQS, and SNS console routes and templates under `stackemu-console/src/main/resources/templates/`.
- Existing AWS adapters and service contracts under `stackemu-console/src/main/java/io/github/erf88/stackemu/`.
- SNS-to-SQS integration coverage is present, but the current console has no SNS service page.
- Product scope and local sandbox constraints are recorded in `concepts_brief.md` and `docs/designs/stackemu-local-aws-sandbox-v1.md`.

## Product Principles

- Put environment health and recovery guidance before exploration.
- Keep service operations aligned with the tested compatibility matrix.
- Make the common developer path direct and reversible; make destructive actions explicit.
- Keep the interface local-first, fast, and useful without decorative client-side complexity.
