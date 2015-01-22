package se.kth.bbc.jobs.jobhistory;

import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PushEndpoint;

/**
 *
 * @author stig
 */
@PushEndpoint
public class ExecutionUpdate {
  
  @OnMessage
  public String onMessage(String jobid){
    return jobid;
  }
}
