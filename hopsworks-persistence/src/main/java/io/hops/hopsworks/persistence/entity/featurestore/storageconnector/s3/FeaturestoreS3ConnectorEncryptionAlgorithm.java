package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3;

import javax.xml.bind.annotation.XmlEnumValue;
import java.util.Arrays;
import java.util.Optional;

public enum FeaturestoreS3ConnectorEncryptionAlgorithm {
  AES256("AES256", "Server-Side Encryption with Amazon S3-Managed Keys (SSE-S3)", false),
  SSE_KMS("SSE-KMS", "Server-Encryption with AWS KMS-Managed Keys (SSE-KMS)", true);
  
  private String algorithm;
  private String description;
  private boolean requiresKey;
  
  FeaturestoreS3ConnectorEncryptionAlgorithm(String algorithm, String description, boolean requiresKey) {
    this.algorithm = algorithm;
    this.description = description;
    this.requiresKey = requiresKey;
  }
  
  public String getAlgorithm() {
    return algorithm;
  }
  
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }
  
  public String getDescription() { return description; }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public boolean isRequiresKey() {
    return requiresKey;
  }
  
  public void setRequiresKey(boolean requiresKey) {
    this.requiresKey = requiresKey;
  }
  
  public static FeaturestoreS3ConnectorEncryptionAlgorithm fromString(String s)
    throws IllegalArgumentException{
    Optional<FeaturestoreS3ConnectorEncryptionAlgorithm> algorithm =
      Arrays.asList(FeaturestoreS3ConnectorEncryptionAlgorithm.values())
        .stream().filter(a -> a.getAlgorithm().equals(s)).findFirst();
    
    if (algorithm.isPresent()) {
      return algorithm.get();
    } else {
      throw new IllegalArgumentException("Invalid encryption algorithm provided");
    }
  }
  
  @Override
  public String toString() {
    return algorithm;
  }
}
