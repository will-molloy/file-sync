package com.willmolloy.sync.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    return BaseEncoding.base64().encode(md5(file));
  }

  private static byte[] md5(Path file) throws IOException {
    log.debug("md5({})", file);
    try (InputStream inputStream = Files.newInputStream(file);
        HashingInputStream his = new HashingInputStream(Hashing.md5(), inputStream)) {
      his.readAllBytes();
      HashCode md5Hash = his.hash();
      return md5Hash.asBytes();
    }
  }

  private Md5Helper() {}
}
