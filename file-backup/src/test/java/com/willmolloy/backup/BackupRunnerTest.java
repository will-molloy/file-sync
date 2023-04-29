package com.willmolloy.backup;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BackupRunnerTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class BackupRunnerTest {

  @Mock private Backup.Source mockSource;
  @Mock private Backup.Destination mockDestination;
  @Mock private Backup mockBackup;
  @InjectMocks private BackupRunner backupRunner;

  @BeforeEach
  void setUp() {
    when(mockBackup.source()).thenReturn(mockSource);
    when(mockBackup.destination()).thenReturn(mockDestination);
  }

  @AfterEach
  void tearDown() {
    verify(mockSource).scan();
    verify(mockDestination).scan();
    verifyNoMoreInteractions(mockSource);
    verifyNoMoreInteractions(mockDestination);
    verifyNoMoreInteractions(mockBackup);
  }

  @Test
  void copiesOrUpdatesSourceFiles() {
    // Given
    when(mockSource.scan()).thenReturn(Stream.of(Path.of("A"), Path.of("B"), Path.of("C")));
    when(mockDestination.scan()).thenReturn(Stream.of());

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).tryCopyOrUpdate(Path.of("A"));
    verify(mockBackup).tryCopyOrUpdate(Path.of("B"));
    verify(mockBackup).tryCopyOrUpdate(Path.of("C"));
  }

  @Test
  void deletesDestinationFiles() {
    // Given
    when(mockSource.scan()).thenReturn(Stream.of());
    when(mockDestination.scan()).thenReturn(Stream.of(Path.of("D"), Path.of("E"), Path.of("F")));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).tryDelete(Path.of("D"));
    verify(mockBackup).tryDelete(Path.of("E"));
    verify(mockBackup).tryDelete(Path.of("F"));
  }
}
