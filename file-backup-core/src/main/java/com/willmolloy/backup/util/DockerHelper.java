package com.willmolloy.backup.util;

import static com.willmolloy.backup.util.Preconditions.require;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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

  private static final Gson GSON = new Gson();

  public static boolean isRunningInsideDocker() {
    return Files.exists(Path.of("/.dockerenv"));
  }

  public static Optional<String> hostPath(String path) {
    log.debug("hostPath({})", path);
    try {
      Inspect inspect = dockerInspect();
      Inspect.Mount mount =
          Arrays.stream(inspect.Mounts())
              .filter(m -> m.Destination().equals(path))
              .findFirst()
              // TODO chaining options, no throw
              .orElseThrow();
      return Optional.of(extractHostPath(mount));
    } catch (Exception e) {
      log.error("Error", e);
      return Optional.empty();
    }
  }

  private static String extractHostPath(Inspect.Mount mount) throws IOException {
    return switch (mount.Type) {
      case "bind" -> {
        Pattern p = Pattern.compile("^/run/desktop/mnt/host(.*)$");
        Matcher m = p.matcher(mount.Source());
        require(m.matches());
        yield m.group(1);
      }
      case "volume" -> {
        Pattern p = Pattern.compile("^/var/lib/docker/volumes/(.*)/_data$");
        Matcher m = p.matcher(mount.Source());
        require(m.matches());
        String volume = m.group(1);
        InspectVolume inspectVolume = dockerInspectVolume(volume);
        yield inspectVolume.Options().device();
      }
      default -> throw new IllegalArgumentException();
    };
  }

  private static Inspect dockerInspect() throws IOException {
    String[] cmd = {"docker", "inspect", "005"};
    Inspect[] inspect = executeAndDeser(cmd, Inspect[].class);
    return inspect[0];
  }

  private static InspectVolume dockerInspectVolume(String volume) throws IOException {
    String[] cmd = {"docker", "inspect", "volume", volume};
    InspectVolume[] inspectVolume = executeAndDeser(cmd, InspectVolume[].class);
    return inspectVolume[0];
  }

  private static <T> T executeAndDeser(String[] cmd, Class<T> classOfT) throws IOException {
    Process process = Runtime.getRuntime().exec(cmd);
    try (InputStreamReader reader = new InputStreamReader(process.getInputStream())) {
      return GSON.fromJson(reader, classOfT);
    }
  }

  private record Inspect(Mount[] Mounts) {
    private record Mount(String Type, String Source, String Destination) {}
  }

  private record InspectVolume(Options Options) {
    private record Options(String device) {}
  }

  public static void main(String[] args) {
    log.info(isRunningInsideDocker());
    log.info(hostPath("/source"));
    log.info(hostPath("/destination"));
  }

  private DockerHelper() {}
}
