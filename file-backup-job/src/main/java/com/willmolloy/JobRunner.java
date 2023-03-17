package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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
    HashSet<Path> toCopy = new HashSet<>();
    // TODO not gonna work if files are absolute
    toCopy.addAll(sourceFiles);
    toCopy.removeAll(destinationFiles);
    log.debug("Copy to dest: {}", toCopy);
    for (Path path : toCopy) {
      Path sourcePath = job.sourceRoot().resolve(path);
      Path destinationPath = job.destinationRoot().resolve(path);
      job.copyToDestination(sourcePath, destinationPath);
    }

    // if file/directory not on src AND on dest, delete from dest
    HashSet<Path> toDelete = new HashSet<>();
    toDelete.addAll(destinationFiles);
    toDelete.removeAll(sourceFiles);
    log.debug("Delete from dest: {}", toDelete);
    for (Path path : toDelete) {
      Path destinationPath = job.destinationRoot().resolve(path);
      job.deleteFromDestination(destinationPath);
    }

    // if file/directory on src AND dest, update dest - if src newer
    HashSet<Path> toUpdate = new HashSet<>();
    toUpdate.addAll(sourceFiles);
    toUpdate.retainAll(destinationFiles);
    log.debug("Update on dest: {}", toUpdate);
    for (Path path : toUpdate) {
      Path sourcePath = job.sourceRoot().resolve(path);
      Path destinationPath = job.destinationRoot().resolve(path);
      try {
        FileTime sourceLastModified = Files.getLastModifiedTime(sourcePath);
        FileTime destLastModified = Files.getLastModifiedTime(destinationPath);
        if (sourceLastModified.compareTo(destLastModified) > 0) {
          job.copyToDestination(sourcePath, destinationPath);
        }
      } catch (IOException e) {
        log.error("Error getting last modified attribute", e);
        throw new UncheckedIOException(e);
      }
    }
  }
}
