
package io.hops.hopsworks.jwt;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.ejb.Singleton;

@Singleton
public class SigningKeyGenerator {
  
  private KeyGenerator keyGenerator;
  
  /**
   * Generates a secret key for the given algorithm.
   * @param algorithm
   * @return base64Encoded string
   * @throws NoSuchAlgorithmException 
   */
  public String getSigningKey(String algorithm) throws NoSuchAlgorithmException {
    if (keyGenerator == null || !keyGenerator.getAlgorithm().equals(algorithm)) {
      keyGenerator = KeyGenerator.getInstance(algorithm);
    }
    byte[] keyBytes = keyGenerator.generateKey().getEncoded();
    String base64Encoded = Base64.getEncoder().encodeToString(keyBytes);
    return base64Encoded;
  }
}
