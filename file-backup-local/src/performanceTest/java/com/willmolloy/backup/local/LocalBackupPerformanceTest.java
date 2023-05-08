package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.github.javafaker.Faker;
import com.willmolloy.backup.util.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * LocalBackupPerformanceTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class LocalBackupPerformanceTest {

  private static final Logger log = LogManager.getLogger();

  private Path fs;
  private Path sourceRoot;
  private Path destRoot;
  private Faker faker;
  private Random random;

  @BeforeEach
  void setUp() throws IOException {
    fs = Path.of("build").resolve(getClass().getSimpleName());
    delete(fs);

    sourceRoot = fs.resolve("source");
    Files.createDirectories(sourceRoot);

    destRoot = fs.resolve("dest");
    Files.createDirectories(destRoot);

    faker = new Faker();
    random = new Random();
  }

  @AfterEach
  void tearDown() {
    log.info("Cleaning up...");
    delete(fs);
  }

  private void delete(Path path) {
    try {
      if (Files.isDirectory(path)) {
        Files.list(path).forEach(this::delete);
      }
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @ParameterizedTest(name = "{0} {1}MB file(s)")
  @MethodSource
  @SetEnvironmentVariable(key = "SOURCE_PATH", value = "build/LocalBackupPerformanceTest/source")
  @SetEnvironmentVariable(key = "DESTINATION_PATH", value = "build/LocalBackupPerformanceTest/dest")
  void performanceTest(int count, int fileSizeInMB) throws IOException {
    // Given
    List<String> fileNames = generateRandomFiles(count, fileSizeInMB);

    // When
    long start = System.nanoTime();
    Main.main();
    Duration duration = Duration.ofNanos(System.nanoTime() - start);

    // Then
    assertThat(duration).isLessThan(Duration.ofMinutes(1));
    assertThat(Files.walk(sourceRoot).filter(Files::isRegularFile))
        .containsExactlyElementsIn(fileNames.stream().map(sourceRoot::resolve).toList());
    assertThat(Files.walk(destRoot).filter(Files::isRegularFile))
        .containsExactlyElementsIn(fileNames.stream().map(destRoot::resolve).toList());
  }

  static Stream<Arguments> performanceTest() {
    return Stream.of(
        Arguments.of(1_000, 10),
        Arguments.of(100, 100),
        Arguments.of(10, 1_000),
        Arguments.of(2, 5_000));
  }

  private List<String> generateRandomFiles(int count, int sizeInMB) throws IOException {
    Preconditions.require(count * sizeInMB <= 10_000, "GitHub Actions disk space limit");
    Preconditions.require(count > 0 && count % 2 == 0, "Can't create files evenly on source/dest");
    log.info("Generating {} random {}MB file(s)...", count, sizeInMB);

    List<String> fileNames = new ArrayList<>();
    for (int i = 0; i < count / 2; i++) {
      String fileName = randomFileName();
      fileNames.add(fileName);

      Path sourceFile = sourceRoot.resolve(fileName);
      createFileWithRandomContents(sourceFile, sizeInMB);

      // reuse half the names to trigger updates
      Path destFile = destRoot.resolve(i % 2 == 0 ? fileName : randomFileName());
      createFileWithRandomContents(destFile, sizeInMB);
    }
    return fileNames;
  }

  private String randomFileName() {
    return faker.file().fileName() + faker.random().hex();
  }

  private void createFileWithRandomContents(Path file, int sizeInMB) throws IOException {
    Path parent = file.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.createFile(file);

    try (OutputStream outputStream = Files.newOutputStream(file)) {
      for (int i = 0; i < sizeInMB; i++) {
        byte[] oneMB = new byte[1_000_000];
        random.nextBytes(oneMB);
        outputStream.write(oneMB);
      }
    }
  }
}
