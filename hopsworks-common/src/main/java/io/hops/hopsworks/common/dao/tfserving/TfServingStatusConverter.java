package io.hops.hopsworks.common.dao.tfserving;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class TfServingStatusConverter implements AttributeConverter<TfServingStatusEnum, String> {

  @Override
  public String convertToDatabaseColumn(TfServingStatusEnum attribute) {
    return attribute.toString();
  }

  @Override
  public TfServingStatusEnum convertToEntityAttribute(String dbData) {
    return TfServingStatusEnum.fromString(dbData);
  }

}
