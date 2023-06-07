package com.willmolloy.backup.util;

import static com.willmolloy.backup.util.EnvHelper.readRequiredEnvVariable;
import static com.willmolloy.backup.util.Preconditions.require;

import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper methods (hacks) for when running via Docker container.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class DockerHelper {

  private static final Logger log = LogManager.getLogger();

  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  private static final Gson GSON = new Gson();

  /** {@code true} if running in docker container. */
  public static boolean isRunningInDocker() {
    return Files.exists(Path.of("/.dockerenv"));
  }

  /** Gets the corresponding host path for the mount/volume. */
  public static Optional<String> getHostPath(String dockerPath) {
    log.debug("getHostPath({})", dockerPath);
    return containerInspect()
        .flatMap(
            containerInspect ->
                containerInspect.Mounts().stream()
                    .filter(mount -> mount.Destination().equals(dockerPath))
                    .findFirst())
        .flatMap(mount -> extractHostPath(mount));
  }

  private static Optional<String> extractHostPath(ContainerInspect.Mount mount) {
    return switch (mount.Type()) {
      case "bind" -> Optional.of(mount.Source());
      case "volume" -> {
        Pattern p = Pattern.compile("^/var/lib/docker/volumes/(.*)/_data$");
        Matcher m = p.matcher(mount.Source());
        require(m.matches(), "Doesn't match pattern: %s".formatted(p));
        String volume = m.group(1);
        yield volumeInspect(volume).map(volumeInspect -> volumeInspect.Options().device());
      }
      default -> Optional.empty();
    };
  }

  private static Optional<ContainerInspect> containerInspect() {
    // https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerInspect
    String hostname = readRequiredEnvVariable("HOSTNAME");
    return getAndDeser(
        "http://host.docker.internal:2375/containers/%s/json".formatted(hostname),
        ContainerInspect.class);
  }

  private static Optional<VolumeInspect> volumeInspect(String volume) {
    // https://docs.docker.com/engine/api/v1.43/#tag/Volume/operation/VolumeInspect
    return getAndDeser(
        "http://host.docker.internal:2375/volumes/%s".formatted(volume), VolumeInspect.class);
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private static <T> Optional<T> getAndDeser(String getUrl, Class<T> classOfT) {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(getUrl)).GET().build();
      HttpResponse<String> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status != 200) {
        log.error(
            "Unsuccessful status sending GET request: {} ({} {})", getUrl, status, response.body());
        return Optional.empty();
      }
      return Optional.of(GSON.fromJson(response.body(), classOfT));
    } catch (Exception e) {
      log.error("Error sending GET request: {}", getUrl, e);
      return Optional.empty();
    }
  }

  @SuppressFBWarnings(
      value = "NM_METHOD_NAMING_CONVENTION",
      justification = "Docker API uses uppercase...")
  private record ContainerInspect(List<Mount> Mounts) {
    private record Mount(String Type, String Source, String Destination) {}
  }

  @SuppressFBWarnings(
      value = "NM_METHOD_NAMING_CONVENTION",
      justification = "Docker API uses uppercase...")
  private record VolumeInspect(Options Options) {
    private record Options(String device) {}
  }

  private DockerHelper() {}
}
