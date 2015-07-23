package se.kth.bbc.jobs.model.configuration;

import com.google.common.base.Strings;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import se.kth.bbc.jobs.DatabaseJsonObject;

/**
 *
 * @author stig
 */
@Converter
public class JobConfigurationConverter implements
        AttributeConverter<JobConfiguration, String> {

  @Override
  public String convertToDatabaseColumn(JobConfiguration config) {
    return config.getReducedJsonObject().toJson();
  }

  @Override
  public JobConfiguration convertToEntityAttribute(String config) {
    if(Strings.isNullOrEmpty(config)){
      return null;
    }
    try (JsonReader reader = Json.createReader(new StringReader(config))) {
      JsonObject obj = reader.readObject();
      DatabaseJsonObject json = new DatabaseJsonObject(obj);
      return JobConfiguration.JobConfigurationFactory.getJobConfigurationFromJson(json);
    }
  }

}
