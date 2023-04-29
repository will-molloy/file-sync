package com.willmolloy.backup;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BackupAlgorithmTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class BackupRunnerTest {

  @Mock private Backup mockBackup;
  @InjectMocks private BackupRunner backupRunner;

  @AfterEach
  void tearDown() {
    verify(mockBackup).scanSource();
    verify(mockBackup).scanDestination();
    verifyNoMoreInteractions(mockBackup);
  }

  @Test
  void copiesOrUpdatesSourceFiles() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of(Path.of("A"), Path.of("B"), Path.of("C")));
    when(mockBackup.scanDestination()).thenReturn(Stream.of());

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).copyOrUpdate(Path.of("A"));
    verify(mockBackup).copyOrUpdate(Path.of("B"));
    verify(mockBackup).copyOrUpdate(Path.of("C"));
  }

  @Test
  void deletesDestinationFiles() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of());
    when(mockBackup.scanDestination())
        .thenReturn(Stream.of(Path.of("D"), Path.of("E"), Path.of("F")));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).delete(Path.of("D"));
    verify(mockBackup).delete(Path.of("E"));
    verify(mockBackup).delete(Path.of("F"));
  }
}
