package com.willmolloy.backup.statistics.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.willmolloy.backup.Backup;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import com.willmolloy.backup.statistics.BackupObserver;
import com.willmolloy.backup.statistics.Statistics;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody.EmbedObject;
import com.willmolloy.backup.util.HttpClientWrapper;
import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * {@link BackupObserver} which sends notifications to Discord via webhook.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class DiscordWebhook implements BackupObserver {

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  private final URI webhookUrl;
  private final DiscordApi api;
  private final Clock clock;

  @VisibleForTesting
  DiscordWebhook(String webhookUrl, DiscordApi api, Clock clock) {
    this.webhookUrl = URI.create(webhookUrl);
    this.api = checkNotNull(api);
    this.clock = checkNotNull(clock);
  }

  public DiscordWebhook(String webhookUrl) {
    this(
        webhookUrl,
        new DiscordApi(
            new HttpClientWrapper(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build())),
        Clock.systemDefaultZone());
  }

  @Override
  public void notifyStarted(Backup<?, ?> backup) {
    WebhookBody body =
        new WebhookBody(
            List.of(
                new EmbedObject(
                    "Backup Started",
                    null,
                    colorCode(new Color(88, 185, 255)),
                    List.of(
                        new EmbedObject.Field("Source", backup.source().toString()),
                        new EmbedObject.Field("Destination", backup.destination().toString())),
                    // TODO loading icon
                    null,
                    Instant.now(clock).toString())));
    api.executeWebhook(webhookUrl, body);
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {}

  // TODO post as reply or (even better) start a thread - not possible via webhook atm?
  //  a fully fledged bot is a bit overkill
  @Override
  public void notifyFinished(Backup<?, ?> backup, Statistics.Snapshot stats, Duration elapsed) {
    WebhookBody body;
    if (!stats.anyErrors()) {
      body =
          new WebhookBody(
              List.of(
                  new EmbedObject(
                      "Backup Finished in: %s".formatted(formatDuration(elapsed)),
                      "%s files created, %s files updated, %s files deleted,\n%s files same.\n\n%sMB added, %sMB removed."
                          .formatted(
                              NUMBER_FORMAT.format(stats.creates()),
                              NUMBER_FORMAT.format(stats.updates()),
                              NUMBER_FORMAT.format(stats.deletes()),
                              NUMBER_FORMAT.format(stats.same()),
                              NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
                              NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA)),
                      colorCode(new Color(0, 153, 0)),
                      List.of(
                          new EmbedObject.Field("Source", backup.source().toString()),
                          new EmbedObject.Field("Destination", backup.destination().toString())),
                      new EmbedObject.Thumbnail(
                          "https://craftassets.unraid.net/uploads/discord/notify-normal.png"),
                      Instant.now(clock).toString())));
    } else {
      body =
          new WebhookBody(
              List.of(
                  new EmbedObject(
                      "Backup Finished in: %s".formatted(formatDuration(elapsed)),
                      "%s files created, %s files updated, %s files deleted,\n%s files same.\n\n%sMB added, %sMB removed.\n\nFailed: %s creates, %s updates, %s deletes."
                          .formatted(
                              NUMBER_FORMAT.format(stats.creates()),
                              NUMBER_FORMAT.format(stats.updates()),
                              NUMBER_FORMAT.format(stats.deletes()),
                              NUMBER_FORMAT.format(stats.same()),
                              NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
                              NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA),
                              NUMBER_FORMAT.format(stats.failedCreates()),
                              NUMBER_FORMAT.format(stats.failedUpdates()),
                              NUMBER_FORMAT.format(stats.failedDeletes())),
                      colorCode(new Color(255, 140, 47)),
                      List.of(
                          new EmbedObject.Field("Source", backup.source().toString()),
                          new EmbedObject.Field("Destination", backup.destination().toString())),
                      new EmbedObject.Thumbnail(
                          "https://craftassets.unraid.net/uploads/discord/notify-warning.png"),
                      Instant.now(clock).toString())));
    }
    api.executeWebhook(webhookUrl, body);
  }

  @Override
  public void notifyFailed(Backup<?, ?> backup, Throwable t) {
    WebhookBody body =
        new WebhookBody(
            List.of(
                new EmbedObject(
                    "Backup Failed",
                    t.toString(),
                    colorCode(new Color(226, 40, 40)),
                    List.of(
                        new EmbedObject.Field("Source", backup.source().toString()),
                        new EmbedObject.Field("Destination", backup.destination().toString())),
                    new EmbedObject.Thumbnail(
                        "https://craftassets.unraid.net/uploads/discord/notify-alert.png"),
                    Instant.now(clock).toString())));
    api.executeWebhook(webhookUrl, body);
  }

  private static int colorCode(Color color) {
    return (color.getRed() & 0xFF) << 16 | (color.getGreen() & 0xFF) << 8 | color.getBlue() & 0xFF;
  }

  private static String formatDuration(Duration duration) {
    long s = duration.toSeconds();
    return "%d:%02d:%02d".formatted(s / 3600, s % 3600 / 60, s % 60);
  }
}
