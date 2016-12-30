package io.hops.hopsworks.common.jobs.configuration;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import java.util.EnumSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.adam.AdamJobConfiguration;
import io.hops.hopsworks.common.jobs.erasureCode.ErasureCodeJobConfiguration;
import io.hops.hopsworks.common.jobs.flink.FlinkJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;

/**
 * Represents the persistable configuration of a runnable job. To be persisted
 * as JSON, the getReducedJsonObject() method is called.
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
    } else {
      kafka = new KafkaDTO();
      kafka.setSelected(false);
      obj.set(KEY_KAFKA, new KafkaDTO());
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
    KafkaDTO kafkaDTO = new KafkaDTO();
    kafkaDTO.setSelected(false);
    if (json.containsKey(KEY_KAFKA)) {
      MutableJsonObject mj = json.getJsonObject(KEY_KAFKA);
      try {
        kafkaDTO.updateFromJson(mj);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
                "Failed to parse JobConfiguration: invalid kafka properties.", e);
      }
    }
    //Then: set values
    this.appName = json.getString(KEY_APPNAME, null);
    this.schedule = sch;
    this.kafka = kafkaDTO;
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
