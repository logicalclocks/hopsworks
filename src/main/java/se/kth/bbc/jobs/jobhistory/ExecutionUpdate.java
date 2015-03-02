package se.kth.bbc.jobs.jobhistory;

import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;

/**
 *
 * @author stig
 */
@PushEndpoint("/{study}")
public class ExecutionUpdate {
  
  @PathParam("study")
  private String study;
  
  @OnMessage
  public String onMessage(String jobid){
    return jobid;
  }
}
