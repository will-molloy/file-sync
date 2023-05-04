package com.willmolloy.backup.local;

import static com.google.common.truth.Truth8.assertThat;

import com.github.javafaker.Faker;
import com.willmolloy.backup.Main;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * LocalBackupIntegrationTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class LocalBackupIntegrationTest {

  private static final Logger log = LogManager.getLogger();

  private Path fs;
  private Path sourceRoot;
  private Path destRoot;
  private Faker faker;

  @BeforeEach
  void setUp() throws IOException {
    fs = Path.of("build").resolve(getClass().getSimpleName());
    delete(fs);

    sourceRoot = fs.resolve("source");
    Files.createDirectories(sourceRoot);

    destRoot = fs.resolve("dest");
    Files.createDirectories(destRoot);

    faker = new Faker();
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

  @ParameterizedTest
  @ValueSource(ints = {100, 1_000})
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void test(int count) throws IOException {
    // Given
    List<Path> sourceFiles = createRandomFilesIn(sourceRoot, count).toList();
    createRandomFilesIn(destRoot, count).forEach(p -> {});

    // When
    Main.main(LocalBackup.class.getSimpleName(), sourceRoot.toString(), destRoot.toString());

    // Then
    assertThat(Files.walk(sourceRoot).filter(Files::isRegularFile))
        .containsExactlyElementsIn(sourceFiles);
    assertThat(Files.walk(destRoot).filter(Files::isRegularFile))
        .containsExactlyElementsIn(
            sourceFiles.stream()
                .map(sourceFile -> destRoot.resolve(sourceRoot.relativize(sourceFile)))
                .toList());
  }

  private Stream<Path> createRandomFilesIn(Path root, int count) {
    log.info("Creating random files in {}...", root);
    return IntStream.range(0, count).mapToObj(i -> createRandomFileIn(root));
  }

  private Path createRandomFileIn(Path root) {
    try {
      // ensure unique name
      Path file = root.resolve(faker.file().fileName() + faker.random().hex());
      Path parent = file.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.createFile(file);
      try (BufferedWriter bufferedWriter =
          Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
        for (long i = 0; i < faker.number().numberBetween(100, 1_000); i++) {
          bufferedWriter.write(faker.lorem().paragraph());
          bufferedWriter.newLine();
        }
      }
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
