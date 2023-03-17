package com.willmolloy;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Job runner.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class JobRunner {

  private static final Logger log = LogManager.getLogger();

  void run(Job job) {
    List<Path> sourceFiles = job.scanSource();
    log.debug("Source: {}", sourceFiles);
    List<Path> destinationFiles = job.scanDestination();
    log.debug("Dest: {}", destinationFiles);

    // if file/directory on src AND not on dest, copy to dest
    HashSet<Path> filesInSourceAndNotInDestination = new HashSet<>();
    // TODO not gonna work if files are absolute
    filesInSourceAndNotInDestination.addAll(sourceFiles);
    filesInSourceAndNotInDestination.removeAll(destinationFiles);
    log.debug("Copy to dest: {}", filesInSourceAndNotInDestination);

    for (Path sourcePath : filesInSourceAndNotInDestination) {
      Path destinationPath = job.destinationRoot().resolve(job.sourceRoot().relativize(sourcePath));
      job.copyToDestination(sourcePath, destinationPath);
    }

    // if file/directory not on src AND on dest, delete from dest

    // if file/directory on src AND dest, update dest
  }
}
