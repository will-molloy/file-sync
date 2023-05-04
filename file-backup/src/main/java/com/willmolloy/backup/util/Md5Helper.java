package com.willmolloy.backup.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Helper for computing MD5 hash.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Md5Helper {

  // TODO cache the MD5s?

  /** Returns the MD5 hash of the given {@code file} contents, in base64. */
  public static String md5Base64(Path file) throws IOException {
    try (InputStream inputStream = Files.newInputStream(file)) {
      byte[] md5 = DigestUtils.md5(inputStream);
      return Base64.encodeBase64String(md5);
    }
  }

  /** Returns the MD5 hash of the given {@code file} contents, in base16. */
  public static String md5Base16(Path file) throws IOException {
    try (InputStream inputStream = Files.newInputStream(file)) {
      return DigestUtils.md5Hex(inputStream);
    }
  }

  private Md5Helper() {}
}
