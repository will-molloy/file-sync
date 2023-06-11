package com.willmolloy.backup.util.docker;

import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.util.HttpClientWrapper;
import com.willmolloy.backup.util.docker.DockerEngineApi.ContainerInspect;
import com.willmolloy.backup.util.docker.DockerEngineApi.VolumeInspect;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DockerEngineApiTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class DockerEngineApiTest {

  @Mock private HttpClientWrapper mockHttpClientWrapper;
  @InjectMocks private DockerEngineApi sut;

  @Test
  void containerInspect_sendsGet() {
    // Given
    when(mockHttpClientWrapper.getJson(any(), any()))
        .thenReturn(Optional.of(testContainerInspect()));

    // When
    Optional<ContainerInspect> result = sut.containerInspect("my_container");

    // Then
    assertThat(result).hasValue(testContainerInspect());
    verify(mockHttpClientWrapper)
        .getJson(
            "http://host.docker.internal:2375/containers/my_container/json",
            ContainerInspect.class);
  }

  @Test
  void volumeInspect_sendsGet() {
    // Given
    when(mockHttpClientWrapper.getJson(any(), any())).thenReturn(Optional.of(testVolumeInspect()));

    // When
    Optional<VolumeInspect> result = sut.volumeInspect("my_volume");

    // Then
    assertThat(result).hasValue(testVolumeInspect());
    verify(mockHttpClientWrapper)
        .getJson("http://host.docker.internal:2375/volumes/my_volume", VolumeInspect.class);
  }

  private ContainerInspect testContainerInspect() {
    return new ContainerInspect(
        List.of(new ContainerInspect.Mount("bind", "host/source", "container/dest")));
  }

  private VolumeInspect testVolumeInspect() {
    return new VolumeInspect(new VolumeInspect.Options("my_device"));
  }
}
