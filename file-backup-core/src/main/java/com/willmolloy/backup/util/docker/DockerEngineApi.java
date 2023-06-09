package com.willmolloy.backup.util.docker;

import static com.google.common.base.Preconditions.checkNotNull;

import com.willmolloy.backup.util.HttpClientWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Docker Engine API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class DockerEngineApi {

  private static final String BASE_URL = "http://host.docker.internal:2375";

  private final HttpClientWrapper httpClientWrapper;

  DockerEngineApi(HttpClientWrapper httpClientWrapper) {
    this.httpClientWrapper = checkNotNull(httpClientWrapper);
  }

  /**
   * Inspects a container.
   *
   * @param containerHostName container host name
   * @see <a
   *     href=https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerInspect>API
   *     doc</a>
   */
  Optional<ContainerInspect> containerInspect(String containerHostName) {
    return httpClientWrapper.getJson(
        URI.create("%s/containers/%s/json".formatted(BASE_URL, containerHostName)),
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

  /**
   * Inspect a volume.
   *
   * @param volumeName volume name
   * @see <a href=https://docs.docker.com/engine/api/v1.43/#tag/Volume/operation/VolumeInspect>API
   *     doc</a>
   */
  Optional<VolumeInspect> volumeInspect(String volumeName) {
    return httpClientWrapper.getJson(
        URI.create("%s/volumes/%s".formatted(BASE_URL, volumeName)), VolumeInspect.class);
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
}
