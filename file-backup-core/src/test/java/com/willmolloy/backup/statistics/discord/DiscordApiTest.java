package com.willmolloy.backup.statistics.discord;

import static org.mockito.Mockito.verify;

import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody.EmbedObject;
import com.willmolloy.backup.util.HttpClientWrapper;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DiscordApiTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class DiscordApiTest {

  @Mock private HttpClientWrapper mockHttpClientWrapper;
  @InjectMocks private DiscordApi sut;

  @Test
  void executeWebhook_sendsPost() {
    // When
    sut.executeWebhook("https://discord.com/api/webhooks/test", testBody(), "", null);

    // Then
    verify(mockHttpClientWrapper)
        .postJsonAndFile(
            "https://discord.com/api/webhooks/test", "payload_json", testBody(), "", null);
  }

  private WebhookBody testBody() {
    return new WebhookBody(
        List.of(new EmbedObject("Title", "Desc", Color.RED.getRed(), List.of(), null, null)));
  }
}
