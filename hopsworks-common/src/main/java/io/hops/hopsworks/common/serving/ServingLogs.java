package io.hops.hopsworks.common.serving;

public class ServingLogs {
  private String instanceName;
  private String content;
  
  public ServingLogs() { }
  
  public ServingLogs(String instanceName, String content) {
    this.instanceName = instanceName;
    this.content = content;
  }
  
  public String getInstanceName() { return instanceName; }
  public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
  
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
}
