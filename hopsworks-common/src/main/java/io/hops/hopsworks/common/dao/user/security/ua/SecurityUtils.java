/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.dao.user.security.ua;

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
