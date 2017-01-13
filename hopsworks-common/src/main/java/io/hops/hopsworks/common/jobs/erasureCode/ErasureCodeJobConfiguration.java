package io.hops.hopsworks.common.jobs.erasureCode;

import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

@XmlRootElement
public class ErasureCodeJobConfiguration extends JobConfiguration {

  private String filePath;

  protected final static String KEY_TYPE = "type";

  public ErasureCodeJobConfiguration() {
    super();
  }

  public ErasureCodeJobConfiguration(String filePath) {
    this();
    this.filePath = filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getFilePath() {
    return this.filePath;
  }

  @Override
  public JobType getType() {
    return JobType.ERASURE_CODING;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = super.getReducedJsonObject();

    //additional key
    obj.set(KEY_TYPE, JobType.ERASURE_CODING.toString());

    return obj;
  }
}
