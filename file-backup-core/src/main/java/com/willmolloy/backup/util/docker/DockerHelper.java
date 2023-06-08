package com.willmolloy.backup.util.docker;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.willmolloy.backup.util.EnvHelper.getRequiredEnvVariable;

import com.google.common.annotations.VisibleForTesting;
import com.willmolloy.backup.util.docker.DockerEngineApi.ContainerInspect.Mount;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private final Optional<String> hostName;
  private final DockerEngineApi api;

  @VisibleForTesting
  DockerHelper(Optional<String> hostName, DockerEngineApi api) {
    this.hostName = checkNotNull(hostName);
    this.api = checkNotNull(api);
  }

  public DockerHelper() {
    this(
        isRunningInDocker() ? Optional.of(getRequiredEnvVariable("HOSTNAME")) : Optional.empty(),
        new DockerEngineApi());
  }

  /** {@code true} if running in docker container. */
  public static boolean isRunningInDocker() {
    return Files.exists(Path.of("/.dockerenv"));
  }

  /** Gets the corresponding host path for the mount/volume. */
  public Optional<String> getHostPath(String containerPath) {
    log.debug("getHostPath({})", containerPath);
    return hostName
        .flatMap(api::containerInspect)
        .flatMap(
            containerInspect ->
                containerInspect.Mounts().stream()
                    .filter(mount -> mount.Destination().equals(containerPath))
                    .findFirst())
        .flatMap(this::extractHostPath);
  }

  private Optional<String> extractHostPath(Mount mount) {
    return switch (mount.Type()) {
      case "bind" -> Optional.of(mount.Source());
      case "volume" -> {
        Pattern p = Pattern.compile("^/var/lib/docker/volumes/(.*)/_data$");
        Matcher m = p.matcher(mount.Source());
        if (m.matches()) {
          String volume = m.group(1);
          // TODO hostname from IP addr
          yield api.volumeInspect(volume).map(volumeInspect -> volumeInspect.Options().device());
        } else {
          log.error("Volume [{}] doesn't match pattern: {}", mount.Source(), p);
          yield Optional.empty();
        }
      }
      default -> {
        log.error("Unknown mount type: {}", mount.Type());
        yield Optional.empty();
      }
    };
  }
}
