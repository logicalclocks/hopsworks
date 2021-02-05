/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.admin.remote.user.ldap.LdapConfigHelper;
import io.hops.hopsworks.admin.user.administration.AuditedUserAdministration;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.user.UserValidator;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;
import org.primefaces.context.RequestContext;
import org.primefaces.extensions.event.ClipboardErrorEvent;
import org.primefaces.extensions.event.ClipboardSuccessEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@SessionScoped
public class Register implements Serializable {
  private static final Logger LOGGER = Logger.getLogger(Register.class.getName());
  
  private String uuid;
  private String email;
  private String firstName;
  private String lastName;
  private String role;
  private Integer status;
  private Integer maxNumProjects;
  private Integer remoteUserType;
  private Integer accountType;
  private String password;
  
  private boolean remoteAuthEnabled;
  
  private List<String> roles;
  private List<SelectItem> availableStatus;
  private List<SelectItem> accountTypes;
  private List<SelectItem> remoteUserTypes;
  
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private Settings settings;
  @EJB
  private LdapConfigHelper ldapConfigHelper;
  @EJB
  private AuditedUserAdministration auditedUserAdministration;
  @EJB
  private SecurityUtils securityUtils;
  
  @PostConstruct
  public void init() {
    this.roles = new ArrayList<>();
    this.availableStatus = new ArrayList<>();
    this.accountTypes = new ArrayList<>();
    this.remoteUserTypes = new ArrayList<>();
    
    for (RemoteUserType rt : RemoteUserType.values()) {
      if (RemoteUserType.OAUTH2.equals(rt)) {
        continue;//OAuth2 not supported
      }
      this.remoteUserTypes.add(new SelectItem(rt.getValue(), rt.toString()));
    }
    
    for (UserAccountType t : UserAccountType.values()) {
      this.accountTypes.add(new SelectItem(t.getValue(), t.toString()));
    }
    
    for (UserAccountStatus p : UserAccountStatus.values()) {
      this.availableStatus.add(new SelectItem(p.getValue(), p.getUserStatus()));
    }
    
    for (BbcGroup value : bbcGroupFacade.findAll()) {
      this.roles.add(value.getGroupName());
    }
    resetDefault();
  }
  
  private void resetDefault() {
    setAccountType(UserAccountType.M_ACCOUNT_TYPE.getValue());
    setRole(Settings.DEFAULT_ROLE);
    setStatus(UserAccountStatus.TEMP_PASSWORD.getValue());
    setMaxNumProjects(settings.getMaxNumProjPerUser());
    this.remoteAuthEnabled = settings.isKrbEnabled() || settings.isLdapEnabled();
  }
  
  private void restAll() {
    this.uuid = null;
    this.email = null;
    this.firstName = null;
    this.lastName = null;
    this.role = null;
    this.role = Settings.DEFAULT_ROLE;
    this.status = UserAccountStatus.TEMP_PASSWORD.getValue();
    this.maxNumProjects = settings.getMaxNumProjPerUser();
  }
  
  public String getUuid() {
    return uuid;
  }
  
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(String email) {
    this.email = email;
  }
  
  public String getFirstName() {
    return firstName;
  }
  
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  
  public String getLastName() {
    return lastName;
  }
  
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
  
  public Integer getStatus() {
    return status;
  }
  
  public void setStatus(Integer status) {
    this.status = status;
  }
  
  public Integer getMaxNumProjects() {
    return maxNumProjects;
  }
  
  public void setMaxNumProjects(Integer maxNumProjects) {
    this.maxNumProjects = maxNumProjects;
  }
  
  public Integer getAccountType() {
    return accountType;
  }
  
