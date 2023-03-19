package com.willmolloy;

import java.nio.file.Path;
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

  private final Job job;

  public JobRunner(Job job) {
    this.job = job;
  }

  void run() {
    List<Path> sourceFiles = job.scanSource().toList();
    log.debug("Source: {}", sourceFiles);
    List<Path> destFiles = job.scanDestination().toList();
    log.debug("Dest: {}", destFiles);

    // 1.) if file/directory on src AND not on dest, copy to dest
    List<Path> sourceFileLeaves =
        sourceFiles.stream()
            // To minimise copies, leaves only, e.g. A, A/B, A/B/C - Just A/B/C
            .filter(
                path1 ->
                    sourceFiles.stream()
                        .noneMatch(path2 -> path1 != path2 && path2.startsWith(path1)))
            .toList();
    Set<Path> toCopy = difference(sourceFileLeaves, destFiles);
    log.debug("Copy to dest: {}", toCopy);
    for (Path file : toCopy) {
      job.copyToDestination(file);
    }

    // 2.) if file/directory not on src AND on dest, delete from dest
    List<Path> toDelete =
        destFiles.stream()
            .peek(p -> log.debug("1 {}", p))
            // exclude parent directories where children would've been copied from source to dest -
            // e.g. A/B/C copied. Don't delete A/B.
            // TODO O(N^2) quite bad - trie structure solves this?
            .filter(dest -> sourceFileLeaves.stream().noneMatch(source -> source.startsWith(dest)))
            .peek(p -> log.debug("2 {}", p))
            // To minimise deletes, parents only, e.g. e.g. A, A/B, A/B/C - Just A
            .filter(
                dest1 ->
                    // TODO bug is that we compare with the original list, not what remains after above filter
                    destFiles.stream()
                        .noneMatch(dest2 -> dest1 != dest2 && dest1.startsWith(dest2)))
            .peek(p -> log.debug("3 {}", p))
            .toList();
    log.debug("Delete from dest: {}", toDelete);
    for (Path file : toDelete) {
      job.deleteFromDestination(file);
    }

    // 3.) if file/directory on src AND dest, update dest
    List<Path> toUpdate =
        intersection(sourceFileLeaves, destFiles).stream()
            .filter(job::sourceNotEqualDestination)
            .toList();
    log.debug("Update on dest: {}", toUpdate);
    for (Path file : toUpdate) {
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
