package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RasterPreviewProcessTest {
  @TempDir Path directory;

  @Test
  void runsExtractedJdkOnlyWorkerAndKeepsOriginalBytesUnchanged() throws Exception {
    var worker = new RasterPreviewProcess();
    for (String format : new String[] {"png", "jpeg"}) {
      byte[] original = fixture(format, 3, 2, false);
      byte[] copy = original.clone();
      byte[] result = worker.reencode(original, format);
      BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
      assertEquals(3, decoded.getWidth());
      assertEquals(2, decoded.getHeight());
      assertArrayEquals(copy, original);
    }
  }

  @Test
  void refusesInvalidFormatsDimensionsAndRecoversForNextRequest() throws Exception {
    var worker = new RasterPreviewProcess();
    assertThrows(IOException.class, () -> worker.reencode(new byte[0], "png"));
    assertThrows(IOException.class, () -> worker.reencode(new byte[1024 * 1024 + 1], "png"));
    assertThrows(IOException.class, () -> worker.reencode(new byte[] {1, 2, 3}, "png"));
    assertThrows(IOException.class, () -> worker.reencode(fixture("png", 2, 2, false), "jpeg"));
    assertThrows(IOException.class, () -> worker.reencode(fixture("png", 4097, 1, false), "png"));
    assertThrows(
        IOException.class, () -> worker.reencode(fixture("png", 2001, 2000, false), "png"));
    assertThrows(IOException.class, () -> worker.reencode(fixture("png", 2, 2, false), "svg"));
    assertTrue(worker.reencode(fixture("png", 4096, 1, false), "png").length > 0);
  }

  @Test
  void outputAboveLimitIsRefusedRatherThanReturnedPartially() throws Exception {
    // Compressed JPEG input fits; fresh noisy lossless RGBA PNG exceeds the output policy.
    byte[] jpeg = fixture("jpeg", 900, 900, true);
    assertTrue(jpeg.length < RasterPreviewWorker.MAX_BYTES);
    assertThrows(IOException.class, () -> new RasterPreviewProcess().reencode(jpeg, "jpeg"));
  }

  @Test
  void forciblyTerminatesTimedOutProcessBeforeReturning() throws Exception {
    Path input = Files.write(directory.resolve("input"), new byte[0]);
    Path output = directory.resolve("output");
    assertThrows(
        IOException.class,
        () ->
            RasterPreviewProcess.run(
                command("sleep"), directory, input, output, Duration.ofMillis(1200)));
    assertFalse(
        ProcessHandle.of(Long.parseLong(Files.readString(directory.resolve("pid"))))
            .map(ProcessHandle::isAlive)
            .orElse(false));
  }

  @Test
  void interruptionTerminatesWorkerAndStartupFailureIsReported() throws Exception {
    Path input = Files.write(directory.resolve("input"), new byte[0]);
    Path output = directory.resolve("output");
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread request =
        Thread.ofPlatform()
            .start(
                () -> {
                  try {
                    RasterPreviewProcess.run(
                        command("sleep"), directory, input, output, Duration.ofSeconds(15));
                  } catch (IOException | InterruptedException expected) {
                    failure.set(expected);
                  }
                });
    long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
    while (!Files.exists(directory.resolve("pid")) && System.nanoTime() < deadline)
      Thread.sleep(20);
    request.interrupt();
    request.join(5000);
    assertFalse(request.isAlive());
    assertTrue(failure.get() instanceof InterruptedException);
    assertFalse(
        ProcessHandle.of(Long.parseLong(Files.readString(directory.resolve("pid"))))
            .map(ProcessHandle::isAlive)
            .orElse(false));
    assertThrows(
        IOException.class,
        () ->
            RasterPreviewProcess.run(
                List.of(directory.resolve("missing-java").toString()),
                directory,
                input,
                output,
                Duration.ofSeconds(1)));
  }

  @Test
  void occupiedSlotRefusesImmediatelyAndTemporaryFilesAreRemovedOnSuccessAndFailure()
      throws Exception {
    Semaphore slot = new Semaphore(0);
    var worker = new RasterPreviewProcess(slot, directory);
    byte[] png = fixture("png", 2, 2, false);
    assertThrows(IOException.class, () -> worker.reencode(png, "png"));
    try (var paths = Files.list(directory)) {
      assertEquals(0, paths.count());
    }
    slot.release();
    assertTrue(worker.reencode(png, "png").length > 0);
    try (var paths = Files.list(directory)) {
      assertEquals(0, paths.count());
    }
    assertThrows(IOException.class, () -> worker.reencode(new byte[] {1}, "png"));
    try (var paths = Files.list(directory)) {
      assertEquals(0, paths.count());
    }
    assertEquals(1, slot.availablePermits());
  }

  @Test
  void discardedDiagnosticsCannotBlockOrContaminateOutput() throws Exception {
    Path input = Files.write(directory.resolve("input"), new byte[0]);
    Path output = directory.resolve("output");
    RasterPreviewProcess.run(command("noise"), directory, input, output, Duration.ofSeconds(5));
    assertEquals("done", Files.readString(output));
  }

  @Test
  void subprocessDoesNotInheritEnvironmentOrJavaOptionVariables() throws Exception {
    Path input = Files.write(directory.resolve("input"), new byte[0]);
    Path output = directory.resolve("output");
    RasterPreviewProcess.run(
        command("environment"), directory, input, output, Duration.ofSeconds(5));
    assertEquals("empty", Files.readString(output));
  }

  private static List<String> command(String mode) {
    String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    String classpath;
    try {
      classpath =
          Path.of(
                  RasterPreviewProcessTest.class
                      .getProtectionDomain()
                      .getCodeSource()
                      .getLocation()
                      .toURI())
              .toString();
    } catch (java.net.URISyntaxException failure) {
      throw new IllegalStateException(failure);
    }
    return List.of(
        Path.of(System.getProperty("java.home"), "bin", executable).toString(),
        "-Xmx64m",
        "-cp",
        classpath,
        ProcessFixture.class.getName(),
        mode);
  }

  private static byte[] fixture(String format, int width, int height, boolean noisy)
      throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    if (noisy) {
      Random random = new Random(42);
      for (int y = 0; y < height; y++)
        for (int x = 0; x < width; x++) image.setRGB(x, y, random.nextInt());
    }
    var output = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(image, format, output));
    return output.toByteArray();
  }

  public static class ProcessFixture {
    public static void main(String[] arguments) throws Exception {
      if (arguments[0].equals("environment")) {
        // Windows may add the OS-required SystemRoot even after environment().clear().
        boolean unexpected =
            System.getenv().keySet().stream()
                .anyMatch(name -> !name.equalsIgnoreCase("SystemRoot"));
        System.out.print(unexpected ? "inherited" : "empty");
      } else if (arguments[0].equals("noise")) {
        System.err.write(new byte[1024 * 1024]);
        System.out.print("done");
      } else {
        Files.writeString(Path.of("pid"), Long.toString(ProcessHandle.current().pid()));
        Thread.sleep(30000);
      }
    }
  }
}
