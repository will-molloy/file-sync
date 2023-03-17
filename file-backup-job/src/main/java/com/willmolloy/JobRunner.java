package com.willmolloy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    List<String> sourceFiles = job.scanSource();
    log.debug("Source: {}", sourceFiles);
    List<String> destinationFiles = job.scanDestination();
    log.debug("Dest: {}", destinationFiles);

    // if file/directory on src AND not on dest, copy to dest
    Set<String> toCopy = difference(sourceFiles, destinationFiles);
    log.debug("Copy to dest: {}", toCopy);
    for (String file : toCopy) {
      job.copyToDestination(file);
    }

    // if file/directory not on src AND on dest, delete from dest
    Set<String> toDelete = difference(destinationFiles, sourceFiles);
    log.debug("Delete from dest: {}", toDelete);
    for (String file : toDelete) {
      job.deleteFromDestination(file);
    }

    // if file/directory on src AND dest, update dest - if src newer
    List<String> toUpdate =
        intersection(sourceFiles, destinationFiles).stream().filter(job::isNewerOnSource).toList();
    log.debug("Update on dest: {}", toUpdate);
    for (String file : toUpdate) {
      job.copyToDestination(file);
    }
  }

  private static <T> Set<T> difference(Collection<T> collA, Collection<T> collB) {
    Set<T> set = new HashSet<>();
    set.addAll(collA);
    set.removeAll(collB);
    return set;
  }

  private static <T> Set<T> intersection(Collection<T> collA, Collection<T> collB) {
    Set<T> set = new HashSet<>();
    set.addAll(collA);
    set.retainAll(collB);
    return set;
  }
}
