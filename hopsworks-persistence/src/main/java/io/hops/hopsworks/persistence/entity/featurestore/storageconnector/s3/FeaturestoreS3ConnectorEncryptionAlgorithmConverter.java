package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3;


import com.google.common.base.Strings;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class FeaturestoreS3ConnectorEncryptionAlgorithmConverter implements
  AttributeConverter<FeaturestoreS3ConnectorEncryptionAlgorithm, String> {

  @Override
  public String convertToDatabaseColumn(
    FeaturestoreS3ConnectorEncryptionAlgorithm featurestoreS3ConnectorEncryptionAlgorithm) {
    if (featurestoreS3ConnectorEncryptionAlgorithm == null) {
      return null;
    }
    return featurestoreS3ConnectorEncryptionAlgorithm.toString();
  }
  
  @Override
  public FeaturestoreS3ConnectorEncryptionAlgorithm convertToEntityAttribute(String s) {
    if(Strings.isNullOrEmpty(s)){
      return  null;
    }
    return FeaturestoreS3ConnectorEncryptionAlgorithm.fromValue(s);
  }
}
