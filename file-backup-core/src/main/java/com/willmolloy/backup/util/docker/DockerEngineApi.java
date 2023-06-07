package com.willmolloy.backup.util.docker;

import static com.willmolloy.backup.util.EnvHelper.readRequiredEnvVariable;

import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Docker Engine API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class DockerEngineApi {

  private static final Logger log = LogManager.getLogger();

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  private final Gson gson = new Gson();

  Optional<ContainerInspect> containerInspect() {
    // https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerInspect
    String hostname = readRequiredEnvVariable("HOSTNAME");
    return getAndDeser(
        "http://host.docker.internal:2375/containers/%s/json".formatted(hostname),
        ContainerInspect.class);
  }

  /**
   * Container inspect result.
   *
   * @param Mounts container mounts
   */
  @SuppressFBWarnings(
      value = "NM_METHOD_NAMING_CONVENTION",
      justification = "Docker API uses uppercase...")
  record ContainerInspect(List<Mount> Mounts) {
    /**
     * Container mounts.
     *
     * @param Type mount type
     * @param Source host path or volume name
     * @param Destination container path
     */
    record Mount(String Type, String Source, String Destination) {}
  }

  Optional<VolumeInspect> volumeInspect(String volume) {
    // https://docs.docker.com/engine/api/v1.43/#tag/Volume/operation/VolumeInspect
    return getAndDeser(
        "http://host.docker.internal:2375/volumes/%s".formatted(volume), VolumeInspect.class);
  }

  /**
   * Volume inspect result.
   *
   * @param Options volume options
   */
  @SuppressFBWarnings(
      value = "NM_METHOD_NAMING_CONVENTION",
      justification = "Docker API uses uppercase...")
  record VolumeInspect(Options Options) {
    /**
     * Volume options.
     *
     * @param device host path
     */
    record Options(String device) {}
  }

  private <T> Optional<T> getAndDeser(String url, Class<T> classOfT) {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status != 200) {
        log.error(
            "Unsuccessful status sending GET request: {} ({} {})", url, status, response.body());
        return Optional.empty();
      }
      return Optional.of(gson.fromJson(response.body(), classOfT));
    } catch (RuntimeException | IOException | InterruptedException | URISyntaxException e) {
      log.error("Error sending GET request: {}", url, e);
      return Optional.empty();
    }
  }
}
