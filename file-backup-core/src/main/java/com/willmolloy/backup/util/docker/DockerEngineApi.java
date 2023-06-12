package com.willmolloy.backup.util.docker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.io.IOException;
import java.util.List;

/**
 * Docker Engine API.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
interface DockerEngineApi {

  static DockerEngineApi create(){
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://host.docker.internal:2375")
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    return retrofit.create(DockerEngineApi.class);
  }

  /**
   * Inspects a container.
   *
   * @param containerHostName container host name
   * @see <a
   *     href=https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerInspect>API
   *     doc</a>
   */
  @GET("containers/{containerHostName}/json")
  Call<ContainerInspect> inspectContainer(@Path("containerHostName") String containerHostName);

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
  @GET("volumes/{volumeName}")
  VolumeInspect inspectVolume(@Path("volumeName") String volumeName);

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
