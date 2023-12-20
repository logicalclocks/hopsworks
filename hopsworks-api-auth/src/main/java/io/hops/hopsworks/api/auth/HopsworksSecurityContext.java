/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.api.auth;

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
