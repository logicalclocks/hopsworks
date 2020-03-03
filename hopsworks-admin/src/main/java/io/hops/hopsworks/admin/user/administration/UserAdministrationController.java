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

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.exceptions.UserException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
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
  private AuditedUserAdministration auditedUserAdministration;
  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  
  public String getAccountType(UserAccountType type) {
    return type.getAccountType();
  }
  
  public String getUserStatus(UserAccountStatus status) {
    return status.getUserStatus();
  }
  
  private HttpServletRequest getHttpServletRequest() {
    FacesContext context = FacesContext.getCurrentInstance();
    return (HttpServletRequest) context.getExternalContext().getRequest();
  }
  
  public void rejectUser(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    HttpServletRequest httpServletRequest = getHttpServletRequest();
    try {
      auditedUserAdministration.changeStatus(user, UserAccountStatus.SPAM_ACCOUNT, httpServletRequest);
      MessagesController.addInfoMessage(user.getEmail() + " was rejected.");
    } catch (UserException ex) {
      MessagesController.addSecurityErrorMessage("Rejection failed. " + ex.getMessage());
      LOGGER.log(Level.SEVERE, "Could not reject user.", ex);
    }
  }
  
  public void activateUser(Users user) {
    if (user == null) {
      MessagesController.addSecurityErrorMessage("User is null.");
      return;
    }
    if (user.getGroupName() == null || user.getGroupName().isEmpty()) {
      user.setGroupName("HOPS_USER");
    }
    HttpServletRequest httpServletRequest = getHttpServletRequest();
    try {
      auditedUserAdministration.activateUser(user, httpServletRequest);
      MessagesController.addInfoMessage("User " + user.getEmail() + " activated successfully");
    } catch (UserException ue) {
      MessagesController.addSecurityErrorMessage(ue.getMessage());
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
    HttpServletRequest request = getHttpServletRequest();
    try {
      auditedUserAdministration.resendAccountVerificationEmail(user, request);
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
  
  public void deleteSpamUser(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    HttpServletRequest request = getHttpServletRequest();
    try {
      auditedUserAdministration.deleteSpamUser(user, request);
      MessagesController.addInfoMessage(user.getEmail() + " was removed.");
    } catch (RuntimeException | UserException ex) {
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
    HttpServletRequest httpServletRequest = getHttpServletRequest();
    try {
      auditedUserAdministration.changeStatus(user, UserAccountStatus.NEW_MOBILE_ACCOUNT, httpServletRequest);
      MessagesController.addInfoMessage(user.getEmail() + " was removed from spam list.");
    } catch (UserException ex) {
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
      ExternalContext extContext = FacesContext.getCurrentInstance().getExternalContext();
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }
}
