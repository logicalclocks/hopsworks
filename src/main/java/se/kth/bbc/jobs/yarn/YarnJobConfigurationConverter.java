package se.kth.bbc.jobs.yarn;

import com.google.gson.Gson;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 *
 * @author stig
 */
@Converter
public class YarnJobConfigurationConverter implements
        AttributeConverter<YarnJobConfiguration, String> {

  @Override
  public String convertToDatabaseColumn(YarnJobConfiguration config) {
    return config.getReducedJsonObject().toJson();
  }

  @Override
  public YarnJobConfiguration convertToEntityAttribute(String config) {
    //TODO
  }

}
