/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.security.CertificatesMgmService;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

@ManagedBean(name = "masterEncPassBean")
@ViewScoped
public class ChangeEncryptionPasswordBean implements Serializable {
  private static final Logger LOG = Logger.getLogger(ChangeEncryptionPasswordBean.class.getName());
  
  @EJB
  private CertificatesMgmService certificatesMgmService;
  
  private String currentPassword;
  private String newPassword;
  private String retypePassword;
  
  public ChangeEncryptionPasswordBean() {
  }
  
  public String getCurrentPassword() {
    return currentPassword;
  }
  
  public void setCurrentPassword(String oldPassword) {
    this.currentPassword = oldPassword;
  }
  
  public String getNewPassword() {
    return newPassword;
  }
  
  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
  
  public String getRetypePassword() {
    return retypePassword;
  }
  
  public void setRetypePassword(String retypePassword) {
    this.retypePassword = retypePassword;
  }
  
  public void changeMasterEncryptionPassword() {
    try {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
      String userEmail = request.getUserPrincipal().getName();
      certificatesMgmService.checkPassword(currentPassword, userEmail);
      certificatesMgmService.resetMasterEncryptionPassword(newPassword, userEmail);
      MessagesController.addInfoMessage("Changing password...", "Check your Inbox for completion status");
    } catch (EncryptionMasterPasswordException ex) {
      MessagesController.addErrorMessage(ex.getMessage());
    } catch (IOException ex) {
      MessagesController.addErrorMessage("Error while reading master password file!");
    }
  }
}
