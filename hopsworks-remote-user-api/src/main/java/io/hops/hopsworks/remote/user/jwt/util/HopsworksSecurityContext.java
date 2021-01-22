/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.jwt.util;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class HopsworksSecurityContext implements SecurityContext {
  
  private final String scheme;
  private final Subject subject;
  
  public HopsworksSecurityContext(Subject subject, String scheme) {
    this.scheme = scheme;
    this.subject = subject;
  }
  
  @Override
  public Principal getUserPrincipal() {
    if (this.subject == null) {
      return null;
    }
    return () -> this.subject.getName();
  }
  
  @Override
  public boolean isUserInRole(String role) {
    if (this.subject.getRoles() != null && !this.subject.getRoles().isEmpty()) {
      return this.subject.getRoles().contains(role);
    }
    return false;
  }
  
  @Override
  public boolean isSecure() {
    return "https".equals(this.scheme);
  }
  
  @Override
  public String getAuthenticationScheme() {
    return SecurityContext.BASIC_AUTH;
  }
}
