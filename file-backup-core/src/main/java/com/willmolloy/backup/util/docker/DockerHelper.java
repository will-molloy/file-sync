package com.willmolloy.backup.util.docker;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.willmolloy.backup.util.EnvHelper.getRequiredEnvVariable;

import com.google.common.annotations.VisibleForTesting;
import com.willmolloy.backup.util.HttpClientWrapper;
import com.willmolloy.backup.util.docker.DockerEngineApi.ContainerInspect.Mount;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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

  /** {@code true} if running in docker container. */
  public static boolean isRunningInDocker() {
    return Files.exists(Path.of("/.dockerenv"));
  }

  private final Optional<String> containerHostName;
  private final DockerEngineApi api;

  @VisibleForTesting
  DockerHelper(Optional<String> containerHostName, DockerEngineApi api) {
    this.containerHostName = checkNotNull(containerHostName);
    this.api = checkNotNull(api);
  }

  public DockerHelper() {
    this(
        isRunningInDocker() ? Optional.of(getRequiredEnvVariable("HOSTNAME")) : Optional.empty(),
        new DockerEngineApi(
            new HttpClientWrapper(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build())));
  }

  /** Gets the corresponding host path for the mount/volume. */
  public Optional<String> getHostPath(String containerPath) {
    log.debug("getHostPath({})", containerPath);
    return containerHostName
        .flatMap(api::containerInspect)
        .flatMap(
            containerInspect ->
                containerInspect.Mounts().stream()
                    .filter(mount -> mount.Destination().equals(containerPath))
                    .findFirst())
        .flatMap(this::extractHostPathFromMount);
  }

  private Optional<String> extractHostPathFromMount(Mount mount) {
    return switch (mount.Type()) {
      case "bind" -> Optional.of(mount.Source());
      case "volume" -> extractHostPathFromVolume(mount.Source());
      default -> {
        log.error("Unknown mount type: {}", mount.Type());
        yield Optional.empty();
      }
    };
  }

  private Optional<String> extractHostPathFromVolume(String volume) {
    Pattern p = Pattern.compile("^/var/lib/docker/volumes/(.*)/_data$");
    Matcher m = p.matcher(volume);
    if (m.matches()) {
      String volumeName = m.group(1);
      return api.volumeInspect(volumeName)
          .map(volumeInspect -> volumeInspect.Options().device())
          .map(this::tryMapIpAddress);
    } else {
      log.error("Volume [{}] doesn't match pattern: {}", volume, p);
      return Optional.empty();
    }
  }

  // most likely reason for volume over bind is a network drive (e.g. NAS)
  // this maps ip address to server name
  private String tryMapIpAddress(String device) {
    String bytePattern = "([2][5][0-5]|[2][0-4][0-9]|[1]?[0-9]{1,2})";
    String ipv4Pattern = "(" + bytePattern + "[.]){3}" + bytePattern;
    Pattern p = Pattern.compile("^//(" + ipv4Pattern + ")/(.*)$");
    Matcher m = p.matcher(device);
    if (m.matches()) {
      String ipAddr = m.group(1);
      String path = m.group(m.groupCount());
      try {
        InetAddress inetAddress = InetAddress.getByName(ipAddr);
        String host = inetAddress.getHostName();
        log.info("inetAddress={}", inetAddress);
        return "//%s/%s".formatted(host, path);
      } catch (UnknownHostException e) {
        log.warn("Unknown host: {}", ipAddr, e);
        return device;
      }
    } else {
      log.warn("Device [{}] doesn't match pattern: {}", device, p);
      return device;
    }
  }
}
