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
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserProfileBuilder {
  @EJB
  private UsersController usersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;

  public UserProfileDTO acceptUser(Integer userId, Users newUser) throws UserException, ServiceException {
    Users u = usersController.getUserById(userId);
    if (u.getStatus().equals(UserAccountStatus.VERIFIED_ACCOUNT)) {
      Collection<BbcGroup> groups = null;
      if (newUser != null) {
        groups = newUser.getBbcGroupCollection();
      }
      if (groups == null || groups.isEmpty()) {
        BbcGroup bbcGroup = bbcGroupFacade.findByGroupName("HOPS_USER");
        groups = new ArrayList<>();
        groups.add(bbcGroup);
      }
      u.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);
      u.setBbcGroupCollection(groups);
      u = userFacade.update(u);
      usersController.sendConfirmationMail(u);
    } else {
      throw new UserException(RESTCodes.UserErrorCode.TRANSITION_STATUS_ERROR, Level.WARNING,
        "status: " + u.getStatus().name() + " to status " + UserAccountStatus.ACTIVATED_ACCOUNT.name());
    }
    return new UserProfileDTO(u);
  }
  
  public UserProfileDTO rejectUser(Integer userId) throws UserException, ServiceException {
    Users u = userFacade.find(userId);
    if (u != null) {
      u.setStatus(UserAccountStatus.SPAM_ACCOUNT);
      u = userFacade.update(u);
      usersController.sendRejectionEmail(u);
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    return new UserProfileDTO(u);
  }
  
  public UserProfileDTO pendUser(String linkUrl, Integer userId) throws UserException, ServiceException {
    Users u = userFacade.find(userId);
    if (u != null) {
      if (u.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
        u = usersController.resendAccountVerificationEmail(u, linkUrl);
      } else {
        throw new UserException(RESTCodes.UserErrorCode.TRANSITION_STATUS_ERROR, Level.WARNING,
          "status: "+ u.getStatus().name() + ", to pending status");
      }
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    
    return new UserProfileDTO(u);
  }
  
  public UserProfileDTO updateUser(Integer userIdToUpdate, Users newUser) throws UserException {
    Users u = userFacade.find(userIdToUpdate);
    if (u == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    
    if (newUser.getStatus() != null) {
      u.setStatus(newUser.getStatus());
    }
    if (newUser.getBbcGroupCollection() != null) {
      u.setBbcGroupCollection(newUser.getBbcGroupCollection());
    }
    if (newUser.getMaxNumProjects() != null) {
      u.setMaxNumProjects(newUser.getMaxNumProjects());
    }
    u = userFacade.update(u);
    return new UserProfileDTO(u);
  }
}
