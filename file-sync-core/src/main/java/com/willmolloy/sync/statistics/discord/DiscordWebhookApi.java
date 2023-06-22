package com.willmolloy.sync.statistics.discord;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.form.FormData;
import feign.form.FormEncoder;
import feign.gson.GsonEncoder;
import java.util.List;

/**
 * Discord Webhook API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
interface DiscordWebhookApi {

  static DiscordWebhookApi create(String webhookUrl) {
    return Feign.builder()
        .encoder(new FormEncoder(new GsonEncoder()))
        .target(DiscordWebhookApi.class, webhookUrl);
  }

  /**
   * Execute Webhook.
   *
   * @see <a href=https://discord.com/developers/docs/resources/webhook#execute-webhook>API doc</a>
   * @see <a href=https://birdie0.github.io/discord-webhooks-guide/index.html>Discord Webhooks
   *     Guide</a>
   */
  @RequestLine("POST")
  @Headers("Content-Type: multipart/form-data")
  // TODO accept POJO... not sure why the JSON ser doesn't work
  //  https://github.com/OpenFeign/feign-form/issues/118
  void executeWebhook(@Param("payload_json") String jsonBody, @Param("file") FormData file);

  /**
   * Webhook body.
   *
   * @param embeds embedded rich content
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
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
