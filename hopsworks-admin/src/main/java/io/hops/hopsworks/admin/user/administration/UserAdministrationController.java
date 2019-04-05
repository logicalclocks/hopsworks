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
package io.hops.hopsworks.admin.user.administration;

import com.google.common.base.Strings;
import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.FormatUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserAdministrationController {
  
  private static final Logger LOGGER = Logger.getLogger(UserAdministrationController.class.getName());
  @EJB
  protected UsersController usersController;
  @EJB
  private EmailBean emailBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  
  public String getAccountType(UserAccountType type) {
    switch (type) {
      case M_ACCOUNT_TYPE:
        return "Mobile Account";
      case REMOTE_ACCOUNT_TYPE:
        return "Remote Account";
      default:
        return "Unknown Account type";
    }
  }
  
  public String getUserStatus(UserAccountStatus status) {
    switch (status) {
      case SPAM_ACCOUNT:
        return "Spam account";
      case DEACTIVATED_ACCOUNT:
        return "Deactivated account";
      case VERIFIED_ACCOUNT:
        return "Verified account";
      case ACTIVATED_ACCOUNT:
        return "Activated account";
      case NEW_MOBILE_ACCOUNT:
        return "New account";
      case BLOCKED_ACCOUNT:
        return "Blocked account";
      case LOST_MOBILE:
        return "Lost mobile account";
      default:
        return "Unknown account type";
    }
  }
  
  public void rejectUser(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      usersController.changeAccountStatus(user.getUid(), UserAccountStatus.SPAM_ACCOUNT.toString(),
        UserAccountStatus.SPAM_ACCOUNT);
      MessagesController.addInfoMessage(user.getEmail() + " was rejected.");
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Rejection failed. " + ex.getMessage());
      LOGGER.log(Level.SEVERE, "Could not reject user.", ex);
    }
    try {
      // Send rejection email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REJECT,
        UserAccountsEmailMessages.accountRejectedMessage());
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to " + user.getEmail());
      LOGGER.log(Level.SEVERE, "Could not send email to {0}. {1}", new Object[]{user.getEmail(), e});
    }
  }
  
  public void activateUser(Users user, Users initiator) {
    if (user == null) {
      MessagesController.addSecurityErrorMessage("User is null.");
      return;
    }
    if (user.getGroupName() == null || user.getGroupName().isEmpty()) {
      user.setGroupName("HOPS_USER");
    }
    
    HttpServletRequest httpServletRequest =
      (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    String message = usersController.activateUser(user.getGroupName(), user, initiator, httpServletRequest);
    if (Strings.isNullOrEmpty(message)) {
      MessagesController.addInfoMessage("User " + user.getEmail() + " activated successfully", message);
    } else {
      MessagesController.addSecurityErrorMessage(message);
    }
    try {
      //send confirmation email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
        UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
        UserAccountsEmailMessages.
          accountActivatedMessage(user.getEmail()));
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to " + user.getEmail() + ". " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Could not send email to {0}. {1}", new Object[]{user.getEmail(), e});
    }
  }
  
  public boolean notActivated(Users user) {
    if (user == null || user.getBbcGroupCollection() == null || !user.getBbcGroupCollection().isEmpty() ||
      user.getStatus().equals(UserAccountStatus.VERIFIED_ACCOUNT)) {
      return false;
    }
    return true;
  }
  
  public void resendAccountVerificationEmail(Users user) {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    String activationKey = SecurityUtils.getRandomPassword(64);
    try {
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
        UserAccountsEmailMessages.buildMobileRequestMessage(FormatUtils.getUserURL(request), user.getUsername()
          + activationKey));
      user.setValidationKey(activationKey);
      userFacade.update(user);
      MessagesController.addInfoMessage("Account verification Email Successfully Resent");
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to " + user.getEmail() + ". " + e.getMessage());
      LOGGER.log(Level.SEVERE, "Could not send email to {0}. {1}", new Object[]{user.getEmail(), e});
    }
  }
  
  public List<String> getAllGroupsNames() {
    List<String> groups = new ArrayList<>();
    for (BbcGroup group : bbcGroupFacade.findAll()) {
      groups.add(group.getGroupName());
    }
    return groups;
  }
  
  public void deleteUser(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      usersController.deleteUser(user);
      MessagesController.addInfoMessage(user.getEmail() + " was removed.");
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Remove failed. " + ex.getMessage());
      LOGGER.log(Level.SEVERE, "Could not remove user.", ex);
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
      usersController.changeAccountStatus(user.getUid(), "", UserAccountStatus.NEW_MOBILE_ACCOUNT);
      MessagesController.addInfoMessage(user.getEmail() + " was removed from spam list.");
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Remove failed. " + ex.getMessage());
      LOGGER.log(Level.SEVERE, "Could not remove user from spam list.", ex);
    }
  }
  
  public String getLoginName(Principal principal) throws IOException {
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
}
