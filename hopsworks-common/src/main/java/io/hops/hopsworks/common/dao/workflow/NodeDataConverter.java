package io.hops.hopsworks.common.dao.workflow;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.PersistenceException;

@Converter(autoApply = true)
public class NodeDataConverter implements AttributeConverter<JsonNode, String> {

  @Override
  public String convertToDatabaseColumn(JsonNode jsonObject) {
    if (jsonObject == null) {
      return null;
    }
    return jsonObject.toString();
  }

  @Override
  public JsonNode convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      return new ObjectMapper().readTree(dbData);
    } catch (final IOException e) {
      throw new PersistenceException(e);
    }

  }

}
