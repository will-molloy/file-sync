package com.willmolloy.sync.statistics.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.Sync;
import com.willmolloy.sync.statistics.Statistics;
import com.willmolloy.sync.statistics.SyncObserver;
import com.willmolloy.sync.statistics.discord.DiscordWebhookApi.WebhookBody;
import com.willmolloy.sync.statistics.discord.DiscordWebhookApi.WebhookBody.EmbedObject;
import feign.form.FormData;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link SyncObserver} which sends notifications to Discord via webhook.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class DiscordWebhook implements SyncObserver {

  private static final Logger log = LogManager.getLogger();

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  private final DiscordWebhookApi api;
  private final Clock clock;

  @VisibleForTesting
  DiscordWebhook(DiscordWebhookApi api, Clock clock) {
    this.api = checkNotNull(api);
    this.clock = checkNotNull(clock);
  }

  public DiscordWebhook(String webhookUrl) {
    this(DiscordWebhookApi.create(webhookUrl), Clock.systemDefaultZone());
  }

  @Override
  public void notifyStarted(Sync<?, ?> sync) {
    webhook(sync, "Sync Started", null, "#316CFF", "sync.png");
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {}

  // TODO post as reply or (even better) start a thread - not possible via webhook atm?
  //  https://github.com/discord/discord-api-docs/discussions/3282
  @Override
  public void notifyFinished(Sync<?, ?> sync, Statistics.Snapshot stats, Duration elapsed) {
    if (!stats.anyErrors()) {
      webhook(
          sync,
          "Sync Finished in: %s".formatted(formatDuration(elapsed)),
          "%s files created, %s files updated, %s files deleted,\n%s files same.\n\n%sMB added, %sMB removed."
              .formatted(
                  NUMBER_FORMAT.format(stats.creates()),
                  NUMBER_FORMAT.format(stats.updates()),
                  NUMBER_FORMAT.format(stats.deletes()),
                  NUMBER_FORMAT.format(stats.same()),
                  NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
                  NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA)),
          "#009900",
          "ok.png");
    } else {
      webhook(
          sync,
          "Sync Finished in: %s".formatted(formatDuration(elapsed)),
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
          "warn.png");
    }
  }

  @Override
  public void notifyFailed(Sync<?, ?> sync, Throwable t) {
    webhook(sync, "Sync Failed", t.toString(), "#E22828", "error.png");
  }

  private void webhook(
      Sync<?, ?> sync, String title, String description, String color, String iconName) {
    WebhookBody webhookBody =
        new WebhookBody(
            List.of(
                new EmbedObject(
                    title,
                    description,
                    Integer.decode(color),
                    List.of(
                        new EmbedObject.Field("Source", sync.source().toString()),
                        new EmbedObject.Field("Destination", sync.destination().toString())),
                    new EmbedObject.Thumbnail("attachment://" + iconName),
                    Instant.now(clock).toString())));
    api.executeWebhook(new Gson().toJson(webhookBody), iconFile(iconName));
  }

  private FormData iconFile(String iconName) {
    try {
      // read directly to byte[], can't deal with File within jar
      // https://stackoverflow.com/a/20389418/6122976
      byte[] bytes = Resources.toByteArray(Resources.getResource("icons/" + iconName));
      return new FormData("image/png", iconName, bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String formatDuration(Duration duration) {
    long s = duration.toSeconds();
    return "%d:%02d:%02d".formatted(s / 3600, s % 3600 / 60, s % 60);
  }
}
