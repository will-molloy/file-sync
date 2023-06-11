package com.willmolloy.backup.util;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HttpClient with serialisation, error handling, logging, etc.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class HttpClientWrapper {

  private static final Logger log = LogManager.getLogger();

  // TODO unit tests...?
  // TODO retrofit?
  private final OkHttpClient client =
      new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(30)).build();
  private final Gson gson = new Gson();

  /** Sends GET request and deserialises the JSON response. */
  public <T> Optional<T> getJson(String url, Class<T> classOfT) {
    Request request = new Request.Builder().url(url).build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("Unsuccessful status sending GET: {} ({})", url, response.code());
        return Optional.empty();
      }
      return Optional.of(gson.fromJson(verifyNotNull(response.body()).charStream(), classOfT));
    } catch (IOException e) {
      log.error("Error sending GET: {}", url, e);
      return Optional.empty();
    }
  }

  /** Sends POST request with JSON and file (multipart request). */
  public <T> void postJsonAndFile(
      String url, String jsonName, T body, String fileName, byte[] fileBytes) {
    MultipartBody requestBody =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(jsonName, gson.toJson(body))
            .addFormDataPart("file", fileName, RequestBody.create(fileBytes))
            .build();

    Request request = new Request.Builder().url(url).post(requestBody).build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("Unsuccessful status sending POST: {} ({})", url, response.code());
      }
    } catch (IOException e) {
      log.error("Error sending POST: {}", url, e);
    }
  }
}
