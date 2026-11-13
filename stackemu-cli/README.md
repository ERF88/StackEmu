# StackEmu CLI

Build the self-contained launcher with Maven:

    mvn -B -ntp -f stackemu-cli/pom.xml package

Run it from a project directory containing stackemu.yaml. The command uses the
Docker CLI, creates a labelled stackemu-project-id volume when persistence is
enabled, and starts the local stackemu:local image. By default, AWS APIs and the
console are available at http://127.0.0.1:4566 and http://127.0.0.1:8080.
Projects can declare a distinct loopback-only pair in `stackemu.yaml`:

```yaml
ports:
  aws: 14566
  console: 18080
```

Use `http://127.0.0.1:14566` for local SDK/AWS CLI clients and open the console
at `http://127.0.0.1:18080`. If a selected port is occupied, `start` or
`restart` exits with code 3 and asks you to choose another port. A port-only
`restart` preserves the project's volume; changing declared resources still
follows the existing reset flow. Use status or diagnose to inspect effective
endpoints and the current port mapping. Reset requires the project ID as an
argument or the explicit --yes flag.

The Compose harness is for development and runs the combined MiniStack and
StackEmu console image on loopback ports 4566 and 8080.

## Native build

Install GraalVM 25 and the native compiler toolchain for your operating
system, then build the launcher with:

```sh
mvn -Pnative -DskipTests package
```

The executable is written to `target/stackemu` (`target/stackemu.exe` on
Windows). The `native-cli` GitHub Actions workflow smoke-tests Linux x64/arm64,
Windows x64, and macOS x64/arm64 builds.