  public void setAccountType(Integer accountType) {
    this.accountType = accountType;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  public List<String> getRoles() {
    return roles;
  }
  
  public void setRoles(List<String> roles) {
    this.roles = roles;
  }
  
  public List<SelectItem> getAvailableStatus() {
    return availableStatus;
  }
  
  public void setAvailableStatus(List<SelectItem> availableStatus) {
    this.availableStatus = availableStatus;
  }
  
  public List<SelectItem> getAccountTypes() {
    return accountTypes;
  }
  
  public void setAccountTypes(List<SelectItem> accountTypes) {
    this.accountTypes = accountTypes;
  }
  
  public Integer getRemoteUserType() {
    return remoteUserType;
  }
  
  public void setRemoteUserType(Integer remoteUserType) {
    this.remoteUserType = remoteUserType;
  }
  
  public List<SelectItem> getRemoteUserTypes() {
    return remoteUserTypes;
  }
  
  public void setRemoteUserTypes(List<SelectItem> remoteUserTypes) {
    this.remoteUserTypes = remoteUserTypes;
  }
  
  public boolean isRemoteAuthEnabled() {
    return remoteAuthEnabled;
  }
  
  public void setRemoteAuthEnabled(boolean remoteAuthEnabled) {
    this.remoteAuthEnabled = remoteAuthEnabled;
  }
  
  public void save() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
      .getRequest();
    if (accountType == UserAccountType.M_ACCOUNT_TYPE.getValue()) {
      createUser(httpServletRequest);
    } else {
      createRemoteUser(httpServletRequest);
    }
  }
  
  private void createUser(HttpServletRequest httpServletRequest) {
    try {
      UserDTO newUser = new UserDTO();
      newUser.setEmail(this.email);
      newUser.setFirstName(this.firstName);
      newUser.setLastName(this.lastName);
      newUser.setMaxNumProjects(this.maxNumProjects);
      this.password = securityUtils.generateRandomString(UserValidator.TEMP_PASSWORD_LENGTH);
      newUser.setChosenPassword(this.password);
      newUser.setRepeatedPassword(this.password);
      auditedUserAdministration.createUser(newUser, this.role, UserAccountStatus.fromValue(status),
        UserAccountType.M_ACCOUNT_TYPE, httpServletRequest);
      showDialog();
      restAll();
      MessagesController.addInfoMessage("Success", "User created.", "msg");
    } catch (UserException e) {
      showErrorMsg("Failed to create user.", e);
    }
  }
  
  private void createRemoteUser(HttpServletRequest httpServletRequest) {
    try {
      RemoteUserDTO userDTO = ldapConfigHelper.getLdapHelper().getRemoteUserByUuid(this.uuid);
      if (email == null || email.isEmpty()) {
        email = userDTO.getEmail().isEmpty()? null : userDTO.getEmail().get(0);// email is needed for audit
      }
      if (email == null || email.isEmpty()) {
        throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Email not provided.");
      }
      auditedUserAdministration.createRemoteUser(userDTO, email, firstName, lastName,
        UserAccountStatus.fromValue(status), RemoteUserType.fromValue(remoteUserType), httpServletRequest);
      restAll();
      MessagesController.addInfoMessage("Success", "User created.", "msg");
    } catch (UserException | GenericException e) {
      showErrorMsg("Create remote user failed.", e);
    }
  }
  
  public void successListener(final ClipboardSuccessEvent successEvent) {
    MessagesController.addInfoMessage("Success", " Copied to clipboard text: " + successEvent.getText());
  }
  
  public void errorListener(final ClipboardErrorEvent errorEvent) {
    MessagesController.addErrorMessage("Error ", errorEvent.getAction());
  }
  
  public void showDialog() {
    RequestContext context = RequestContext.getCurrentInstance();
    context.execute("PF('dlg1').show();");
  }
  
  private void showErrorMsg(String msg, RESTException e) {
    String errorMsg = e.getUsrMsg() != null? e.getUsrMsg() : e.getErrorCode().getMessage();
    MessagesController.addErrorMessage(msg, errorMsg, "msg");
    LOGGER.log(Level.SEVERE, msg + " {0}", errorMsg);
  }
}
