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

package io.hops.hopsworks.admin.user.profile;

import io.hops.hopsworks.admin.maintenance.ClientSessionState;
import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.admin.user.administration.AuditedUserAdministration;
import io.hops.hopsworks.common.dao.user.UserFacade;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.Userlogins;
import io.hops.hopsworks.common.user.UsersController;

@ManagedBean
@ViewScoped
public class ProfileManager implements Serializable {

  public static final String DEFAULT_GRAVATAR = "resources/images/icons/default-icon.jpg";

  private static final long serialVersionUID = 1L;
  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;
  @EJB
  private AccountAuditFacade auditManager;
  @EJB
  private AuditedUserAdministration auditedUserAdministration;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  private Users user;
  private Address address;
  private Userlogins login;
  private Organization organization;

  private boolean editable;

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Userlogins getLogin() {
    return login;
  }

  public void setLogin(Userlogins login) {
    this.login = login;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Address getAddress() {
    return address;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public Users getUser() {
    if (user == null) {
      user = userFacade.findByEmail(sessionState.getLoggedInUsername());
      address = user.getAddress();
      organization = user.getOrganization();
      login = auditManager.getLastUserLogin(user);
    }

    return user;
  }

  public List<String> getCurrentGroups() {
    List<String> list = usersController.getUserRoles(user);
    return list;
  }

  public void updateUserInfo() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
        getCurrentInstance().getExternalContext().getRequest();
    try {
      auditedUserAdministration.updateProfile(user, httpServletRequest);
      MessagesController.addInfoMessage("Success", "Profile updated successfully.");
    } catch (RuntimeException ex) {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);
      return;
    }
  }

  /**
   * Update organization info.
   */
  public void updateUserOrg() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
        getCurrentInstance().getExternalContext().getRequest();
    try {
      user.setOrganization(organization);
      auditedUserAdministration.updateProfile(user, httpServletRequest);
      MessagesController.addInfoMessage("Success", "Profile updated successfully.");
    } catch (RuntimeException ex) {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);
    }
  }

  /**
   * Update the user address in the profile and register the audit logs.
   */
  public void updateAddress() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
        getCurrentInstance().getExternalContext().getRequest();
    try {
      user.setAddress(address);
      auditedUserAdministration.updateProfile(user, httpServletRequest);
      MessagesController.addInfoMessage("Success", "Address updated successfully.");
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Update failed.");
    }
  }

}
