package com.willmolloy.backup;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * FileSystemBackupPerformanceTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class FileSystemBackupPerformanceTest {

  private FileSystem fileSystem;
  private Path sourceRoot;
  private Path destinationRoot;

  @BeforeEach
  void setUp() throws IOException {
    fileSystem = Jimfs.newFileSystem(Configuration.unix());

    sourceRoot = fileSystem.getPath("/source");
    Files.createDirectory(sourceRoot);

    destinationRoot = fileSystem.getPath("/dest");
    Files.createDirectory(destinationRoot);
  }

  @AfterEach
  void tearDown() throws IOException {
    fileSystem.close();
  }

  @Disabled
  @ParameterizedTest
  @ValueSource(ints = {1_000, 10_000, 100_000, 1_000_000})
  void copy_files(int count) throws IOException {
    // Given
    List<Path> files =
        IntStream.range(0, count)
            .mapToObj(
                i -> {
                  try {
                    Path relativeFile = fileSystem.getPath("File-%d".formatted(i));
                    Files.createFile(sourceRoot.resolve(relativeFile));
                    return relativeFile;
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                })
            .toList();

    // When
    new BackupAlgorithm(new FileSystemBackup(sourceRoot, destinationRoot)).run();

    // Then
    assertThat(Files.list(sourceRoot))
        .containsExactlyElementsIn(files.stream().map(sourceRoot::resolve).toList());
    assertThat(Files.list(destinationRoot))
        .containsExactlyElementsIn(files.stream().map(destinationRoot::resolve).toList());
  }

  @Disabled
  @ParameterizedTest
  @ValueSource(ints = {1_000, 10_000, 100_000, 1_000_000})
  void delete_files(int count) throws IOException {
    // Given
    IntStream.range(0, count)
        .forEach(
            i -> {
              try {
                Path relativeFile = fileSystem.getPath("File-%d".formatted(i));
                Files.createFile(destinationRoot.resolve(relativeFile));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });

    // When
    new BackupAlgorithm(new FileSystemBackup(sourceRoot, destinationRoot)).run();

    // Then
    assertThat(Files.list(sourceRoot)).isEmpty();
    assertThat(Files.list(destinationRoot)).isEmpty();
  }
}
