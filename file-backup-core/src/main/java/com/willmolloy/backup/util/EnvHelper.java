package com.willmolloy.backup.util;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/**
 * Methods for reading env variables.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class EnvHelper {

  public static String readRequiredEnvVariable(String name) {
    return requireNonNull(System.getenv(name), "Missing %s".formatted(name));
  }

  public static Optional<String> readOptionalEnvVariable(String name) {
    return Optional.ofNullable(System.getenv(name));
  }

  private EnvHelper() {}
}
