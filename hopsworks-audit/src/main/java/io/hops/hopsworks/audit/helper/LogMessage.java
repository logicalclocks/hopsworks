/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.audit.helper;

public class LogMessage {
  private String className;
  private String methodName;
  private String parameters;
  private String outcome;
  private CallerIdentifier caller;
  private String clientIp;
  private String userAgent;
  private String pathInfo;
  private String dateTime;
  
  public LogMessage() {
  }
  
  public String getClassName() {
    return className;
  }
  
  public void setClassName(String className) {
    this.className = className;
  }
  
  public String getMethodName() {
    return methodName;
  }
  
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }
  
  public String getOutcome() {
    return outcome;
  }
  
  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }
  
  public CallerIdentifier getCaller() {
    return caller;
  }
  
  public void setCaller(CallerIdentifier caller) {
    this.caller = caller;
  }
  
  public String getClientIp() {
    return clientIp;
  }
  
  public void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }
  
  public String getUserAgent() {
    return userAgent;
  }
  
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
  
  public String getParameters() {
    return parameters;
  }
  
  public void setParameters(String parameters) {
    this.parameters = parameters;
  }
  
  public String getDateTime() {
    return dateTime;
  }
  
  public void setDateTime(String dateTime) {
    this.dateTime = dateTime;
  }
  
  public String getPathInfo() {
    return pathInfo;
  }
  
  public void setPathInfo(String pathInfo) {
    this.pathInfo = pathInfo;
  }
}
