package com.willmolloy.backup.statistics.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import com.willmolloy.backup.Backup;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import com.willmolloy.backup.statistics.BackupObserver;
import com.willmolloy.backup.statistics.Statistics;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody.EmbedObject;
import com.willmolloy.backup.util.HttpClientWrapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link BackupObserver} which sends notifications to Discord via webhook.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class DiscordWebhook implements BackupObserver {

  private static final Logger log = LogManager.getLogger();

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  private final String webhookUrl;
  private final DiscordApi api;
  private final Clock clock;

  @VisibleForTesting
  DiscordWebhook(String webhookUrl, DiscordApi api, Clock clock) {
    this.webhookUrl = checkNotNull(webhookUrl);
    this.api = checkNotNull(api);
    this.clock = checkNotNull(clock);
  }

  public DiscordWebhook(String webhookUrl) {
    this(webhookUrl, new DiscordApi(new HttpClientWrapper()), Clock.systemDefaultZone());
  }

  @Override
  public void notifyStarted(Backup<?, ?> backup) {
    webhook(backup, "Backup Started", null, "#316CFF", "sync-44.png");
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {}

  // TODO post as reply or (even better) start a thread - not possible via webhook atm?
  //  https://github.com/discord/discord-api-docs/discussions/3282
  @Override
  public void notifyFinished(Backup<?, ?> backup, Statistics.Snapshot stats, Duration elapsed) {
    if (!stats.anyErrors()) {
      webhook(
          backup,
          "Backup Finished in: %s".formatted(formatDuration(elapsed)),
          "%s files created, %s files updated, %s files deleted,\n%s files same.\n\n%sMB added, %sMB removed."
              .formatted(
                  NUMBER_FORMAT.format(stats.creates()),
                  NUMBER_FORMAT.format(stats.updates()),
                  NUMBER_FORMAT.format(stats.deletes()),
                  NUMBER_FORMAT.format(stats.same()),
                  NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
                  NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA)),
          "#009900",
          "ok-48.png");
    } else {
      webhook(
          backup,
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
          "#FF8C2F",
          "warn-48.png");
    }
  }

  @Override
  public void notifyFailed(Backup<?, ?> backup, Throwable t) {
    webhook(backup, "Backup Failed", t.toString(), "#E22828", "error-48.png");
  }

  private void webhook(
      Backup<?, ?> backup, String title, String description, String color, String iconName) {
    try {
      WebhookBody webhookBody =
          new WebhookBody(
              List.of(
                  new EmbedObject(
                      title,
                      description,
                      Integer.decode(color),
                      List.of(
                          new EmbedObject.Field("Source", backup.source().toString()),
                          new EmbedObject.Field("Destination", backup.destination().toString())),
                      new EmbedObject.Thumbnail("attachment://" + iconName),
                      Instant.now(clock).toString())));
      api.executeWebhook(
          webhookUrl,
          webhookBody,
          iconName,
          // read directly to byte[], can't deal with File within jar
          // https://stackoverflow.com/a/20389418/6122976
          Resources.toByteArray(Resources.getResource("icons/" + iconName)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String formatDuration(Duration duration) {
    long s = duration.toSeconds();
    return "%d:%02d:%02d".formatted(s / 3600, s % 3600 / 60, s % 60);
  }
}
