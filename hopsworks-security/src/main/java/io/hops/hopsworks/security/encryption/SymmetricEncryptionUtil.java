package io.hops.hopsworks.security.encryption;

import org.apache.commons.net.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class SymmetricEncryptionUtil {
  
  private static Key generateKey(String userKey, String masterKey) {
    // This is for backwards compatibility
    // sha256 of 'adminpw'
    if (masterKey.equals("5fcf82bc15aef42cd3ec93e6d4b51c04df110cf77ee715f62f3f172ff8ed9de9")) {
      return new SecretKeySpec(userKey.substring(0, 16).getBytes(), "AES");
    }
    
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      sb.append(userKey.charAt(i));
      if (masterKey.length() > i + 1) {
        sb.append(masterKey.charAt(i + 1));
      } else {
        sb.append(userKey.charAt(Math.max(0, userKey.length() - i)));
      }
    }
    return new SecretKeySpec(sb.toString().getBytes(), "AES");
  }
  
  /**
   *
   * @param key
   * @param plaintext
   * @return
   * @throws Exception
   */
  public static String encrypt(String key, String plaintext, String masterEncryptionPassword)
    throws Exception {
    
    Key aesKey = generateKey(key, masterEncryptionPassword);
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, aesKey);
    byte[] encrypted = cipher.doFinal(plaintext.getBytes());
    return Base64.encodeBase64String(encrypted);
  }
  
  /**
   *
   * @param key
   * @param ciphertext
   * @return
   * @throws Exception
   */
  public static String decrypt(String key, String ciphertext, String masterEncryptionPassword)
    throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    Key aesKey = generateKey(key, masterEncryptionPassword);
    cipher.init(Cipher.DECRYPT_MODE, aesKey);
    String decrypted = new String(cipher.doFinal(Base64.decodeBase64(ciphertext)));
    return decrypted;
  }
}
