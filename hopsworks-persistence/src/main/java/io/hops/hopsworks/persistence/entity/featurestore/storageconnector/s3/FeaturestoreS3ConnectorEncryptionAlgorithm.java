package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3;


import com.fasterxml.jackson.annotation.JsonValue;

public enum FeaturestoreS3ConnectorEncryptionAlgorithm {
  AES256("AES256", "Server-Side Encryption with Amazon S3-Managed Keys (SSE-S3)", false),
  SSE_KMS("SSE_KMS", "Server-Encryption with AWS KMS-Managed Keys (SSE-KMS)", true);
  
  private String algorithm;
  private String description;
  private boolean requiresKey;
  FeaturestoreS3ConnectorEncryptionAlgorithm(String algorithm, String description, boolean requiresKey) {
    this.algorithm = algorithm;
    this.description = description;
    this.requiresKey = requiresKey;
  }
  
  @JsonValue
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
}
