package io.github.erf88.stackemu.adapter.aws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Owns one bounded worker at a time; shared by the application-scoped preview adapter. */
@jakarta.inject.Singleton
final class RasterPreviewProcess {
  private final Semaphore slot;
  private final Path temporaryRoot;

  @jakarta.inject.Inject
  RasterPreviewProcess() {
    this(new Semaphore(1), Path.of(System.getProperty("java.io.tmpdir")));
  }

  RasterPreviewProcess(Semaphore slot, Path temporaryRoot) {
    this.slot = slot;
    this.temporaryRoot = temporaryRoot;
  }

  byte[] reencode(byte[] bytes, String format) throws IOException, InterruptedException {
    if (bytes.length == 0
        || bytes.length > RasterPreviewWorker.MAX_BYTES
        || (!"png".equals(format) && !"jpeg".equals(format))) throw new IOException("Unsupported");
    if (!slot.tryAcquire()) throw new IOException("Busy");
    Path directory = null;
    try {
      directory = Files.createTempDirectory(temporaryRoot, "stackemu-raster-");
      Path worker =
          directory.resolve("io/github/erf88/stackemu/adapter/aws/RasterPreviewWorker.class");
      Files.createDirectories(worker.getParent());
      try (var resource =
          RasterPreviewWorker.class.getResourceAsStream("RasterPreviewWorker.class")) {
        if (resource == null) throw new IOException("Worker unavailable");
        byte[] code = resource.readNBytes(65537);
        if (code.length > 65536) throw new IOException("Worker limit");
        Files.write(worker, code);
      }
      Path input = directory.resolve("input.bin");
      Path output = directory.resolve("output.png");
      Files.write(input, bytes);
      String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
      List<String> command =
          List.of(
              Path.of(System.getProperty("java.home"), "bin", executable).toString(),
              "-Xmx128m",
              "-XX:ActiveProcessorCount=1",
              "-Djava.awt.headless=true",
              "-cp",
              directory.toString(),
              RasterPreviewWorker.class.getName(),
              format);
      run(command, directory, input, output, Duration.ofSeconds(5));
      if (Files.size(output) == 0 || Files.size(output) > RasterPreviewWorker.MAX_BYTES)
        throw new IOException("Output limit");
      return Files.readAllBytes(output);
    } finally {
      try {
        if (directory != null) {
          // Only this invocation's generated files, never paths supplied by an object key.
          try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
              Files.delete(path);
          }
        }
      } finally {
        slot.release();
      }
    }
  }

  static void run(List<String> command, Path directory, Path input, Path output, Duration budget)
      throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
    builder.environment().clear();
    builder.redirectInput(input.toFile());
    builder.redirectOutput(output.toFile());
    builder.redirectError(ProcessBuilder.Redirect.DISCARD);
    Process process = builder.start();
    try {
      if (!process.waitFor(budget.toMillis(), TimeUnit.MILLISECONDS))
        throw new IOException("Timeout");
      if (process.exitValue() == 2) throw new WorkerException();
      if (process.exitValue() != 0) throw new IOException("Worker unavailable");
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
        // Preserve interruption only after confirming worker termination and releasing files.
        boolean interrupted = false;
        while (process.isAlive()) {
          try {
            process.waitFor();
          } catch (InterruptedException failure) {
            interrupted = true;
          }
        }
        if (interrupted) Thread.currentThread().interrupt();
      }
    }
  }

  static final class WorkerException extends IOException {
    WorkerException() {
      super("Decode refused");
    }
  }
}
