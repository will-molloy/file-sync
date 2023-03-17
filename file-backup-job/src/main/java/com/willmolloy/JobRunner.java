package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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
    List<Path> sourceFiles = job.scanSource();
    log.debug("Source: {}", sourceFiles);
    List<Path> destinationFiles = job.scanDestination();
    log.debug("Dest: {}", destinationFiles);

    // if file/directory on src AND not on dest, copy to dest
    Set<Path> toCopy = difference(sourceFiles, destinationFiles);
    log.debug("Copy to dest: {}", toCopy);
    for (Path path : toCopy) {
      Path sourcePath = job.sourceRoot().resolve(path);
      Path destinationPath = job.destinationRoot().resolve(path);
      job.copyToDestination(sourcePath, destinationPath);
    }

    // if file/directory not on src AND on dest, delete from dest
    Set<Path> toDelete = difference(destinationFiles, sourceFiles);
    log.debug("Delete from dest: {}", toDelete);
    for (Path path : toDelete) {
      Path destinationPath = job.destinationRoot().resolve(path);
      job.deleteFromDestination(destinationPath);
    }

    // if file/directory on src AND dest, update dest - if src newer
    List<Path> toUpdate =
        intersection(sourceFiles, destinationFiles).stream()
            .filter(
                path -> {
                  Path sourcePath = job.sourceRoot().resolve(path);
                  Path destinationPath = job.destinationRoot().resolve(path);
                  try {
                    FileTime sourceLastModified = Files.getLastModifiedTime(sourcePath);
                    FileTime destLastModified = Files.getLastModifiedTime(destinationPath);
                    return sourceLastModified.compareTo(destLastModified) > 0;
                  } catch (IOException e) {
                    log.error("Error getting last modified attribute", e);
                    throw new UncheckedIOException(e);
                  }
                })
            .toList();

    log.debug("Update on dest: {}", toUpdate);
    for (Path path : toUpdate) {
      Path sourcePath = job.sourceRoot().resolve(path);
      Path destinationPath = job.destinationRoot().resolve(path);
      job.copyToDestination(sourcePath, destinationPath);
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
