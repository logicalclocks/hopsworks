package io.hops.hopsworks.common.featurestore.storageconnectors.s3;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.Arrays;
import java.util.logging.Level;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FeaturestoreS3ConnectorEncryptionAlgorithm {
  AES_256("AES-256", "Server-Side Encryption with Amazon S3-Managed Keys (SSE-S3)", false),
  AWS_KMS("AWS-KMS", "Server-Encryption with AWS KMS-Managed Keys (SSE-KMS)", true);
  
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
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public boolean isRequiresKey() {
    return requiresKey;
  }
  
  public void setRequiresKey(boolean requiresKey) {
    this.requiresKey = requiresKey;
  }
  
  /**
   * Searches encryption algorithms by name
   * @param algorithmName
   * @return
   * @throws FeaturestoreException
   */
  public static FeaturestoreS3ConnectorEncryptionAlgorithm getEncryptionAlgorithmByName(String algorithmName) throws
    FeaturestoreException {
    return
      Arrays.asList(FeaturestoreS3ConnectorEncryptionAlgorithm.values()).stream().
        filter(a -> a.getAlgorithm().equals(algorithmName)).findAny().orElseThrow(() ->
        new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_SERVER_ENCRYPTION_ALGORITHM_DOES_NOT_EXIST,
      Level.FINE, "Encryption algorithm does not exist or is not supported"));
  }
}
