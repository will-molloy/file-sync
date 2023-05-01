package com.willmolloy.backup;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup {

  Source source();

  Destination destination();

  /** Backup location (source or destination). */
  interface Location {

    /**
     * Scans the location.
     * @return map of relativized key (i.e. file name) to file
     */
    Map<String, File> scan();
  }

  interface Source extends Location {

    /**
     * Gets the file, at the given path, downloaded to disk!
     */
    // TODO Path can't be abstracted, right?? We need something concrete eventually...
    //  Just strange how we have this AND File... merge them?
    Path get(String key);
  }

  interface Destination extends Location {

    void put(String key, Path sourceFile);

    void delete(String key);
  }

  interface File {

    long sizeInBytes();

    Instant lastModified();
  }
}
