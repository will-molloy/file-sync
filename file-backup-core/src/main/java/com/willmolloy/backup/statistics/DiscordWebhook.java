package com.willmolloy.backup.statistics;

import static com.willmolloy.backup.util.Preconditions.require;

import com.google.gson.Gson;
import com.willmolloy.backup.BaseBackup;
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
  public void notifyStarted(BaseBackup<?, ?> backup) {
    String msg = "Started: %s".formatted(backup);
    sendContent(msg);
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {
    String msg =
        "Scanned: %s in: %s. %s files. %sMB"
            .formatted(
                location,
                elapsed,
                NUMBER_FORMAT.format(fileTree.leafCount()),
                NUMBER_FORMAT.format(fileTree.totalSize() / MEGA));
    sendContent(msg);
  }

  @Override
  public void notifyFinished(BaseBackup<?, ?> backup, Statistics.Snapshot stats, Duration elapsed) {
    String msg =
        "Finished: %s in: %s. %s files created, %s files updated, %s files deleted, %s files same. %sMB added, %sMB removed"
            .formatted(
                backup,
                elapsed,
                NUMBER_FORMAT.format(stats.creates()),
                NUMBER_FORMAT.format(stats.updates()),
                NUMBER_FORMAT.format(stats.deletes()),
                NUMBER_FORMAT.format(stats.same()),
                NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
                NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA));
    sendContent(msg);

    if (!stats.allSuccess()) {
      String warningMsg =
          "Failed: %s creates, %s updates, %s deletes"
              .formatted(
                  NUMBER_FORMAT.format(stats.failedCreates()),
                  NUMBER_FORMAT.format(stats.failedUpdates()),
                  NUMBER_FORMAT.format(stats.failedDeletes()));
      sendContent(warningMsg);
    }
  }

  private void sendContent(String msg) {
    try {
      String json = gson.toJson(new Body(msg));
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
      require(status == 200 || status == 204, "Status was: %s".formatted(status));
    } catch (IOException e) {
      log.error("Error", e);
    } catch (InterruptedException e) {
      log.error("Error", e);
      Thread.currentThread().interrupt();
    }
  }

  private record Body(String content) {}
}
