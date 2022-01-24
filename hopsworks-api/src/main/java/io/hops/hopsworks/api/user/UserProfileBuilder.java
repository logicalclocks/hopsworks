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

import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.user.UserAccountHandler;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserProfileBuilder {
  private final static Logger LOGGER = Logger.getLogger(UserProfileBuilder.class.getName());
  @EJB
  private UsersController usersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @Inject
  @Any
  private Instance<UserAccountHandler> userAccountHandlers;

  private Users accept(Integer userId, Users newUser) throws UserException, ServiceException {
    Users u = usersController.getUserById(userId);
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
    UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, u); // run user update handlers
    usersController.sendConfirmationMail(u);
    return u;
  }

  public UserProfileDTO acceptUser(Integer userId, Users newUser) throws UserException, ServiceException {
    Users u = accept(userId, newUser);
    return new UserProfileDTO(u);
  }

  public UserProfileDTO acceptUsers(List<Integer> ids, HttpServletRequest req) throws UserException {
    UserProfileDTO userProfileDTO = new UserProfileDTO();
    Users target = null;
    if (ids != null && ids.size() > 0) {
      for (Integer id : ids) {
        try {
          target = accept(id, null);
          userProfileDTO.addItem(new UserProfileDTO(target));
        } catch (UserException | ServiceException e) {
          LOGGER.log(Level.WARNING, "Failed to accept user with id: {0}", id);
        }
      }
      if (userProfileDTO.getItems() != null) {
        userProfileDTO.setCount((long) userProfileDTO.getItems().size());
      } else {
        userProfileDTO.setCount(0L);
      }
    
      if (userProfileDTO.getCount() < 1) {
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_ACTIVATION_FAILED, Level.FINE, "Failed to activate " +
          "users");
      }
    }
    return userProfileDTO;
  }

  public UserProfileDTO changeRole(Integer userId, String role) throws UserException {
    Users u = usersController.getUserById(userId);
    Collection<BbcGroup> groups = new ArrayList<>();
    BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(role);
    if (bbcGroup == null) {
      throw new UserException(RESTCodes.UserErrorCode.ROLE_NOT_FOUND, Level.FINE);
    }
    groups.add(bbcGroup);
    u.setBbcGroupCollection(groups);
    u = userFacade.update(u);
    return new UserProfileDTO(u);
  }

  public Users reject(Integer userId) throws UserException, ServiceException {
    Users u = usersController.getUserById(userId);
    u.setStatus(UserAccountStatus.SPAM_ACCOUNT);
    u = userFacade.update(u);
    UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, u); // run user update handlers
    usersController.sendRejectionEmail(u);
    return u;
  }

  public UserProfileDTO rejectUser(Integer userId) throws UserException, ServiceException {
    Users u = reject(userId);
    return new UserProfileDTO(u);
  }

  public UserProfileDTO rejectUsers(List<Integer> ids, HttpServletRequest req) throws UserException {
    UserProfileDTO userProfileDTO = new UserProfileDTO();
    Users target = null;
    if (ids != null && ids.size() > 0) {
      for (Integer id : ids) {
        try {
          target = reject(id);
          userProfileDTO.addItem(new UserProfileDTO(target));
        } catch (UserException | ServiceException e) {
          LOGGER.log(Level.WARNING, "Failed to reject user with id: {0}", id);
        }
      }
      if (userProfileDTO.getItems() != null) {
        userProfileDTO.setCount((long) userProfileDTO.getItems().size());
      } else {
        userProfileDTO.setCount(0L);
      }
    
      if (userProfileDTO.getCount() < 1) {
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_REJECTION_FAILED, Level.FINE,
          "Failed to reject users");
      }
    }
    return userProfileDTO;
  }

  public UserProfileDTO pendUser(String linkUrl, Integer userId) throws UserException, ServiceException {
    Users u = usersController.getUserById(userId);
    if (u.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
      u = usersController.resendAccountVerificationEmail(u, linkUrl);
    } else {
      throw new UserException(RESTCodes.UserErrorCode.TRANSITION_STATUS_ERROR, Level.WARNING,
        "status: " + u.getStatus().name() + ", to pending status");
    }

    return new UserProfileDTO(u);
  }

  public UserProfileDTO updateUser(Integer userIdToUpdate, Users newUser) throws UserException {
    Users u = usersController.getUserById(userIdToUpdate);

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
    
    // run user update handlers
    UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, u);
    
    return new UserProfileDTO(u);
  }
}
