package se.kth.bbc.jobs.jobhistory;

import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
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
    System.out.println("Publishing state change for "+jobid + " to channel");
    return jobid;
  }
  
  @OnClose
  public void onClose(RemoteEndpoint r){
    System.out.println("Primefaces push connection closed "+r.path());
  }
  
  @OnOpen
  public void onOpen(RemoteEndpoint r){
    System.out.println("Primefaces push connection opened: "+r.path());
  }
}
