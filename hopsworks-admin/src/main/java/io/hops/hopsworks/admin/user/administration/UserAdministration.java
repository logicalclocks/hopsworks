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

package io.hops.hopsworks.admin.user.administration;

import io.hops.hopsworks.admin.maintenance.ClientSessionState;
import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.Userlogins;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.FormatUtils;
import org.elasticsearch.common.Strings;

import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class UserAdministration implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(UserAdministration.class.getName());

  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;

  @EJB
  private AccountAuditFacade auditManager;

  @EJB
  private BbcGroupFacade bbcGroupFacade;

  @EJB
  private EmailBean emailBean;

  @ManagedProperty("#{clientSessionState}")
  private ClientSessionState sessionState;
  @ManagedProperty("#{param.userMail}")
  private String userMail;

  private String secAnswer;

  private List<Users> filteredUsers;
  private List<Users> selectedUsers;

  // All verified users
  private List<Users> allUsers;

  // Accounts waiting to be validated by the email owner
  private List<Users> spamUsers;

  // for modifying user roles and status
  private Users editingUser;

  // for mobile users activation
  private List<Users> requests;

  // for user activation
  private List<Users> yRequests;

  // to remove an existing group
  private String role;

  // to assign a new status
  private String selectedStatus;

  // to assign a new group
  private String nGroup;

  List<String> status;

  // all groups
  List<String> groups;

  // all existing groups belong tp
  List<String> cGroups;

  // all possible new groups user doesnt belong to
  List<String> nGroups;

  // current status of the editing user
  private String eStatus;

  // list of roles that can be activated for a user
  List<String> actGroups;

  public UserAdministration() {
    // Default no-arg constructor
  }

  public String getUserMail() {
    return userMail;
  }

  public void setUserMail(String userMail) {
    this.userMail = userMail;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String geteStatus() {
    if (this.editingUser == null) {
      return "";
    }

    this.eStatus
        = this.editingUser.getStatusName();
    return this.eStatus;
  }

  public void seteStatus(String eStatus) {
    this.eStatus = eStatus;
  }

  public String getnGroup() {
    return nGroup;
  }

  public void setnGroup(String nGroup) {
    this.nGroup = nGroup;
  }

  public Users getEditingUser() {
    return editingUser;
  }

  public void setEditingUser(Users editingUser) {
    this.editingUser = editingUser;
  }

  public List<Users> getyRequests() {
    return yRequests;
  }

  public void setyRequests(List<Users> yRequests) {
    this.yRequests = yRequests;
  }

  public List<String> getUserRole(Users p) {
    List<String> list = usersController.getUserRoles(p);
    return list;
  }

  public String getChanged_Status(Users p) {
    return userFacade.findByEmail(p.getEmail()).getStatusName();
  }

  public String accountTypeStr(Users u) {
    switch (u.getMode()) {
      case M_ACCOUNT_TYPE:
        return "Mobile Account";
      case LDAP_ACCOUNT_TYPE:
        return "LDAP Account";
      default:
        return "Unknown Account type";
    }
  }

  public List<String> getActGroups() {
    return actGroups;
  }

  public void setActGroups(List<String> actGroups) {

    this.actGroups = actGroups;
  }

//  public Users getUser() {
//    return user;
//  }
//
//  public void setUser(Users user) {
//    this.user = user;
//  }
  /**
   * Filter the current groups
   *
   * @return
   */
  public List<String> getcGroups() {
    List<String> list = getUserRole(editingUser);
    return list;
  }

  public void setcGroups(List<String> cGroups) {
    this.cGroups = cGroups;
  }

  public List<String> getnGroups() {
    List<String> list = getUserRole(editingUser);
    List<String> tmp = new ArrayList<>();

    for (BbcGroup b : bbcGroupFacade.findAll()) {
      if (!list.contains(b.getGroupName())) {
        tmp.add(b.getGroupName());
      }
    }
    return tmp;
  }

  public void setnGroups(List<String> nGroups) {
    this.nGroups = nGroups;
  }

  public String getSelectedStatus() {
    return selectedStatus;
  }

  public void setSelectedStatus(String selectedStatus) {
    this.selectedStatus = selectedStatus;
  }

  @PostConstruct
  public void initGroups() {
    groups = new ArrayList<>();
    status = getStatus();
    actGroups = new ArrayList<>();
    spamUsers = new ArrayList<>();
    for (BbcGroup b : bbcGroupFacade.findAll()) {
      groups.add(b.getGroupName());
      actGroups.add(b.getGroupName());
    }
  }

  public List<String> getStatus() {

    this.status = new ArrayList<>();

    for (UserAccountStatus p : UserAccountStatus.values()) {
      status.add(p.name());
    }

    return status;
  }

  public void setStatus(List<String> status) {
    this.status = status;
  }

  public void setFilteredUsers(List<Users> filteredUsers) {
    this.filteredUsers = filteredUsers;
  }

  public List<Users> getFilteredUsers() {
    return filteredUsers;
  }

  /*
   * Find all registered users
   */
  public List<Users> getAllUsers() {
    allUsers = userFacade.findAllUsers();
    return allUsers;
  }


  public List<String> getGroups() {
    return groups;
  }
//
//  public Users getSelectedUser() {
//    return user;
//  }
//
//  public void setSelectedUser(Users user) {
//    this.user = user;
//  }

  /**
   * Reject users that are not validated.
   *
   */
  public void rejectUser() {
  
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    Users user = userFacade.findByEmail(userMail);
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      usersController.changeAccountStatus(user.getUid(), UserAccountStatus.SPAM_ACCOUNT.toString(),
          UserAccountStatus.SPAM_ACCOUNT);
      MessagesController.addInfoMessage(user.getEmail() + " was rejected.");
      spamUsers.add(user);
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Rejection failed. " + ex.
          getMessage());
      LOGGER.log(Level.SEVERE, "Could not reject user.", ex);
    }
    try {
      // Send rejection email
      emailBean.sendEmail(user.getEmail(), RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REJECT,
          UserAccountsEmailMessages.accountRejectedMessage());
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to " + user.getEmail());
      LOGGER.log(Level.SEVERE, "Could not send email to {0}. {1}", new Object[]{user.getEmail(), e});
    }
  }

  /**
   * Removes a user from the db
   * Only works for new not yet activated users
   *
   * @param user
   */
  public void deleteUser(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      usersController.deleteUser(user);
      MessagesController.addInfoMessage(user.getEmail() + " was removed.");
      spamUsers.remove(user);
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Remove failed. " + ex.
          getMessage());
      Logger.getLogger(UserAdministration.class.getName()).log(Level.SEVERE,
          "Could not remove user.", ex);
    }
  }

  /**
   * Remove a user from spam list and set the users status to new mobile user.
   *
   * @param user
   */
  public void removeFromSpam(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      usersController.changeAccountStatus(user.getUid(), "",
          UserAccountStatus.NEW_MOBILE_ACCOUNT);
      MessagesController.addInfoMessage(user.getEmail()
          + " was removed from spam list.");
      spamUsers.remove(user);
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Remove failed. " + ex.
          getMessage());
      Logger.getLogger(UserAdministration.class.getName()).log(Level.SEVERE,
          "Could not remove user from spam list.", ex);
    }
  }

  public void confirmMessage(ActionEvent actionEvent) {

    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
        "Deletion Successful!", null);
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public String getLoginName() throws IOException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.
        getExternalContext().getRequest();

    Principal principal = request.getUserPrincipal();

    try {
      Users p = userFacade.findByEmail(principal.getName());
      if (p != null) {
        return p.getFname() + " " + p.getLname();
      } else {
        return principal.getName();
      }
    } catch (Exception ex) {
      ExternalContext extContext = FacesContext.getCurrentInstance().
          getExternalContext();
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  /**
   * Get all open user requests (mobile or simple accounts).
   *
   * @return
   */
  public List<Users> getAllRequests() {
    if (requests == null) {
      requests = userFacade.findAllMobileRequests();
    }
    return requests;
  }

  public List<Users> getSelectedUsers() {
    return selectedUsers;
  }

  public void setSelectedUsers(List<Users> users) {
    this.selectedUsers = users;
  }

  public void activateUser() {
    Users user = userFacade.findByEmail(userMail);
    if (user == null) {
      MessagesController.addSecurityErrorMessage("User is null.");
      return;
    }
    if (role == null || role.isEmpty()) {
      role = "HOPS_USER";
    }

    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
        getCurrentInstance().getExternalContext().getRequest();
    String message = usersController.activateUser(role, user, sessionState.getLoggedInUser(), httpServletRequest);
    if(Strings.isNullOrEmpty(message)){
      MessagesController.addInfoMessage("User "+user.getEmail()+ " activated successfully", message);
    } else {
      MessagesController.addSecurityErrorMessage(message);
    }
    try {
      //send confirmation email
      emailBean.sendEmail(user.getEmail(), RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
          UserAccountsEmailMessages.
              accountActivatedMessage(user.getEmail()));
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to "
          + user.getEmail() + ". " + e.getMessage());
      Logger.getLogger(UserAdministration.class.getName()).log(Level.SEVERE,
          "Could not send email to {0}. {1}", new Object[]{user.getEmail(),e});
    }

    requests.remove(user);
  }

  public boolean notVerified(Users user) {
    if (user == null
        || user.getBbcGroupCollection() == null
        || !user.getBbcGroupCollection().isEmpty()
        || user.getStatus().equals(UserAccountStatus.VERIFIED_ACCOUNT)) {
      return false;
    }
    return true;
  }

  public void resendAccountVerificationEmail() throws
      MessagingException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    Users user = userFacade.findByEmail(userMail);
    String activationKey = SecurityUtils.getRandomPassword(64);
    emailBean.sendEmail(user.getEmail(), RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
        UserAccountsEmailMessages.buildMobileRequestMessage(FormatUtils.getUserURL(request), user.getUsername()
            + activationKey));
    user.setValidationKey(activationKey);
    userFacade.update(user);
    MessagesController.addInfoMessage("Account verification Email Successfully Resent");

  }

  public String modifyUser() {
    Users user1 = userFacade.findByEmail(userMail);
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("editinguser", user1);

    Userlogins login = auditManager.getLastUserLogin(user1);
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("editinguser_logins", login);

    MessagesController.addInfoMessage("User successfully modified for " + user1.getEmail());
    return "admin_profile";
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

  public List<Users> getSpamUsers() {
    return spamUsers = userFacade.findAllByStatus(UserAccountStatus.SPAM_ACCOUNT);
  }

  public void setSpamUsers(List<Users> spamUsers) {
    this.spamUsers = spamUsers;
  }

  public String getSecAnswer() {
    return secAnswer;
  }

  public void setSecAnswer(String secAnswer) {
    this.secAnswer = secAnswer;
  }

  public SecurityQuestion[] getQuestions() {
    return SecurityQuestion.values();
  }

  public ClientSessionState getSessionState() {
    return sessionState;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

}
