package com.willmolloy;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests Local To NAS job with real file system.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class LocalToNasIntegrationTest {

  private FileSystem fileSystem;

  @BeforeEach
  void setUp() {
    fileSystem = Jimfs.newFileSystem(Configuration.unix());
  }

  @AfterEach
  void tearDown() throws IOException {
    fileSystem.close();
  }

  @Test
  void copiesFilesFromSourceToDestination() throws IOException {
    // Given
    Path sourceRoot = fileSystem.getPath("/source");
    Files.createDirectory(sourceRoot);
    Path destinationRoot = fileSystem.getPath("/dest");
    Files.createDirectory(destinationRoot);

    Files.createFile(sourceRoot.resolve("A"));
    Files.createFile(sourceRoot.resolve("B"));
    Files.createFile(sourceRoot.resolve("C"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve("A"),
            sourceRoot.resolve("B"),
            sourceRoot.resolve("C"),
            destinationRoot.resolve("A"),
            destinationRoot.resolve("B"),
            destinationRoot.resolve("C"));
  }

  private StreamSubject assertThatFileSystem() throws IOException {
    try (Stream<Path> testFiles = Files.walk(fileSystem.getPath("/"))) {
      return assertThat(testFiles.filter(Files::isRegularFile));
    }
  }
}
