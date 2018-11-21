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

package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditAction;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.ws.rs.core.SecurityContext;

@Path("/admin")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Api(value = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersAdmin {

  @EJB
  private UserFacade userFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private AccountAuditFacade auditManager;
  @EJB
  private EmailBean emailBean;
  @EJB
  private Settings settings;
  @EJB
  private JWTHelper jWTHelper;

  @GET
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUsers(@QueryParam("status") String filter){
    List<Users> list = new ArrayList<>();
    if (filter == null) {
      list = userFacade.findAllUsers();
    } else {
      String[] filterStrings = filter.split(",");
      for (String filterString : filterStrings) {
        UserAccountStatus status = UserAccountStatus.valueOf(filterString);
        list.addAll(userFacade.findAllByStatus(status));
      }
    }
    GenericEntity<List<Users>> users = new GenericEntity<List<Users>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(users).build();
  }

  @GET
  @Path("/users/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUser(@PathParam("email") String email) throws UserException {
    Users u = userFacade.findByEmail(email);
    if (u == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateUser(@Context HttpServletRequest req, @Context SecurityContext sc,
      @PathParam("email") String email, Users user) throws UserException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      if (user.getStatus() != null) {
        u.setStatus(user.getStatus());
        u = userFacade.update(u);
        Users initiator = jWTHelper.getUserPrincipal(sc);
        auditManager.registerRoleChange(initiator, AccountsAuditActions.CHANGEDSTATUS.name(),
            AccountsAuditActions.SUCCESS.name(), u.getStatusName(), u, req);
      }
      if (user.getBbcGroupCollection() != null) {
        u.setBbcGroupCollection(user.getBbcGroupCollection());
        u = userFacade.update(u);
        String result = "";
        for (BbcGroup group : u.getBbcGroupCollection()) {
          result = result + group.getGroupName() + ", ";
        }
        Users initiator = jWTHelper.getUserPrincipal(sc);
        auditManager.registerRoleChange(initiator, RolesAuditAction.ROLE_UPDATED.name(), RolesAuditAction.SUCCESS.
            name(), result, u, req);
      }
      if (user.getMaxNumProjects() != null) {
        u.setMaxNumProjects(user.getMaxNumProjects());
        u = userFacade.update(u);

      }
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}/accepted")
  @Produces(MediaType.APPLICATION_JSON)
  public Response acceptUser(@Context HttpServletRequest req, @Context SecurityContext sc,
      @PathParam("email") String email, Users user) throws UserException, ServiceException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      if (u.getStatus().equals(UserAccountStatus.VERIFIED_ACCOUNT)) {
        Collection<BbcGroup> groups = user.getBbcGroupCollection();
        if (groups == null || groups.isEmpty()) {
          BbcGroup bbcGroup = bbcGroupFacade.findByGroupName("HOPS_USER");
          groups = new ArrayList<>();
          groups.add(bbcGroup);
        }
        u.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);
        u.setBbcGroupCollection(groups);
        u = userFacade.update(u);
        String result = "";
        for (BbcGroup group : u.getBbcGroupCollection()) {
          result = result + group.getGroupName() + ", ";
        }
        Users initiator = jWTHelper.getUserPrincipal(sc);
        auditManager.registerRoleChange(initiator,
            RolesAuditAction.ROLE_UPDATED.name(), RolesAuditAction.SUCCESS.
            name(), result, u, req);
        auditManager.registerRoleChange(initiator, UserAccountStatus.ACTIVATED_ACCOUNT.name(),
            AccountsAuditActions.SUCCESS.name(), "", u, req);
        sendConfirmationMail(u);
      } else {
        throw new UserException(RESTCodes.UserErrorCode.TRANSITION_STATUS_ERROR, Level.WARNING,
          "status: "+ u.getStatus().name() + " to status " + UserAccountStatus.ACTIVATED_ACCOUNT.name());
      }
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }

    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}/rejected")
  @Produces(MediaType.APPLICATION_JSON)
  public Response rejectUser(@Context HttpServletRequest req, @Context SecurityContext sc,
      @PathParam("email") String email) throws UserException, ServiceException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      u.setStatus(UserAccountStatus.SPAM_ACCOUNT);
      u = userFacade.update(u);
      Users initiator = jWTHelper.getUserPrincipal(sc);

      auditManager.registerRoleChange(initiator, UserAccountStatus.SPAM_ACCOUNT.name(),
          AccountsAuditActions.SUCCESS.name(), "", u, req);
      sendRejectionEmail(u);
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }

    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}/pending")
  @Produces(MediaType.APPLICATION_JSON)
  public Response pendingUser(@Context HttpServletRequest req, @PathParam("email") String email) throws UserException,
      ServiceException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      if (u.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
        u = resendAccountVerificationEmail(u);
      } else {
        throw new UserException(RESTCodes.UserErrorCode.TRANSITION_STATUS_ERROR, Level.WARNING,
          "status: "+ u.getStatus().name() + ", to pending status");
      }
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }

    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @GET
  @Path("/usergroups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllGroups() {
    List<BbcGroup> list = bbcGroupFacade.findAll();
    GenericEntity<List<BbcGroup>> groups = new GenericEntity<List<BbcGroup>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(groups).build();
  }

  private void sendConfirmationMail(Users user) throws ServiceException {
    try {
      //send confirmation email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
          UserAccountsEmailMessages.
          accountActivatedMessage(user.getEmail()));
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }

  private Users resendAccountVerificationEmail(Users user) throws ServiceException {
    try {
      String activationKey = SecurityUtils.getRandomPassword(64);
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
          UserAccountsEmailMessages.buildMobileRequestMessageRest(settings.getVerificationEndpoint(), user.getUsername()
              + activationKey));
      user.setValidationKey(activationKey);
      return userFacade.update(user);
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }

  private void sendRejectionEmail(Users user) throws ServiceException {
    try {
      // Send rejection email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_REJECT,
          UserAccountsEmailMessages.accountRejectedMessage());
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }

}
