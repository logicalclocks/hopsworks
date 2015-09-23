package se.kth.bbc.fileoperations;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;

/**
 *
 * @author vangelis
 */
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
