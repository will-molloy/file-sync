package com.willmolloy;

import java.util.List;

/**
 * Job definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Job {

  List<String> scanSource();

  List<String> scanDestination();

  void copyToDestination(String file);

  void deleteFromDestination(String file);

  boolean isNewerOnSource(String file);
}
