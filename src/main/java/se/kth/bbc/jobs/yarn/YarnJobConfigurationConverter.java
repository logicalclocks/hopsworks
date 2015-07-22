package se.kth.bbc.jobs.yarn;

import com.google.gson.Gson;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.cuneiform.model.CuneiformJobConfiguration;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;

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
    try (JsonReader reader = Json.createReader(new StringReader(config))) {
      JsonObject obj = reader.readObject();
      DatabaseJsonObject json = new DatabaseJsonObject(obj);
      String jType = json.getString("type");
      JobType type = JobType.valueOf(jType);
      YarnJobConfiguration conf;
      switch (type) {
        case ADAM:
          conf = new AdamJobConfiguration();
          break;
        case CUNEIFORM:
          conf = new CuneiformJobConfiguration();
          break;
        case SPARK:
          conf = new SparkJobConfiguration();
          break;
        case YARN:
          conf = new YarnJobConfiguration();
          break;
        default:
          throw new UnsupportedOperationException(
                  "The given jobtype does not yet support conversion to DB JSON object.");
      }
      conf.updateFromJson(json);
      return conf;
    }
  }

}
