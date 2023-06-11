package com.willmolloy.backup.statistics.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import com.willmolloy.backup.util.HttpClientWrapper;
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
   * @see <a href=https://birdie0.github.io/discord-webhooks-guide/structure/file.html>Discord
   *     Webhooks Guide - files</a>
   */
  void executeWebhook(String webhookUrl, WebhookBody body, String fileName, byte[] fileBytes) {
    httpClientWrapper.postJsonAndFile(webhookUrl, "payload_json", body, fileName, fileBytes);
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
