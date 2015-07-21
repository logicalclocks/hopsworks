package se.kth.bbc.jobs.yarn;

import com.google.gson.Gson;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 *
 * @author stig
 */
@Converter
public class YarnJobConfigurationConverter implements
        AttributeConverter<YarnJobConfiguration, String> {

  @Override
  public String convertToDatabaseColumn(YarnJobConfiguration config) {
    Gson gson = new Gson();
    return gson.toJson(config);
  }

  @Override
  public YarnJobConfiguration convertToEntityAttribute(String config) {
    Gson gson = new Gson();
    return gson.fromJson(config, YarnJobConfiguration.class);
  }

}
