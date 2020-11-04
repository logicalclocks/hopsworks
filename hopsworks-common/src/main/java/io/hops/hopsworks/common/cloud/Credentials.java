/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.cloud;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Credentials {
  private String accessKeyId;
  private String secretAccessKey;
  private String sessionToken;
  
  public Credentials() {
  }
  
  public Credentials(String accessKeyId, String secretAccessKey, String sessionToken) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.sessionToken = sessionToken;
  }
  
  public String getAccessKeyId() {
    return accessKeyId;
  }
  
  public void setAccessKeyId(String accessKeyId) {
    this.accessKeyId = accessKeyId;
  }
  
  public String getSecretAccessKey() {
    return secretAccessKey;
  }
  
  public void setSecretAccessKey(String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
  }
  
  public String getSessionToken() {
    return sessionToken;
  }
  
  public void setSessionToken(String sessionToken) {
    this.sessionToken = sessionToken;
  }
}
