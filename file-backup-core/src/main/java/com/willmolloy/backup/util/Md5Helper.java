package com.willmolloy.backup.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper for computing MD5 hash.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Md5Helper {

  private static final Logger log = LogManager.getLogger();

  /** Computes the MD5 hash of the given {@code file} contents, in base64. */
  public static String md5Base64(Path file) throws IOException {
    return Base64.encodeBase64String(md5(file));
  }

  /** Computes the MD5 hash of the given {@code file} contents, in base16. */
  public static String md5Base16(Path file) throws IOException {
    return Hex.encodeHexString(md5(file));
  }

  // TODO cache the MD5s?
  private static byte[] md5(Path file) throws IOException {
    log.debug("md5({})", file);
    try (InputStream inputStream = Files.newInputStream(file)) {
      return DigestUtils.md5(inputStream);
    }
  }

  private Md5Helper() {}
}
