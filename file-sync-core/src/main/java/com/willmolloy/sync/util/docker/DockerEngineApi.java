package com.willmolloy.sync.util.docker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import feign.optionals.OptionalDecoder;
import java.util.List;
import java.util.Optional;

/**
 * Docker Engine API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
interface DockerEngineApi {

  static DockerEngineApi create() {
    return Feign.builder()
        .decoder(new OptionalDecoder(new GsonDecoder()))
        .dismiss404()
        .target(DockerEngineApi.class, "http://host.docker.internal:2375");
  }

  /**
   * Inspects a container.
   *
   * @param container container host name
   * @see <a
   *     href=https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerInspect>API
   *     doc</a>
   */
  @RequestLine("GET /containers/{container}/json")
  Optional<Container> inspectContainer(@Param("container") String container);

  /**
   * Container inspect result.
   *
   * @param Mounts container mounts
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "NM_METHOD_NAMING_CONVENTION"})
  record Container(List<Mount> Mounts) {
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
   * @see <a href=https://docs.docker.com/engine/api/v1.43/#tag/Volume/operation/VolumeInspect>API
   *     doc</a>
   */
  @RequestLine("GET /volumes/{volume}")
  Optional<Volume> inspectVolume(@Param("volume") String volume);

  /**
   * Volume inspect result.
   *
   * @param Options volume options
   */
  @SuppressFBWarnings("NM_METHOD_NAMING_CONVENTION")
  record Volume(Options Options) {
    /**
     * Volume options.
     *
     * @param device host path
     */
    record Options(String device) {}
  }
}
