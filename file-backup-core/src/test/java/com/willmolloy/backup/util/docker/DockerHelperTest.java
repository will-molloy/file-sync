package com.willmolloy.backup.util.docker;

import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.util.docker.DockerEngineApi.ContainerInspect;
import com.willmolloy.backup.util.docker.DockerEngineApi.VolumeInspect;
import com.willmolloy.backup.util.docker.DockerEngineApi.VolumeInspect.Options;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DockerHelperTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class DockerHelperTest {

  @Mock private DockerEngineApi mockApi;
  private DockerHelper sut;

  @BeforeEach
  void setUp() {
    sut = new DockerHelper(Optional.of("my_container"), mockApi);
  }

  @Test
  void getHostPath_whenNotInDockerContainer_empty() {
    // Given
    DockerHelper sut = new DockerHelper(Optional.empty(), mockApi);

    // Then
    assertThat(sut.getHostPath("")).isEmpty();
    verifyNoInteractions(mockApi);
  }

  @Test
  void getHostPath_whenBind_getsHostPath() {
    // Given
    when(mockApi.containerInspect("my_container"))
        .thenReturn(
            Optional.of(
                new ContainerInspect(
                    List.of(
                        new ContainerInspect.Mount("bind", "host/path/1", "container/path/1")))));

    // When
    Optional<String> result = sut.getHostPath("container/path/1");

    // Then
    assertThat(result).hasValue("host/path/1");
  }

  @Test
  void getHostPath_whenVolume_getsHostPathViaVolume() {
    // Given
    when(mockApi.containerInspect("my_container"))
        .thenReturn(
            Optional.of(
                new ContainerInspect(
                    List.of(
                        new ContainerInspect.Mount("bind", "host/path/1", "container/path/1"),
                        new ContainerInspect.Mount(
                            "volume",
                            "/var/lib/docker/volumes/my_volume/_data",
                            "container/path/2")))));
    when(mockApi.volumeInspect("my_volume"))
        .thenReturn(Optional.of(new VolumeInspect(new Options("host/path/2"))));

    // When
    Optional<String> result = sut.getHostPath("container/path/2");

    // Then
    assertThat(result).hasValue("host/path/2");
  }

  @Test
  void getHostPath_whenVolume_doesntMatchPattern_failsGracefully() {
    // Given
    when(mockApi.containerInspect("my_container"))
        .thenReturn(
            Optional.of(
                new ContainerInspect(
                    List.of(
                        new ContainerInspect.Mount("volume", "my_volume", "container/path/2")))));

    // When
    Optional<String> result = sut.getHostPath("container/path/2");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void getHostPath_whenUnknownMount_failsGracefully() {
    // Given
    when(mockApi.containerInspect("my_container"))
        .thenReturn(
            Optional.of(
                new ContainerInspect(
                    List.of(
                        new ContainerInspect.Mount("???", "host/path/1", "container/path/1")))));

    // When
    Optional<String> result = sut.getHostPath("container/path/1");

    // Then
    assertThat(result).isEmpty();
    ;
  }
}
