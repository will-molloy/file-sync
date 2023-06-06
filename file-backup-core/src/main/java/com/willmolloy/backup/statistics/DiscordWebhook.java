package com.willmolloy.backup.statistics;

import com.google.gson.Gson;
import com.willmolloy.backup.Backup;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link BackupObserver} which sends notifications to Discord.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class DiscordWebhook implements BackupObserver {
  private static final Logger log = LogManager.getLogger();

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  private final Gson gson = new Gson();

  private final URI webhook;

  public DiscordWebhook(String webhookUrl) {
    log.info("DiscordWebhook({})", webhookUrl);
    try {
      this.webhook = new URI(webhookUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void notifyStarted(Backup<?, ?> backup) {
    Body body =
        new Body(
            List.of(
                new EmbedObject(
                    "Backup Started",
                    null,
                    colorCode(88, 185, 255),
                    new Author(
                        "file-backup",
                        "https://w7.pngwing.com/pngs/496/218/png-transparent-computer-icons-backup-data-backup-icon-text-trademark-logo.png"),
                    List.of(
                        new Field("Source", backup.source().toString()),
                        new Field("Destination", backup.destination().toString())),
                    // TODO loading icon
                    null,
                    Instant.now().toString())));
    send(body);
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {}

  // TODO post as reply or (even better) start a thread - not possible via webhook atm?
  //  a fully fledged bot is a bit overkill
  @Override
  public void notifyFinished(Backup<?, ?> backup, Statistics.Snapshot stats, Duration elapsed) {
    Body body;
    if (stats.allSuccess()) {
      body =
          new Body(
              List.of(
                  new EmbedObject(
                      "Backup Finished in: %s".formatted(formatDuration(elapsed)),
                      "%s files created, %s files updated, %s files deleted, %s files same.\n\n%sMB added, %sMB removed."
                          .formatted(
                              NUMBER_FORMAT.format(stats.creates()),
                              NUMBER_FORMAT.format(stats.updates()),
                              NUMBER_FORMAT.format(stats.deletes()),
                              NUMBER_FORMAT.format(stats.same()),
                              NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
                              NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA)),
                      colorCode(0, 153, 0),
                      new Author(
                          "file-backup",
                          "https://w7.pngwing.com/pngs/496/218/png-transparent-computer-icons-backup-data-backup-icon-text-trademark-logo.png"),
                      List.of(
                          new Field("Source", backup.source().toString()),
                          new Field("Destination", backup.destination().toString())),
                      new Thumbnail(
                          "https://craftassets.unraid.net/uploads/discord/notify-normal.png"),
                      Instant.now().toString())));
    } else {
      body =
          new Body(
              List.of(
                  new EmbedObject(
                      "Backup Finished in: %s".formatted(formatDuration(elapsed)),
                      "%s files created, %s files updated, %s files deleted, %s files same.\n\n%sMB added, %sMB removed.\n\nFailed: %s creates, %s updates, %s deletes."
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
                      colorCode(255, 140, 47),
                      new Author(
                          "file-backup",
                          "https://w7.pngwing.com/pngs/496/218/png-transparent-computer-icons-backup-data-backup-icon-text-trademark-logo.png"),
                      List.of(
                          new Field("Source", backup.source().toString()),
                          new Field("Destination", backup.destination().toString())),
                      new Thumbnail(
                          "https://craftassets.unraid.net/uploads/discord/notify-warning.png"),
                      Instant.now().toString())));
    }
    send(body);
  }

  @Override
  public void notifyFailed(Backup<?, ?> backup, Throwable t) {
    Body body =
        new Body(
            List.of(
                new EmbedObject(
                    "Backup Failed",
                    t.toString(),
                    colorCode(226, 40, 40),
                    new Author(
                        "file-backup",
                        "https://w7.pngwing.com/pngs/496/218/png-transparent-computer-icons-backup-data-backup-icon-text-trademark-logo.png"),
                    List.of(
                        new Field("Source", backup.source().toString()),
                        new Field("Destination", backup.destination().toString())),
                    new Thumbnail(
                        "https://craftassets.unraid.net/uploads/discord/notify-alert.png"),
                    Instant.now().toString())));
    send(body);
  }

  private void send(Body body) {
    try {
      String json = gson.toJson(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(webhook)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();
      log.info("request({})", request);
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (!(status >= 200 && status < 300)) {
        log.error("Unsuccessful status: {} {}", status, response.body());
      }
    } catch (IOException e) {
      log.error("Error", e);
    } catch (InterruptedException e) {
      log.error("Error", e);
      Thread.currentThread().interrupt();
    }
  }

  private int colorCode(int r, int g, int b) {
    return (((r << 8) + g) << 8) + b;
  }

  private String formatDuration(Duration duration) {
    long s = duration.toSeconds();
    return "%d:%02d:%02d".formatted(s / 3600, (s % 3600) / 60, (s % 60));
  }

  private record Body(List<EmbedObject> embeds) {}

  private record EmbedObject(
      String title,
      String description,
      int color,
      Author author,
      List<Field> fields,
      Thumbnail thumbnail,
      String timestamp) {}

  private record Author(String name, String icon_url) {}

  private record Field(String name, String value) {}

  private record Thumbnail(String url) {}
}
