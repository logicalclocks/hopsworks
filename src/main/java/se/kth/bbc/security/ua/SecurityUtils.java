 
package se.kth.bbc.security.ua;

import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base32;
import java.security.SecureRandom;
import java.util.UUID;
 
public class SecurityUtils {

  /**
   * Generate the secrete key for mobile devices.
   *
   * @return
   */
  public static String calculateSecretKey() throws NoSuchAlgorithmException {
    byte[] secretKey = new byte[10];
    SecureRandom sha1Prng = SecureRandom.getInstance("SHA1PRNG");
    sha1Prng.nextBytes(secretKey);
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
  public static String getRandomPassword(int length) {

    String randomStr = UUID.randomUUID().toString();
    while (randomStr.length() < length) {
      randomStr += UUID.randomUUID().toString();
    }
    return randomStr.substring(0, length);
  }
}
