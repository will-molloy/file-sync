package com.willmolloy;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Job runner. Main backup algorithm is defined here.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class JobRunner {

  private static final Logger log = LogManager.getLogger();

  private final Job job;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "False positive? Job is not mutable")
  public JobRunner(Job job) {
    this.job = requireNonNull(job);
  }

  void run() {
    Set<Path> sourceFiles = job.scanSource().collect(toSet());
    log.debug("Source: {}", sourceFiles);
    Set<Path> destFiles = job.scanDestination().collect(toSet());
    log.debug("Dest: {}", destFiles);

    // 1.) if file/directory on src AND not on dest, copy to dest
    Set<Path> toCopy = difference(sourceFiles, destFiles);
    // To minimise copies, leaves only, e.g. A, A/B, A/B/C - Just A/B/C
    toCopy = leaves(toCopy);
    log.debug("toCopy: {}", toCopy);
    for (Path file : toCopy) {
      job.copy(file);
    }

    // 2.) if file/directory not on src AND on dest, delete from dest
    Set<Path> toDelete = difference(destFiles, sourceFiles);
    // To minimise deletes, parents only, e.g. e.g. A, A/B, A/B/C - Just A
    toDelete = parents(toDelete);
    log.debug("toDelete: {}", toDelete);
    for (Path file : toDelete) {
      job.delete(file);
    }

    // 3.) if file/directory on src AND dest, update dest
    Set<Path> toUpdate =
        // TODO only needs to run on leaves? (and just files, not directories??)
        //  what if entire directory structure is mirrored... need to update attributes??
        intersection(leaves(sourceFiles), leaves(destFiles));
    log.debug("toUpdate: {}", toUpdate);
    for (Path file : toUpdate) {
      job.update(file);
    }
  }

  private static <T> Set<T> difference(Set<T> set1, Set<T> set2) {
    Set<T> set = new HashSet<>();
    set.addAll(set1);
    set.removeAll(set2);
    return set;
  }

  private static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
    Set<T> set = new HashSet<>();
    set.addAll(set1);
    set.retainAll(set2);
    return set;
  }

  // TODO O(n^2) not good
  private static Set<Path> leaves(Set<Path> paths) {
    return paths.stream()
        .filter(
            path1 -> paths.stream().noneMatch(path2 -> path1 != path2 && path2.startsWith(path1)))
        .collect(toSet());
  }

  // TODO name is right? What exactly is this method doing haha... wrote it and forgot.
  //  It's just an inverse of the above?
  private static Set<Path> parents(Set<Path> paths) {
    return paths.stream()
        .filter(
            path1 -> paths.stream().noneMatch(path2 -> path1 != path2 && path1.startsWith(path2)))
        .collect(toSet());
  }
}
