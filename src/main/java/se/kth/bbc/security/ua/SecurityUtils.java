 
package se.kth.bbc.security.ua;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base32;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
 
public class SecurityUtils {

  /**
   * Generate the secrete key for mobile devices.
   *
   * @return
   */
  public static String calculateSecretKey() {
    byte[] secretKey = new byte[10];
    new SecureRandom().nextBytes(secretKey);
    Base32 codec = new Base32();
    byte[] encodedKey = codec.encode(secretKey);
    return new String(encodedKey);
  }

  /**
   * Generate a random password to be sent to the user
   *
   * @param length
   * @return
   */
  public static String getRandomString(int length) {

    String randomStr = UUID.randomUUID().toString();
    while (randomStr.length() < length) {
      randomStr += UUID.randomUUID().toString();
    }
    return randomStr.substring(0, length);
  }
}
