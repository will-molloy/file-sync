package com.willmolloy.backup.statistics.discord;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.File;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import com.willmolloy.backup.statistics.Statistics;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody;
import com.willmolloy.backup.statistics.discord.DiscordApi.WebhookBody.EmbedObject;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DiscordWebhookTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class DiscordWebhookTest {

  private String webhookUrl;
  @Mock private DiscordApi mockApi;
  private Instant fixedInstant;

  private DiscordWebhook sut;

  @BeforeEach
  void setUp() {
    webhookUrl = "https://discord.com/api/webhooks/test";
    fixedInstant = Instant.now();
    sut =
        new DiscordWebhook(webhookUrl, mockApi, Clock.fixed(fixedInstant, ZoneId.systemDefault()));
  }

  @Test
  void notifyStarted_executesWebhook() {
    // Given
    TestBackup backup = new TestBackup(new TestLocation("/source"), new TestLocation("/dest"));

    // When
    sut.notifyStarted(backup);

    // Then
    verify(mockApi)
        .executeWebhook(
            URI.create(webhookUrl),
            new WebhookBody(
                List.of(
                    new EmbedObject(
                        "Backup Started",
                        null,
                        5814783,
                        List.of(
                            new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                            new EmbedObject.Field("Destination", "TestLocation[name=/dest]")),
                        null,
                        fixedInstant.toString()))));
  }

  @Test
  void notifyScanned_noop() {
    // When
    sut.notifyScanned(null, null, null);

    // Then
    verifyNoInteractions(mockApi);
  }

  @Test
  void notifyFinished_executesWebhook() {
    // Given
    TestBackup backup = new TestBackup(new TestLocation("/source"), new TestLocation("/dest"));

    // When
    sut.notifyFinished(
        backup,
        new Statistics.Snapshot(1000, 2000, 3000, 10_000, 0, 0, 0, 10_000_000, 20_000_000),
        Duration.ofSeconds(123456));

    // Then
    verify(mockApi)
        .executeWebhook(
            URI.create(webhookUrl),
            new WebhookBody(
                List.of(
                    new EmbedObject(
                        "Backup Finished in: 34:17:36",
                        "1,000 files created, 2,000 files updated, 3,000 files deleted,\n10,000 files same.\n\n10MB added, 20MB removed.",
                        39168,
                        List.of(
                            new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                            new EmbedObject.Field("Destination", "TestLocation[name=/dest]")),
                        new EmbedObject.Thumbnail(
                            "https://craftassets.unraid.net/uploads/discord/notify-normal.png"),
                        fixedInstant.toString()))));
  }

  @Test
  void notifyFinished_whenErrors_executesWebhook_withWarning() {
    // Given
    TestBackup backup = new TestBackup(new TestLocation("/source"), new TestLocation("/dest"));

    // When
    sut.notifyFinished(
        backup,
        new Statistics.Snapshot(1000, 2000, 3000, 10_000, 4000, 5000, 6000, 10_000_000, 20_000_000),
        Duration.ofSeconds(654321));

    // Then
    verify(mockApi)
        .executeWebhook(
            URI.create(webhookUrl),
            new WebhookBody(
                List.of(
                    new EmbedObject(
                        "Backup Finished in: 181:45:21",
                        "1,000 files created, 2,000 files updated, 3,000 files deleted,\n10,000 files same.\n\n10MB added, 20MB removed.\n\nFailed: 4,000 creates, 5,000 updates, 6,000 deletes.",
                        16747567,
                        List.of(
                            new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                            new EmbedObject.Field("Destination", "TestLocation[name=/dest]")),
                        new EmbedObject.Thumbnail(
                            "https://craftassets.unraid.net/uploads/discord/notify-warning.png"),
                        fixedInstant.toString()))));
  }

  @Test
  void notifyFailed_executesWebhook() {
    // Given
    TestBackup backup = new TestBackup(new TestLocation("/source"), new TestLocation("/dest"));
    Throwable t = new OutOfMemoryError("OOM!");

    // When
    sut.notifyFailed(backup, t);

    // Then
    verify(mockApi)
        .executeWebhook(
            URI.create(webhookUrl),
            new WebhookBody(
                List.of(
                    new EmbedObject(
                        "Backup Failed",
                        "java.lang.OutOfMemoryError: OOM!",
                        14821416,
                        List.of(
                            new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                            new EmbedObject.Field("Destination", "TestLocation[name=/dest]")),
                        new EmbedObject.Thumbnail(
                            "https://craftassets.unraid.net/uploads/discord/notify-alert.png"),
                        fixedInstant.toString()))));
  }

  private record TestBackup(TestLocation source, TestLocation destination)
      implements Backup<File, File> {
    @Override
    public boolean run() {
      return false;
    }
  }

  private record TestLocation(String name) implements Location<File> {
    @Override
    public FileTree<File> scan() {
      return null;
    }
  }
}
