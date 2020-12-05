/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.cloud;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;

@XmlRootElement
public class AWSClusterCredentials {
  private String dbUser;
  private String dbPassword;
  private Instant expiration;
  
  public AWSClusterCredentials(String dbUser, String dbPassword, Instant expiration) {
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
    this.expiration = expiration;
  }
  
  public String getDbUser() {
    return dbUser;
  }
  
  public void setDbUser(String dbUser) {
    this.dbUser = dbUser;
  }
  
  public String getDbPassword() {
    return dbPassword;
  }
  
  public void setDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
  }
  
  public Instant getExpiration() {
    return expiration;
  }
  
  public void setExpiration(Instant expiration) {
    this.expiration = expiration;
  }
}
