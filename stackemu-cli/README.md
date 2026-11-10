# StackEmu CLI

Build the self-contained launcher with Maven:

    mvn -B -ntp -f stackemu-cli/pom.xml package

Run it from a project directory containing stackemu.yaml. The command uses the
Docker CLI, creates a labelled stackemu-project-id volume when persistence is
enabled, and starts the local stackemu:local image. Use status or diagnose to
inspect the resolved project and volume. Reset requires the project ID as an
argument or the explicit --yes flag.

The Compose harness is for development and runs the combined MiniStack and
StackEmu console image on loopback ports 4566 and 8080.
