# Release candidates

The `Release candidate` workflow builds a reviewable, multi-platform candidate from `main`. It does not publish a stable release.

## Build a candidate

1. Open **Actions → Release candidate → Run workflow** and select `main`.
2. Enter a version without the leading `v`, such as `0.1.0-rc.1`.
3. Review the completed jobs. The workflow builds and smoke-tests the native CLI for Windows x64, macOS x64/arm64, and Linux x64/arm64; publishes the image to GHCR; then creates a **draft pre-release**.
4. Review the draft assets and `image-digests.json`. Check all downloaded files with `SHA256SUMS`.
5. Before asking pilot users to pull the image, verify that the GHCR package is public. Package visibility is controlled separately from the public source repository. Publish the draft only after the candidate has been reviewed.

The workflow accepts only `X.Y.Z-rc.N` versions and runs from `main`. It applies that version to the Maven reactor with the CI-friendly `revision` property, to the OCI image tag/labels, and to the release assets. The image manifest records the source commit, published image digest, target platforms, and pinned MiniStack base image.

## Test a candidate locally

The current launcher uses the local Docker tag `stackemu:local`. Pull the versioned GHCR image and assign that tag before starting a downloaded CLI:

```sh
docker pull ghcr.io/erf88/stackemu:v0.1.0-rc.1
docker tag ghcr.io/erf88/stackemu:v0.1.0-rc.1 stackemu:local
```

Then rename the downloaded binary to `stackemu` (or `stackemu.exe` on Windows), make it executable on macOS/Linux, and run it from a directory containing `stackemu.yaml`.

Use the immutable image reference from `image-digests.json` when validating or preloading a candidate by digest. The release build pins the MiniStack base to its multi-platform manifest digest, so the base selected for each architecture cannot silently move with a tag.

## What the draft contains

- Versioned native binaries: `stackemu-<version>-linux-x64`, `stackemu-<version>-linux-arm64`, `stackemu-<version>-darwin-x64`, `stackemu-<version>-darwin-arm64`, and `stackemu-<version>-windows-x64.exe`.
- `image-digests.json` with the image tag, digest, platform list, source commit, and pinned base image.
- `SHA256SUMS` covering every binary and the image manifest.

The release workflow is intentionally manual. Maven CI and the native CLI workflow still run on pull requests; the opt-in Docker lifecycle workflow remains separate from candidate publication.
