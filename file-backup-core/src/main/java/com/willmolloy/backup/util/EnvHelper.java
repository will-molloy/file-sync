package com.willmolloy.backup.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * Methods for reading env variables.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class EnvHelper {

  public static String getRequiredEnvVariable(String name) {
    return checkNotNull(System.getenv(name), "Missing: %s", name);
  }

  public static Optional<String> getOptionalEnvVariable(String name) {
    return Optional.ofNullable(System.getenv(name));
  }

  private EnvHelper() {}
}
