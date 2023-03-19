package com.willmolloy;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class LocalToNasIntegrationTest {

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

  @Test
  void whenFilesOnlyOnSource_copiesFilesToDestination() throws IOException {
    // Given
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

  @Test
  void whenFilesOnlyOnDestination_deletesFilesFromDestination() throws IOException {
    // Given
    Files.createFile(destinationRoot.resolve("D"));
    Files.createFile(destinationRoot.resolve("E"));
    Files.createFile(destinationRoot.resolve("F"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  // TODO test the last modified part?
  //  Even better to put some contents in the files and ensure it gets updated.
  //  Last-Modified is a performance improvement, cover with performance test instead?
  void whenFilesOnSourceAndDestination_updatesFilesOnDestination() throws IOException {
    // Given
    Files.createFile(sourceRoot.resolve("X"));
    Files.createFile(sourceRoot.resolve("Y"));
    Files.createFile(sourceRoot.resolve("Z"));
    Files.createFile(destinationRoot.resolve("X"));
    Files.createFile(destinationRoot.resolve("Y"));
    Files.createFile(destinationRoot.resolve("Z"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve("X"),
            sourceRoot.resolve("Y"),
            sourceRoot.resolve("Z"),
            destinationRoot.resolve("X"),
            destinationRoot.resolve("Y"),
            destinationRoot.resolve("Z"));
  }

  @Test
  void allThreeConditionsAtOnce() throws IOException {
    // Given
    Files.createFile(sourceRoot.resolve("A"));
    Files.createFile(sourceRoot.resolve("B"));
    Files.createFile(sourceRoot.resolve("C"));
    Files.createFile(destinationRoot.resolve("D"));
    Files.createFile(destinationRoot.resolve("E"));
    Files.createFile(destinationRoot.resolve("F"));
    Files.createFile(sourceRoot.resolve("X"));
    Files.createFile(sourceRoot.resolve("Y"));
    Files.createFile(sourceRoot.resolve("Z"));
    Files.createFile(destinationRoot.resolve("X"));
    Files.createFile(destinationRoot.resolve("Y"));
    Files.createFile(destinationRoot.resolve("Z"));

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
            destinationRoot.resolve("C"),
            sourceRoot.resolve("X"),
            sourceRoot.resolve("Y"),
            sourceRoot.resolve("Z"),
            destinationRoot.resolve("X"),
            destinationRoot.resolve("Y"),
            destinationRoot.resolve("Z"));
  }

  @Test
  void whenDirectoryOnlyOnSource_copiesDirectoryToDestination() throws IOException {
    // Given
    Files.createDirectories(sourceRoot.resolve("A/B/C"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve("A").resolve("B").resolve("C"),
            destinationRoot.resolve("A").resolve("B").resolve("C"));
  }

  @Test
  void whenDirectoryOnlyOnDestination_deletesDirectoryFromDestination() throws IOException {
    // Given
    Files.createDirectories(destinationRoot.resolve("A/B/C"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void whenDirectoryOnSourceAndDestination_updatesDirectoryOnDestination() throws IOException {
    // Given
    Files.createDirectories(sourceRoot.resolve("A/B/C"));
    Files.createDirectories(destinationRoot.resolve("A/B/C"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve("A").resolve("B").resolve("C"),
            destinationRoot.resolve("A").resolve("B").resolve("C"));
  }

  @Test
  void whenDirectoryOnSourceAndParentDirectoryOnDestination_copiesChildDirectoryToDestination()
      throws IOException {
    // Given
    Files.createDirectories(sourceRoot.resolve("A/B/C"));
    Files.createDirectories(destinationRoot.resolve("A/B/C"));

    // When
    LocalToNasJob job = new LocalToNasJob(sourceRoot, destinationRoot);
    JobRunner jobRunner = new JobRunner();
    jobRunner.run(job);

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve("A").resolve("B").resolve("C"),
            destinationRoot.resolve("A").resolve("B").resolve("C"));
  }

  private StreamSubject assertThatFileSystem() throws IOException {
    try (Stream<Path> sourceFiles = Files.walk(sourceRoot).skip(1)) {
      try (Stream<Path> destinationFiles = Files.walk(destinationRoot).skip(1)) {
        return assertThat(Stream.concat(sourceFiles, destinationFiles).filter(Helpers::isLeaf));
      }
    }
  }
}
