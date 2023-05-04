package com.willmolloy.backup;

import java.util.Map;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Backup type definition.
 *
 * @param <SourceT> source location type
 * @param <DestinationT> destination location type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup<SourceT extends Backup.Location, DestinationT extends Backup.Location> {

  SourceT source();

  DestinationT destination();

  boolean put(String key);

  boolean delete(String key);

  /** Backup location (source or destination). */
  interface Location {

    /** Map of relativized file path to file. */
    Map<String, File> scan();
  }

  /** Backup file. */
  // interface rather than record so the operations can be lazy
  interface File {

    /**
     * File ETag.
     *
     * <p>Currently implemented as MD5 digest of file contents in base16.
     *
     * @implNote ETags are required to be wrapped in {@code "} quotes.
     * @see S3Object#eTag()
     */
    String etag();
  }
}
