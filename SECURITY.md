# Security policy

[Português (Brasil)](SECURITY.pt-BR.md)

## Supported versions

No released version exists. Security support and supported-version policy will be defined before the first public release.

## Reporting a vulnerability

Do not disclose vulnerabilities in public issues. Until a private reporting channel is published, contact the project maintainers through the private contact listed in the release repository. Include impact, reproduction steps, affected version/commit, and any suggested mitigation. Do not include credentials or private data.

## Planned security posture

StackEmu is planned for trusted local development only. The reference Compose mapping binds the GUI to loopback. It must not mount the Docker socket, run as root, execute an arbitrary shell, log secrets, or expose secret values in HTML/errors. Mutations require CSRF protection; object output is escaped and downloads are attachments with defensive headers.

An endpoint configured by environment is a potential SSRF risk if the service is exposed or misconfigured. Per-request endpoint changes are out of scope for the MVP. These are requirements, not verified controls.
