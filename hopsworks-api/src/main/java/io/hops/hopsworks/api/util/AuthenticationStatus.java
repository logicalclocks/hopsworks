/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.util;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class AuthenticationStatus {
  private OTPAuthStatus otpAuthStatus;
  private boolean ldapEnabled;
  private boolean krbEnabled;
  private boolean oauthEnabled;
  private List<OpenIdProvider> openIdProviders;
  private boolean remoteAuthEnabled;
  private boolean loginDisabled;
  private boolean registerDisabled;
  private String loginPageOverwrite;
  private boolean ldapGroupMappingEnabled;
  private boolean oauthGroupMappingEnabled;
  
  public AuthenticationStatus() {
  }
  
  public AuthenticationStatus(OTPAuthStatus otpAuthStatus, boolean ldapEnabled, boolean krbEnabled,
    boolean oauthEnabled, List<OpenIdProvider> openIdProviders, boolean remoteAuthEnabled, boolean loginDisabled,
    boolean registerDisabled, String loginPageOverwrite, boolean ldapGroupMappingEnabled,
    boolean oauthGroupMappingEnabled) {
    this.otpAuthStatus = otpAuthStatus;
    this.ldapEnabled = ldapEnabled;
    this.krbEnabled = krbEnabled;
    this.oauthEnabled = oauthEnabled;
    this.openIdProviders = openIdProviders;
    this.remoteAuthEnabled = remoteAuthEnabled;
    this.loginDisabled = loginDisabled;
    this.registerDisabled = registerDisabled;
    this.loginPageOverwrite = loginPageOverwrite;
    this.ldapGroupMappingEnabled = ldapGroupMappingEnabled;
    this.oauthGroupMappingEnabled = oauthGroupMappingEnabled;
  }
  
  public OTPAuthStatus getOtpAuthStatus() {
    return otpAuthStatus;
  }
  
  public void setOtpAuthStatus(OTPAuthStatus otpAuthStatus) {
    this.otpAuthStatus = otpAuthStatus;
  }
  
  public boolean isLdapEnabled() {
    return ldapEnabled;
  }
  
  public void setLdapEnabled(boolean ldapEnabled) {
    this.ldapEnabled = ldapEnabled;
  }
  
  public boolean isKrbEnabled() {
    return krbEnabled;
  }
  
  public void setKrbEnabled(boolean krbEnabled) {
    this.krbEnabled = krbEnabled;
  }
  
  public boolean isOauthEnabled() {
    return oauthEnabled;
  }
  
  public void setOauthEnabled(boolean oauthEnabled) {
    this.oauthEnabled = oauthEnabled;
  }
  
  public List<OpenIdProvider> getOpenIdProviders() {
    return openIdProviders;
  }
  
  public void setOpenIdProviders(List<OpenIdProvider> openIdProviders) {
    this.openIdProviders = openIdProviders;
  }
  
  public boolean isRemoteAuthEnabled() {
    return remoteAuthEnabled;
  }
  
  public void setRemoteAuthEnabled(boolean remoteAuthEnabled) {
    this.remoteAuthEnabled = remoteAuthEnabled;
  }
  
  public boolean isLoginDisabled() {
    return loginDisabled;
  }
  
  public void setLoginDisabled(boolean loginDisabled) {
    this.loginDisabled = loginDisabled;
  }
  
  public boolean isRegisterDisabled() {
    return registerDisabled;
  }
  
  public void setRegisterDisabled(boolean registerDisabled) {
    this.registerDisabled = registerDisabled;
  }

  public String getLoginPageOverwrite() {
    return loginPageOverwrite;
  }

  public void setLoginPageOverwrite(String loginPageOverwrite) {
    this.loginPageOverwrite = loginPageOverwrite;
  }
  
  public boolean isLdapGroupMappingEnabled() {
    return ldapGroupMappingEnabled;
  }
  
  public void setLdapGroupMappingEnabled(boolean ldapGroupMappingEnabled) {
    this.ldapGroupMappingEnabled = ldapGroupMappingEnabled;
  }
  
  public boolean isOauthGroupMappingEnabled() {
    return oauthGroupMappingEnabled;
  }
  
  public void setOauthGroupMappingEnabled(boolean oauthGroupMappingEnabled) {
    this.oauthGroupMappingEnabled = oauthGroupMappingEnabled;
  }
}
