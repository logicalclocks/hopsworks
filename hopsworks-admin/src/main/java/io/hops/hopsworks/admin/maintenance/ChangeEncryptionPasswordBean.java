/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;

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
  private LoggedMaintenanceHelper loggedMaintenanceHelper;
  
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
      loggedMaintenanceHelper.changeMasterEncryptionPassword(currentPassword, newPassword, request);
      MessagesController.addInfoMessage("Changing password...", "Check your Inbox for completion status");
    } catch (EncryptionMasterPasswordException ex) {
      MessagesController.addErrorMessage(ex.getMessage());
    } catch (IOException ex) {
      MessagesController.addErrorMessage("Error while reading master password file!");
    }
  }
}
