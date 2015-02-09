package se.kth.bbc.jobs.jobhistory;

import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;

/**
 *
 * @author stig
 */
@PushEndpoint("/{study}/{type}")
public class ExecutionUpdate {
  
  @PathParam("study")
  private String study;
  
  @PathParam("type")
  private String type;
  
  @OnMessage
  public String onMessage(String jobid){
    System.out.println("Publishing state change for "+jobid + " to channel /"+study+"/"+type);
    return jobid;
  }
}
