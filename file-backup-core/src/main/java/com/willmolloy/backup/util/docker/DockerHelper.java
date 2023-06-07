package com.willmolloy.backup.util.docker;

import static com.willmolloy.backup.util.Preconditions.require;

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
  // TODO unit tests!

  private static final Logger log = LogManager.getLogger();

  private static final DockerEngineApi API = new DockerEngineApi();

  /** {@code true} if running in docker container. */
  public static boolean isRunningInDocker() {
    return Files.exists(Path.of("/.dockerenv"));
  }

  /** Gets the corresponding host path for the mount/volume. */
  public static Optional<String> getHostPath(String containerPath) {
    log.debug("getHostPath({})", containerPath);
    return API.containerInspect()
        .flatMap(
            containerInspect ->
                containerInspect.Mounts().stream()
                    .filter(mount -> mount.Destination().equals(containerPath))
                    .findFirst())
        .flatMap(mount -> extractHostPath(mount));
  }

  private static Optional<String> extractHostPath(Mount mount) {
    return switch (mount.Type()) {
      case "bind" -> Optional.of(mount.Source());
      case "volume" -> {
        Pattern p = Pattern.compile("^/var/lib/docker/volumes/(.*)/_data$");
        Matcher m = p.matcher(mount.Source());
        require(m.matches(), "Doesn't match pattern: %s".formatted(p));
        String volume = m.group(1);
        // TODO hostname from IP addr
        yield API.volumeInspect(volume).map(volumeInspect -> volumeInspect.Options().device());
      }
      default -> Optional.empty();
    };
  }

  private DockerHelper() {}
}
