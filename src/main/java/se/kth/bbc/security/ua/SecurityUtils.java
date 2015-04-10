/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base32;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
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
   * Convert the passwords or security questions to sha256.
   *
   * @param data
   * @return
   * @throws NoSuchAlgorithmException
   * @throws UnsupportedEncodingException
   */
  public static String converToSHA256(String data) throws
          NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(data.getBytes("UTF-8"));
    return bytesToHex(md.digest());
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuffer result = new StringBuffer();
    for (byte byt : bytes) {
      result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
    }
    return result.toString();
  }

  /**
   * Generate a random password to be sent to the user
   *
   * @param length
   * @return
   */
  public static String getRandomString() {
    // password length
    int length = 6;
    String randomStr = UUID.randomUUID().toString();
    while (randomStr.length() < length) {
      randomStr += UUID.randomUUID().toString();
    }
    return randomStr.substring(0, length);
  }
}
