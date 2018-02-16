/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
