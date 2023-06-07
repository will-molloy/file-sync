package com.willmolloy.backup.statistics.observers.discord;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Discord API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class DiscordApi {

  private static final Logger log = LogManager.getLogger();

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  private final Gson gson = new Gson();

  /**
   * Execute Webhook.
   *
   * @see <a href=https://discord.com/developers/docs/resources/webhook#execute-webhook>API doc</a>
   */
  void executeWebhook(URI webhookUri, WebhookBody body) {
    try {
      String jsonBody = gson.toJson(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(webhookUri)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status != 204) {
        log.error("Unexpected status executing discord webhook: ({} {})", status, response.body());
      }
    } catch (RuntimeException | IOException | InterruptedException e) {
      log.error("Error executing discord webhook", e);
    }
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
