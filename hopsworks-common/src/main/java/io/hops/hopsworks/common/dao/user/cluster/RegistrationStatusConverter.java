package io.hops.hopsworks.common.dao.user.cluster;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class RegistrationStatusConverter implements AttributeConverter<RegistrationStatusEnum, String> {

  @Override
  public String convertToDatabaseColumn(RegistrationStatusEnum attribute) {
    return attribute.toString();
  }

  @Override
  public RegistrationStatusEnum convertToEntityAttribute(String dbData) {
    return RegistrationStatusEnum.fromString(dbData);
  }
  
}
