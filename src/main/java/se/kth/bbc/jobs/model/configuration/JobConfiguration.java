package se.kth.bbc.jobs.model.configuration;

import java.util.ArrayList;
import java.util.List;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.cuneiform.model.CuneiformJobConfiguration;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.JsonReducable;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * Represents the persistable configuration of a runnable job. To be persisted
 * as JSON, the getReducedJsonObject() method is called.
 * <p>
 * @author stig
 */
public abstract class JobConfiguration implements JsonReducable {

  private String applicationName;

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public static class JobConfigurationFactory {

    public static JobConfiguration getJobConfigurationFromJson(DatabaseJsonObject object)
            throws IllegalArgumentException {   
      //First: check if null
      if(object == null){
        throw new NullPointerException("Cannot get a JobConfiguration object from null.");
      }
      //Get the type
      String jType = object.getString("type");
      JobType type = JobType.valueOf(jType);
      JobConfiguration conf;
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
                  "The given jobtype is not recognized by this factory.");
      }
      //Update the object
      conf.updateFromJson(object);
      return conf;      
    }
    
    public static List<JobConfiguration> getAllAssignableTemplates(){
      List<JobConfiguration> templates = new ArrayList<>(4);
      templates.add(new YarnJobConfiguration());
      templates.add(new CuneiformJobConfiguration());
      templates.add(new SparkJobConfiguration());
      templates.add(new AdamJobConfiguration());
      return templates;
    }
    
  }
}
