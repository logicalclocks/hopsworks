/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
