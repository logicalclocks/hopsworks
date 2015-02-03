package se.kth.bbc.jobs.adam;

import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public final class AdamController extends JobController {
  
  private String jobName, adamCommand;
  private List<String> optionValues;
  
  private AdamCommand selectedCommand = null;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @Override
  protected void afterUploadMainFile(FileUploadEvent event) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void afterUploadExtraFile(FileUploadEvent event) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getPushChannel() {
    return "/" + sessionState.getActiveStudyname() + "/" + JobType.ADAM;
  }

  public void startJob() {
    SparkYarnRunnerBuilder builder = new SparkYarnRunnerBuilder(
            getMainFilePath(), Constants.ADAM_MAINCLASS);
    //Set some ADAM-specific property values
    builder.addSystemVariable("spark.serializer",
            "org.apache.spark.serializer.KryoSerializer");
    builder.addSystemVariable("spark.kryo.registrator",
            "org.bdgenomics.adam.serialization.ADAMKryoRegistrator");
    builder.addSystemVariable("spark.kryoserializer.buffer.mb", "4");
    builder.addSystemVariable("spark.kryo.referenceTracking", "true");
    builder.setExecutorMemoryGB(4);
    
    //Set the job name
    builder.setJobName(jobName);
    
    //Set up AM args
    StringBuilder sb = new StringBuilder();
    sb.append(adamCommand);
    for(String s: optionValues){
      sb.append(" ").append(s);
    }
  }
  
  public AdamCommand[] getAdamCommands(){
    return AdamCommand.values();
  }
  
  public void setSelectedCommand(AdamCommand ac){
    this.selectedCommand = ac;
  }
  
  public AdamCommand getSelectedCommand(){
    return selectedCommand;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }
  
  

}
