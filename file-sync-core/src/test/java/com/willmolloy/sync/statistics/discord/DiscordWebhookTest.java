package com.willmolloy.sync.statistics.discord;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.willmolloy.sync.Backup;
import com.willmolloy.sync.File;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.statistics.Statistics;
import com.willmolloy.sync.statistics.discord.DiscordWebhookApi.WebhookBody;
import com.willmolloy.sync.statistics.discord.DiscordWebhookApi.WebhookBody.EmbedObject;
import feign.form.FormData;
import java.io.IOException;
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

  @Mock private DiscordWebhookApi mockApi;
  private Instant fixedInstant;

  private DiscordWebhook sut;

  @BeforeEach
  void setUp() {
    fixedInstant = Instant.now();
    sut = new DiscordWebhook(mockApi, Clock.fixed(fixedInstant, ZoneId.systemDefault()));
  }

  @Test
  void notifyStarted_executesWebhook() throws IOException {
    // Given
    TestBackup backup = new TestBackup(new TestLocation("/source"), new TestLocation("/dest"));

    // When
    sut.notifyStarted(backup);

    // Then
    verify(mockApi)
        .executeWebhook(
            new Gson()
                .toJson(
                    new WebhookBody(
                        List.of(
                            new EmbedObject(
                                "Backup Started",
                                null,
                                3239167,
                                List.of(
                                    new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                                    new EmbedObject.Field(
                                        "Destination", "TestLocation[name=/dest]")),
                                new EmbedObject.Thumbnail("attachment://sync.png"),
                                fixedInstant.toString())))),
            new FormData(
                "image/png",
                "sync.png",
                Resources.toByteArray(Resources.getResource("icons/sync.png"))));
  }

  @Test
  void notifyScanned_noop() {
    // When
    sut.notifyScanned(null, null, null);

    // Then
    verifyNoInteractions(mockApi);
  }

  @Test
  void notifyFinished_executesWebhook() throws IOException {
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
            new Gson()
                .toJson(
                    new WebhookBody(
                        List.of(
                            new EmbedObject(
                                "Backup Finished in: 34:17:36",
                                "1,000 files created, 2,000 files updated, 3,000 files deleted,\n10,000 files same.\n\n10MB added, 20MB removed.",
                                39168,
                                List.of(
                                    new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                                    new EmbedObject.Field(
                                        "Destination", "TestLocation[name=/dest]")),
                                new EmbedObject.Thumbnail("attachment://ok.png"),
                                fixedInstant.toString())))),
            new FormData(
                "image/png",
                "ok.png",
                Resources.toByteArray(Resources.getResource("icons/ok.png"))));
  }

  @Test
  void notifyFinished_whenErrors_executesWebhook_withWarning() throws IOException {
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
            new Gson()
                .toJson(
                    new WebhookBody(
                        List.of(
                            new EmbedObject(
                                "Backup Finished in: 181:45:21",
                                "1,000 files created, 2,000 files updated, 3,000 files deleted,\n10,000 files same.\n\n10MB added, 20MB removed.\n\nFailed: 4,000 creates, 5,000 updates, 6,000 deletes.",
                                16747567,
                                List.of(
                                    new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                                    new EmbedObject.Field(
                                        "Destination", "TestLocation[name=/dest]")),
                                new EmbedObject.Thumbnail("attachment://warn.png"),
                                fixedInstant.toString())))),
            new FormData(
                "image/png",
                "warn.png",
                Resources.toByteArray(Resources.getResource("icons/warn.png"))));
  }

  @Test
  void notifyFailed_executesWebhook() throws IOException {
    // Given
    TestBackup backup = new TestBackup(new TestLocation("/source"), new TestLocation("/dest"));
    Throwable t = new OutOfMemoryError("OOM!");

    // When
    sut.notifyFailed(backup, t);

    // Then
    verify(mockApi)
        .executeWebhook(
            new Gson()
                .toJson(
                    new WebhookBody(
                        List.of(
                            new EmbedObject(
                                "Backup Failed",
                                "java.lang.OutOfMemoryError: OOM!",
                                14821416,
                                List.of(
                                    new EmbedObject.Field("Source", "TestLocation[name=/source]"),
                                    new EmbedObject.Field(
                                        "Destination", "TestLocation[name=/dest]")),
                                new EmbedObject.Thumbnail("attachment://error.png"),
                                fixedInstant.toString())))),
            new FormData(
                "image/png",
                "error.png",
                Resources.toByteArray(Resources.getResource("icons/error.png"))));
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
