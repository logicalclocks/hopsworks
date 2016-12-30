package io.hops.hopsworks.common.jobs.configuration;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import io.hops.hopsworks.common.jobs.MutableJsonObject;

@Converter
public class JsonReduceableConverter implements
        AttributeConverter<JsonReduceable, String> {

  @Override
  public String convertToDatabaseColumn(JsonReduceable config) {
    return config.getReducedJsonObject().toJson();
  }

  @Override
  public JobConfiguration convertToEntityAttribute(String config) {
    if (Strings.isNullOrEmpty(config)) {
      return null;
    }
    try (JsonReader reader = Json.createReader(new StringReader(config))) {
      JsonObject obj = reader.readObject();
      MutableJsonObject json = new MutableJsonObject(obj);
      return JobConfiguration.JobConfigurationFactory.
              getJobConfigurationFromJson(json);
    }
  }

}
