package se.kth.bbc.jobs.model.configuration;

import com.google.common.base.Strings;
import java.util.EnumSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.fileoperations.ErasureCodeJobConfiguration;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.flink.FlinkJobConfiguration;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.JsonReduceable;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * Represents the persistable configuration of a runnable job. To be persisted
 * as JSON, the getReducedJsonObject() method is called.
 * <p/>
 * @author stig
 */
@XmlRootElement
public abstract class JobConfiguration implements JsonReduceable {

  protected String appName;
  protected ScheduleDTO schedule;
  protected KafkaDTO kafka;
  protected String sessionId = "";

  protected final static String KEY_APPNAME = "APPNAME";
  protected final static String KEY_SCHEDULE = "SCHEDULE";
  protected final static String KEY_KAFKA = "KAFKA";

  protected JobConfiguration() {
    //Needed for JAXB
  }

  public JobConfiguration(String appname) {
    this.appName = appname;
  }

  /**
   * Return the JobType this JobConfiguration is meant for.
   * <p/>
   * @return
   */
  public abstract JobType getType();

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public ScheduleDTO getSchedule() {
    return schedule;
  }

  public void setSchedule(ScheduleDTO schedule) {
    this.schedule = schedule;
  }

  public KafkaDTO getKafka() {
    return kafka;
  }

  public void setKafka(KafkaDTO kafka) {
    this.kafka = kafka;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * As found in Effective Java, the equals contract cannot be satisfied for
   * inheritance hierarchies that add fields in subclasses. Since this is the
   * main goal of extension of this class, these objects should not be compared.
   * <p/>
   * @param o
   * @return
   */
  @Override
  public final boolean equals(Object o) {
    throw new UnsupportedOperationException(
            "JobConfiguration objects cannot be compared.");
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    if (!Strings.isNullOrEmpty(appName)) {
      obj.set(KEY_APPNAME, appName);
    }
    if (schedule != null) {
      obj.set(KEY_SCHEDULE, schedule);
    }
    if (kafka != null) {
      obj.set(KEY_KAFKA, kafka);
    } 
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    //First: check correctness
    ScheduleDTO sch = null;
    if (json.containsKey(KEY_SCHEDULE)) {
      MutableJsonObject mj = json.getJsonObject(KEY_SCHEDULE);
      try {
        sch = new ScheduleDTO();
        sch.updateFromJson(mj);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
                "Failed to parse JobConfiguration: invalid schedule.", e);
      }
    }
   
    if (json.containsKey(KEY_KAFKA)) {
      MutableJsonObject mj = json.getJsonObject(KEY_KAFKA);
      try {
         KafkaDTO kafkaDTO = new KafkaDTO();
         kafkaDTO.updateFromJson(mj);
         this.kafka = kafkaDTO;
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
                "Failed to parse JobConfiguration: invalid kafka properties.", e);
      }
    } 
    //Then: set values
    this.appName = json.getString(KEY_APPNAME, null);
    this.schedule = sch;
    
  }

  public static class JobConfigurationFactory {

    public static JobConfiguration getJobConfigurationFromJson(
            MutableJsonObject object)
            throws IllegalArgumentException {
      //First: check if null
      if (object == null) {
        throw new NullPointerException(
                "Cannot get a JobConfiguration object from null.");
      }
      //Get the type
      String jType = object.getString("type");
      JobType type = JobType.valueOf(jType);
      JobConfiguration conf;
      switch (type) {
        case ADAM:
          conf = new AdamJobConfiguration();
          break;
        case SPARK:
          conf = new SparkJobConfiguration();
          break;
        case FLINK:
          conf = new FlinkJobConfiguration();
          break;
        case ERASURE_CODING:
          conf = new ErasureCodeJobConfiguration();
          break;
        default:
          throw new UnsupportedOperationException(
                  "The given jobtype is not recognized by this factory.");
      }
      //Update the object
      conf.updateFromJson(object);
      return conf;
    }

    /**
     * Get a new JobConfiguration object with the given type.
     * <p/>
     * @param type
     * @return
     */
    public static JobConfiguration getJobConfigurationTemplate(JobType type) {
      JobConfiguration conf;
      switch (type) {
        case ADAM:
          conf = new AdamJobConfiguration();
          break;
        case SPARK:
          conf = new SparkJobConfiguration();
          break;
        case FLINK:
          conf = new FlinkJobConfiguration();
          break;
        case ERASURE_CODING:
          conf = new ErasureCodeJobConfiguration();
          break;
        default:
          throw new UnsupportedOperationException(
                  "The given jobtype is not recognized by this factory.");
      }
      return conf;
    }

    public static Set<JobType> getSupportedTypes() {
      return EnumSet.of(JobType.ADAM,
              JobType.SPARK,
              JobType.FLINK,
              JobType.ERASURE_CODING);
    }
  }
}
