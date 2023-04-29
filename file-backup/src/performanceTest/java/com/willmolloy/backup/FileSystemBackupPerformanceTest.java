package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * FileSystemBackupPerformanceTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class FileSystemBackupPerformanceTest {

  private Path fs;
  private Path sourceRoot;
  private Path destRoot;

  @BeforeEach
  void setUp() throws IOException {
    fs = Path.of("build").resolve(FileSystemBackupPerformanceTest.class.getSimpleName());
    delete(fs);

    sourceRoot = fs.resolve("source");
    Files.createDirectories(sourceRoot);

    destRoot = fs.resolve("dest");
    Files.createDirectories(destRoot);
  }

  @AfterEach
  void tearDown() {
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

  // TODO files with random contents
  // TODO random directory structure

  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  @ParameterizedTest
  @ValueSource(ints = {1_000, 10_000, 100_000})
  void copy_files(int count) {
    // Given
    List<Path> files =
        IntStream.range(0, count)
            .mapToObj(
                i -> {
                  try {
                    Path relativeFile = Path.of("File-%d".formatted(i));
                    Files.createFile(sourceRoot.resolve(relativeFile));
                    return relativeFile;
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                })
            .toList();

    // When
    Main.main(sourceRoot.toString(), destRoot.toString());

    // Then
    // single assert (e.g. containsExactlyElementsIn) is more correct, but too slow here.
    files.forEach(
        file -> {
          assertThat(Files.exists(sourceRoot.resolve(file))).isTrue();
          assertThat(Files.exists(destRoot.resolve(file))).isTrue();
        });
  }

  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  @ParameterizedTest
  @ValueSource(ints = {1_000, 10_000, 100_000})
  void delete_files(int count) throws IOException {
    // Given
    IntStream.range(0, count)
        .forEach(
            i -> {
              try {
                Path relativeFile = Path.of("File-%d".formatted(i));
                Files.createFile(destRoot.resolve(relativeFile));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });

    // When
    Main.main(sourceRoot.toString(), destRoot.toString());

    // Then
    assertThat(Files.list(sourceRoot)).isEmpty();
    assertThat(Files.list(destRoot)).isEmpty();
  }
}
