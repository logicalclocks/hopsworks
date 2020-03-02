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
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.Address;
import io.hops.hopsworks.persistence.entity.user.security.audit.Userlogins;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import org.primefaces.context.RequestContext;
import org.primefaces.extensions.event.ClipboardErrorEvent;
import org.primefaces.extensions.event.ClipboardSuccessEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class AdminProfileAdministration implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(AdminProfileAdministration.class.getName());

  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private AccountAuditFacade auditManager;
  @EJB
  private AuditedUserAdministration auditedUserAdministration;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  private Users user;
  // for modifying user roles and status
  private Users editingUser;

  // to remove an existing group
  private String selectedGroup;

  // to assign a new stauts
  private String selectedStatus;

  // to assign a new group
  private String newGroup;

  // all groups
  List<String> groups;

  // all existing groups belong tp
  List<String> currentGroups;

  // all possible new groups user doesnt belong to
  List<String> newGroups;

  // current status of the editing user
  private String editStatus;

  List<String> status;

  private Userlogins login;

  private Address address;
  
  private String newPassword;

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Userlogins getLogin() {
    return login;
  }

  public void setLogin(Userlogins login) {
    this.login = login;
  }

  public void setEditStatus(String editStatus) {
    this.editStatus = editStatus;
  }

  public String getNew_group() {
    return newGroup;
  }

  public void setNew_group(String new_group) {
    this.newGroup = new_group;
  }

  public Users getEditingUser() {
    return editingUser;
  }

  public void setEditingUser(Users editingUser) {
    this.editingUser = editingUser;
  }

  public String accountTypeStr() {
    return this.editingUser.getMode().getAccountType();
  }

  public List<String> getUserRole(Users p) {
    List<String> list = usersController.getUserRoles(p);
    return list;
  }

  public String getChangedStatus(Users p) {
    return userFacade.findByEmail(p.getEmail()).getStatus().name();
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public void setNewGroups(List<String> newGroups) {
    this.newGroups = newGroups;
  }

  public String getSelectedStatus() {
    return selectedStatus;
  }

  public void setSelectedStatus(String selectedStatus) {
    this.selectedStatus = selectedStatus;
  }

  public String getSelectedGroup() {
    return selectedGroup;
  }

  public void setSelectedGroup(String selectedGroup) {
    this.selectedGroup = selectedGroup;
  }
  
  public String getNewPassword() {
    return newPassword;
  }
  
  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
  
  /**
   * Filter the current groups of the user.
   *
   * @return
   */
  public List<String> getCurrentGroups() {
    List<String> list = usersController.getUserRoles(editingUser);
    return list;
  }

  public void setCurrentGroups(List<String> currentGroups) {
    this.currentGroups = currentGroups;
  }

  public List<String> getNewGroups() {
    List<String> list = usersController.getUserRoles(editingUser);
    List<String> tmp = new ArrayList<>();

    for (BbcGroup b : bbcGroupFacade.findAll()) {

      if (!list.contains(b.getGroupName())) {
        tmp.add(b.getGroupName());
      }
    }
    return tmp;
  }

  public String getEditStatus() {

    this.editStatus = userFacade.findByEmail(this.editingUser.getEmail()).getStatus().name();
    return this.editStatus;
  }
  
  public void updateEditingUser() {
    this.editingUser = userFacade.findByEmail(this.editingUser.getEmail());
  }

  @PostConstruct
  public void init() {

    groups = new ArrayList<>();
    status = new ArrayList<>();

    for (BbcGroup value : bbcGroupFacade.findAll()) {
      groups.add(value.getGroupName());
    }

    editingUser = (Users) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("editinguser");
    if (editingUser != null) {
      address = editingUser.getAddress();
      login = (Userlogins) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(
          "editinguser_logins");
    } else {
      String email = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
      editingUser = userFacade.findByEmail(email);
      login = auditManager.getLastUserLogin(editingUser);
    }

  }

  public List<String> getStatus() {

    status = new ArrayList<>();

    for (UserAccountStatus p : UserAccountStatus.values()) {
      status.add(p.name());
    }

    // Remove the inactive users
    status.remove(UserAccountStatus.NEW_MOBILE_ACCOUNT.name());

    return status;
  }

  public void setStatus(List<String> status) {
    this.status = status;
  }

  public List<Users> getUsersNameList() {
    return userFacade.findAllUsers();
  }

  public List<String> getGroups() {
    return groups;
  }

  public Users getSelectedUser() {
    return user;
  }

  public void setSelectedUser(Users user) {
    this.user = user;
  }

  /**
   * Update user roles from profile by admin.
   */
  public void updateStatusByAdmin() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
      .getRequest();
    // Update status
    if (!"#!".equals(selectedStatus)) {
      UserAccountStatus status = UserAccountStatus.valueOf(selectedStatus);
      try {
        auditedUserAdministration.changeStatus(editingUser, status, httpServletRequest);
        MessagesController.addInfoMessage("User " + editingUser.getEmail() + " status updated successfully.");
      } catch (UserException ex) {
        MessagesController.addInfoMessage("Problem Could not update account status.", ex.getMessage());
        LOGGER.log(Level.SEVERE, "Problem Could not update account status.", ex);
      }
    } else {
      MessagesController.addErrorMessage("Error", "No selection made!");

    }

  }

  public void addRoleByAdmin() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().
        getRequest();
    // Register a new group
    if (!"#!".equals(newGroup)) {
      try {
        auditedUserAdministration.addRole(editingUser, newGroup, httpServletRequest);
        MessagesController
          .addInfoMessage("Added role:" + newGroup + " to user " + editingUser.getEmail() + " activated successfully");
      } catch (UserException ue) {
        MessagesController.addSecurityErrorMessage(ue.getMessage());
      }
    } else {
      MessagesController.addErrorMessage("Error", "No selection made!!");
    }

  }

  public void removeRoleByAdmin() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().
        getRequest();
    // Remove a group
    if (!"#!".equals(selectedGroup)) {
      try {
        auditedUserAdministration.removeRole(editingUser, selectedGroup, httpServletRequest);
        MessagesController.addInfoMessage("Removed role:" + selectedGroup + " from user " + editingUser.getEmail() + " "
          + "successfully");
      } catch (UserException ue) {
        MessagesController.addSecurityErrorMessage(ue.getMessage());
      }
    }
    if ("#!".equals(selectedGroup)) {
      if (("#!".equals(selectedStatus)) || "#!".equals(newGroup)) {
        MessagesController.addErrorMessage("Error", "No selection made!");
      }
    }

  }
  
  public String showProfile() {
    String email = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
    Users user1 = userFacade.findByEmail(email);
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("editinguser", user1);
    
    Userlogins login = auditManager.getLastUserLogin(user1);
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("editinguser_logins", login);
    
    MessagesController.addInfoMessage("User successfully modified for " + user1.getEmail());
    return "admin_profile";
  }

  public ClientSessionState getSessionState() {
    return sessionState;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public String getMaxNumProjs() {
    return userFacade.findByEmail(editingUser.getEmail()).getMaxNumProjects().
        toString();
  }

  public void setMaxNumProjs(String maxNumProjs) {
    int num = Integer.parseInt(maxNumProjs);
    int current = userFacade.findByEmail(editingUser.getEmail()).getMaxNumProjects();
    if (num > -1 && current != num) {
      HttpServletRequest httpServletRequest =
        (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
      auditedUserAdministration.setMaxProject(editingUser, num, httpServletRequest);
      MessagesController.addInfoMessage("Updated max projects to  " + num);
    }
  }
  
  public void resetPassword() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().
      getRequest();
    try {
      updateEditingUser();
      newPassword = auditedUserAdministration.resetPassword(editingUser, httpServletRequest);
      MessagesController.addInfoMessage("Password reset", "Password reset for: " + editingUser.getEmail());
      LOGGER.log(Level.INFO, "User: {0} reset password for user: {1}",
        new Object[]{httpServletRequest.getRemoteUser(), editingUser.getEmail()});
      showDialog();
    } catch (UserException e) {
      MessagesController.addErrorMessage("Error resetting password ", e.getMessage());
      LOGGER.log(Level.WARNING, "Error resetting password. User: {0} trying to reset password for user: {1} ",
        new Object[]{httpServletRequest.getRemoteUser(), editingUser.getEmail()});
    } catch (MessagingException e) {
      MessagesController.addErrorMessage("Password reset. Error sending email ", e.getMessage());
      LOGGER.log(Level.WARNING, "Error sending email after user: {0} reset password for user: {1} ",
        new Object[]{httpServletRequest.getRemoteUser(), editingUser.getEmail()});
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
}
