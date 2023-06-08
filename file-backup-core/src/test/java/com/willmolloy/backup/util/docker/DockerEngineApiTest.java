package com.willmolloy.backup.util.docker;

import static com.google.common.truth.Truth8.assertThat;

import com.google.gson.Gson;
import com.pgssoft.httpclient.HttpClientMock;
import com.willmolloy.backup.util.docker.DockerEngineApi.ContainerInspect;
import com.willmolloy.backup.util.docker.DockerEngineApi.VolumeInspect;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DockerEngineApiTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class DockerEngineApiTest {

  @Spy private HttpClientMock httpClientMock = new HttpClientMock();
  @InjectMocks private DockerEngineApi sut;

  @Test
  void containerInspect_sendsGet_andDeserResult() {
    // Given
    httpClientMock.onGet().doReturn(200, new Gson().toJson(testContainerInspect()));

    // When
    Optional<ContainerInspect> result = sut.containerInspect("my_container");

    // Then
    assertThat(result).hasValue(testContainerInspect());
    httpClientMock
        .verify()
        .get("http://host.docker.internal:2375/containers/my_container/json")
        .called();
  }

  @Test
  void containerInspect_whenGarbageResult_failsGracefully() {
    // Given
    httpClientMock.onGet().doReturn(200, "garbage");

    // When
    Optional<ContainerInspect> result = sut.containerInspect("my_container");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void containerInspect_whenUnexpectedStatusCode_failsGracefully() {
    // Given
    httpClientMock.onGet().doReturnStatus(500);

    // When
    Optional<ContainerInspect> result = sut.containerInspect("my_container");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void containerInspect_whenExceptionThrown_failsGracefully() {
    // Given
    httpClientMock.onGet().doThrowException(new IOException());

    // When
    Optional<ContainerInspect> result = sut.containerInspect("my_container");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void volumeInspect_sendsGet_andDeserResult() {
    // Given
    httpClientMock.onGet().doReturn(200, new Gson().toJson(testVolumeInspect()));

    // When
    Optional<VolumeInspect> result = sut.volumeInspect("my_volume");

    // Then
    assertThat(result).hasValue(testVolumeInspect());
    httpClientMock.verify().get("http://host.docker.internal:2375/volumes/my_volume").called();
  }

  @Test
  void volumeInspect_whenGarbageResult_failsGracefully() {
    // Given
    httpClientMock.onGet().doReturn(200, "garbage");

    // When
    Optional<VolumeInspect> result = sut.volumeInspect("my_volume");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void volumeInspect_whenUnexpectedStatusCode_failsGracefully() {
    // Given
    httpClientMock.onGet().doReturnStatus(500);

    // When
    Optional<VolumeInspect> result = sut.volumeInspect("my_volume");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void volumeInspect_whenExceptionThrown_failsGracefully() {
    // Given
    httpClientMock.onGet().doThrowException(new IOException());

    // When
    Optional<VolumeInspect> result = sut.volumeInspect("my_volume");

    // Then
    assertThat(result).isEmpty();
  }

  private ContainerInspect testContainerInspect() {
    return new ContainerInspect(
        List.of(new ContainerInspect.Mount("bind", "host/source", "container/dest")));
  }

  private VolumeInspect testVolumeInspect() {
    return new VolumeInspect(new VolumeInspect.Options("my_device"));
  }
}
