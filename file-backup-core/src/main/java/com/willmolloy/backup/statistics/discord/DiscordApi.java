package com.willmolloy.backup.statistics.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import com.willmolloy.backup.util.HttpClientWrapper;
import java.net.URI;
import java.util.List;

/**
 * Discord API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class DiscordApi {

  private final HttpClientWrapper httpClientWrapper;

  DiscordApi(HttpClientWrapper httpClientWrapper) {
    this.httpClientWrapper = checkNotNull(httpClientWrapper);
  }

  /**
   * Execute Webhook.
   *
   * @see <a href=https://discord.com/developers/docs/resources/webhook#execute-webhook>API doc</a>
   */
  void executeWebhook(URI webhookUrl, WebhookBody body) {
    httpClientWrapper.postJson(webhookUrl, body);
  }

  /**
   * Webhook body.
   *
   * @param embeds embedded rich content
   */
  record WebhookBody(List<EmbedObject> embeds) {

    /**
     * Embed object.
     *
     * @param title title
     * @param description description
     * @param color color code
     * @param fields fields
     * @param thumbnail thumbnail
     * @param timestamp ISO8601 timestamp
     */
    record EmbedObject(
        String title,
        String description,
        int color,
        List<Field> fields,
        Thumbnail thumbnail,
        String timestamp) {

      /**
       * Embed field.
       *
       * @param name name
       * @param value value
       */
      record Field(String name, String value) {}

      /**
       * Embed thumbnail.
       *
       * @param url image url
       */
      record Thumbnail(String url) {}
    }
  }
}
