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
package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.exceptions.UserException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@RequestScoped
public class RecoverPassword implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(RecoverPassword.class.getName());
  
  private String passwd1;
  private String passwd2;
  private boolean keyError;
  
  @ManagedProperty("#{param.key}")
  private String key;
  
  @EJB
  protected AuditedUserAccountAction auditedUserAccountAction;
  
  @PostConstruct
  public void init() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    try {
      auditedUserAccountAction.checkRecoveryKey(key, req);
      keyError = false;
    } catch (EJBException | IllegalArgumentException e) {
      keyError = true;
      String detail = e.getCause() != null? e.getCause().getMessage() : "Password recovery key validation error.";
      ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid key:", detail));
      LOGGER.log(Level.FINE, detail);
    } catch (UserException ue) {
      keyError = true;
      String detail = ue.getUsrMsg() != null ? ue.getUsrMsg() : ue.getErrorCode().getMessage();
      ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid key:", detail));
      LOGGER.log(Level.FINE, ue.getMessage());
    }
  }
  
  public String getPasswd1() {
    return passwd1;
  }
  
  public void setPasswd1(String passwd1) {
    this.passwd1 = passwd1;
  }
  
  public String getPasswd2() {
    return passwd2;
  }
  
  public void setPasswd2(String passwd2) {
    this.passwd2 = passwd2;
  }
  
  public String getKey() {
    return key;
  }
  
  public void setKey(String key) {
    this.key = key;
  }
  
  public boolean isKeyError() {
    return keyError;
  }
  
  public void setKeyError(boolean keyError) {
    this.keyError = keyError;
  }
  
  /**
   * Change password.
   * <p>
   * @return
   */
  public String changePassword() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    try {
      auditedUserAccountAction.changePassword(key, passwd1, passwd2, req);
    } catch (MessagingException ex) {
      String detail = ex.getCause() != null? ex.getCause().getMessage() : "Failed to send verification email.";
      MessagesController.addSecurityErrorMessage(detail);
      LOGGER.log(Level.FINE, null, detail);
    } catch (UserException ue) {
      ue.getErrorCode().getMessage();
      String detail = ue.getUsrMsg() != null ? ue.getUsrMsg() : ue.getErrorCode().getMessage();
      MessagesController.addSecurityErrorMessage(detail);
      LOGGER.log(Level.FINE, null, detail);
    }
    return ("password_changed");
  }
  
  public String cancel() {
    return ("welcome");
  }
}
