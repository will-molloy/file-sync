package com.willmolloy.backup.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wraps {@link HttpClient} with serialisation, error handling, logging, etc.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class HttpClientWrapper {

  private static final Logger log = LogManager.getLogger();

  private final HttpClient httpClient;
  private final Gson gson = new Gson();

  public HttpClientWrapper(HttpClient httpClient) {
    this.httpClient = checkNotNull(httpClient);
  }

  /** Sends GET request and deserialises the JSON result. */
  public <T> Optional<T> getJson(URI url, Class<T> classOfT) {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (!Range.closedOpen(200, 300).contains(status)) {
        log.error("Unsuccessful status sending GET: {} ({} {})", url, status, response.body());
        return Optional.empty();
      }
      return Optional.of(gson.fromJson(response.body(), classOfT));
    } catch (RuntimeException | IOException | InterruptedException e) {
      log.error("Error sending GET: {}", url, e);
      return Optional.empty();
    }
  }

  /** Sends POST request after serialising the given body to JSON. */
  public <T> void postJson(URI url, T body) {
    try {
      String jsonBody = gson.toJson(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(url)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (!Range.closedOpen(200, 300).contains(status)) {
        log.error("Unsuccessful status sending POST: {} ({} {})", url, status, response.body());
      }
    } catch (RuntimeException | IOException | InterruptedException e) {
      log.error("Error sending POST: {}", url, e);
    }
  }
}
