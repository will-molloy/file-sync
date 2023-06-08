package com.willmolloy.backup.statistics.discord;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.google.gson.Gson;
import com.pgssoft.httpclient.HttpClientMock;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody.EmbedObject;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DiscordApiTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class DiscordApiTest {

  private String webhookUrl = "https://discord.com/api/webhooks/test";
  @Spy private HttpClientMock httpClientMock = new HttpClientMock();
  @InjectMocks private DiscordApi sut;

  @Test
  void executeWebhook_sendsPost() throws URISyntaxException {
    // Given
    httpClientMock.onPost().doReturnStatus(204);

    // When
    sut.executeWebhook(new URI(webhookUrl), testBody());

    // Then
    httpClientMock
        .verify()
        .post(webhookUrl)
        .withHeader("Content-Type", "application/json")
        .withBody(equalTo(new Gson().toJson(testBody())))
        .called();
  }

  @Test
  void executeWebhook_whenUnexpectedStatusCode_failsGracefully() {
    // Given
    httpClientMock.onPost().doReturnStatus(500);

    // When
    assertDoesNotThrow(() -> sut.executeWebhook(new URI(webhookUrl), testBody()));
  }

  @Test
  void executeWebhook_whenExceptionThrown_failsGracefully() {
    // Given
    httpClientMock.onPost().doThrowException(new IOException());

    // When
    assertDoesNotThrow(() -> sut.executeWebhook(new URI(webhookUrl), testBody()));
  }

  private WebhookBody testBody() {
    return new WebhookBody(
        List.of(new EmbedObject("Title", "Desc", Color.RED.getRed(), List.of(), null, null)));
  }
}
