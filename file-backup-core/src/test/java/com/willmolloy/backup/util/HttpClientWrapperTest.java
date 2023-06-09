package com.willmolloy.backup.util;

import static com.google.common.truth.Truth8.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.pgssoft.httpclient.HttpClientMock;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HttpClientWrapperTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class HttpClientWrapperTest {

  @Spy private HttpClientMock httpClientMock = new HttpClientMock();
  @InjectMocks private HttpClientWrapper sut;

  @Test
  void getJson_sendsGet_andDeserResult() {
    // Given
    httpClientMock.onGet().doReturn(200, testObjectJson());

    // When
    Optional<TestObject> result = sut.getJson(URI.create("https://localhost"), TestObject.class);

    // Then
    assertThat(result).hasValue(testObject());
    httpClientMock.verify().get("https://localhost").called();
  }

  @Test
  void getJson_whenGarbageResult_failsGracefully() {
    // Given
    httpClientMock.onGet().doReturn(200, "garbage");

    // When
    Optional<TestObject> result = sut.getJson(URI.create("https://localhost"), TestObject.class);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void getJson_whenUnsuccessfulStatusCode_failsGracefully() {
    // Given
    httpClientMock.onGet().doReturnStatus(500);

    // When
    Optional<TestObject> result = sut.getJson(URI.create("https://localhost"), TestObject.class);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void getJson_whenExceptionThrown_failsGracefully() {
    // Given
    httpClientMock.onGet().doThrowException(new IOException());

    // When
    Optional<TestObject> result = sut.getJson(URI.create("https://localhost"), TestObject.class);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void postJson_serBody_and_sendsPost() {
    // Given
    httpClientMock.onPost().doReturnStatus(204);

    // When
    sut.postJson(URI.create("https://discord.com/api/webhooks/test"), testObject());

    // Then
    httpClientMock
        .verify()
        .post("https://discord.com/api/webhooks/test")
        .withHeader("Content-Type", "application/json")
        .withBody(equalTo(testObjectJson()))
        .called();
  }

  @Test
  void postJson_whenUnsuccessfulStatusCode_failsGracefully() {
    // Given
    httpClientMock.onPost().doReturnStatus(500);

    // When
    assertDoesNotThrow(
        () -> sut.postJson(URI.create("https://discord.com/api/webhooks/test"), testObject()));
  }

  @Test
  void postJson_whenExceptionThrown_failsGracefully() {
    // Given
    httpClientMock.onPost().doThrowException(new IOException());

    // When
    assertDoesNotThrow(
        () -> sut.postJson(URI.create("https://discord.com/api/webhooks/test"), testObject()));
  }

  private TestObject testObject() {
    return new TestObject("abcd", 1234, List.of());
  }

  private String testObjectJson() {
    return "{\"string\":\"abcd\",\"integer\":1234,\"list\":[]}";
  }

  private record TestObject(String string, Integer integer, List<TestObject> list) {}
}
