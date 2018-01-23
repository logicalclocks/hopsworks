package io.hops.hopsworks.api.util;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AuthStatus {
  
  private String twofactor;
  private String ldap;

  public AuthStatus() {
  }

  public AuthStatus(String twofactor, String ldap) {
    this.twofactor = twofactor;
    this.ldap = ldap;
  }

  public String getTwofactor() {
    return twofactor;
  }

  public void setTwofactor(String twofactor) {
    this.twofactor = twofactor;
  }

  public String getLdap() {
    return ldap;
  }

  public void setLdap(String ldap) {
    this.ldap = ldap;
  }
  
}
