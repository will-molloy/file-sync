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

  private final HttpClientWrapper httpClientWrapper;

  DockerEngineApi(HttpClientWrapper httpClientWrapper) {
    this.httpClientWrapper = checkNotNull(httpClientWrapper);
  }

  /**
   * Inspects a container.
   *
   * @param hostname container host name
   * @apiNote Only works if running in docker container!
   * @see <a
   *     href=https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerInspect>API
   *     doc</a>
   */
  Optional<ContainerInspect> containerInspect(String hostname) {
    return httpClientWrapper.getJson(
        URI.create("http://host.docker.internal:2375/containers/%s/json".formatted(hostname)),
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
   * @param volume volume name
   * @apiNote Only works if running in docker container!
   * @see <a href=https://docs.docker.com/engine/api/v1.43/#tag/Volume/operation/VolumeInspect>API
   *     doc</a>
   */
  Optional<VolumeInspect> volumeInspect(String volume) {
    return httpClientWrapper.getJson(
        URI.create("http://host.docker.internal:2375/volumes/%s".formatted(volume)),
        VolumeInspect.class);
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
