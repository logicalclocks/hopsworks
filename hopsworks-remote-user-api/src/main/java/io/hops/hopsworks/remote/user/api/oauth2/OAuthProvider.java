/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.oauth2;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement
public class OAuthProvider {
  private URI loginUrl;
  private String name;
  
  public OAuthProvider() {
  }
  
  public OAuthProvider(URI loginUrl, String name) {
    this.loginUrl = loginUrl;
    this.name = name;
  }
  
  public URI getLoginUrl() {
    return loginUrl;
  }
  
  public void setLoginUrl(URI loginUrl) {
    this.loginUrl = loginUrl;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
}
