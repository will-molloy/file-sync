package com.willmolloy.backup.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper for computing MD5 hash.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Md5Helper {

  private static final Logger log = LogManager.getLogger();

  private static final MessageDigest MD5;
  private static final Base64.Encoder BASE64;

  static {
    try {
      MD5 = MessageDigest.getInstance("MD5");
      BASE64 = Base64.getEncoder();
    } catch (NoSuchAlgorithmException e) {
      log.error("Error initialising", e);
      throw new RuntimeException(e);
    }
  }

  /** Returns the MD5 hash of the given {@code file} contents, in base64. */
  public static String md5AsBase64(Path file) throws IOException {
    try {
      byte[] data = Files.readAllBytes(file);
      byte[] hash = MD5.digest(data);
      return BASE64.encodeToString(hash);
    } catch (RuntimeException | IOException e) {
      log.error("Error computing MD5 hash of file: [%s]".formatted(file), e);
      return BASE64.encodeToString(new byte[0]);
    }
  }

  private Md5Helper() {}
}
