/*
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
 */
package io.hops.hopsworks.apiV2.auth;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UserStatusValidator;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.user.ldap.LdapUserController;
import io.hops.hopsworks.common.user.ldap.LdapUserState;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.annotation.AcceptedTokens;
import io.hops.hopsworks.jwt.annotation.AllowedUserRoles;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.swagger.annotations.Api;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/auth")
@Stateless
@Api(value = "Auth",
    description = "Authentication resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthResource {

  private final static Logger LOGGER = Logger.getLogger(AuthResource.class.getName());
  @EJB
  private AuthController authController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private JWTController jwtController;
  @EJB
  private UsersController userController;
  @EJB
  private UserStatusValidator statusValidator;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @EJB
  private LdapUserController ldapUserController;
  @EJB
  private Settings settings;

  @Context
  private UriInfo uriInfo;

  @GET
  @Path("session")
  @JWTRequired
  @AcceptedTokens({"api"})
  @Produces(MediaType.TEXT_PLAIN)
  public Response session(@Context HttpServletRequest req) {
    return Response.ok().build();
  }

  @GET
  @Path("admin")
  @JWTRequired
  @AcceptedTokens({"api"})
  @AllowedUserRoles({"HOPS_ADMIN"})
  @Produces(MediaType.TEXT_PLAIN)
  public Response admin() {
    return Response.ok("Admin").build();
  }

  @GET
  @Path("user")
  @JWTRequired
  @AcceptedTokens({"api", "service"})
  @AllowedUserRoles({"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.TEXT_PLAIN)
  public Response user() {
    return Response.ok("User").build();
  }

  @POST
  @Path("login")
  @Produces(MediaType.TEXT_PLAIN)
  public Response login(@FormParam("email") String email, @FormParam("password") String password,
      @FormParam("otp") String otp, @Context HttpServletRequest req) throws NoSuchAlgorithmException, AppException,
      LoginException, SigningKeyNotFoundException {
    if (email == null || email.isEmpty()) {
      throw new LoginException("Email address can not be empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new LoginException("Password can not be empty.");
    }
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new LoginException("Unrecognized email address. Have you registered yet?");
    }
    // Do pre cauth realm check 
    String passwordWithSaltPlusOtp = authController.preCustomRealmLoginCheck(user, password, otp, req);
    return login(user, email, passwordWithSaltPlusOtp, req);
  }

  @POST
  @Path("ldapLogin")
  @Produces(MediaType.APPLICATION_JSON)
  public Response ldapLogin(@FormParam("username") String username, @FormParam("password") String password,
      @FormParam("chosenEmail") String chosenEmail, @FormParam("consent") boolean consent,
      @Context HttpServletRequest req) throws LoginException, NoSuchAlgorithmException, AppException,
      SigningKeyNotFoundException {
    if (username == null || username.isEmpty()) {
      throw new LoginException("Username can not be empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new LoginException("Password can not be empty.");
    }
    LdapUserState ldapUserState = ldapUserController.login(username, password, consent, chosenEmail);
    return ldapUserLogin(ldapUserState, req);
  }

  @GET
  @Path("logout")
  @JWTRequired
  @Produces(MediaType.TEXT_PLAIN)
  public Response logout(@Context HttpServletRequest req) throws AppException, InvalidationException {
    String authorizationHeader = req.getHeader(AUTHORIZATION);
    if (authorizationHeader != null) {
      String token = authorizationHeader.substring(Constants.BEARER.length()).trim();
      jwtController.invalidate(token);
      logoutAndInvalidateSession(req);
    }
    return Response.ok().build();
  }

  @GET
  @Path("renew")
  @JWTRequired
  @Produces(MediaType.APPLICATION_JSON)
  public Response renew(@Context HttpServletRequest req) throws SigningKeyNotFoundException, NotRenewableException,
      InvalidationException {
    String authorizationHeader = req.getHeader(AUTHORIZATION);
    String token = "";
    if (authorizationHeader != null) {
      token = authorizationHeader.substring(Constants.BEARER.length()).trim();
      Date now = new Date();
      Date expiresAt = new Date(now.getTime() + settings.getJWTLifetimeMs());
      token = jwtController.renewToken(token, expiresAt, now);
    }
    return Response.ok().header(AUTHORIZATION, Constants.BEARER + token).build();
  }

  private Response login(Users user, String email, String password, HttpServletRequest req) throws AppException,
      NoSuchAlgorithmException, LoginException, SigningKeyNotFoundException {
    if (user.getBbcGroupCollection() == null || user.getBbcGroupCollection().isEmpty()) {
      throw new LoginException(ResponseMessages.NO_ROLE_FOUND);
    }
    // logout any user already loggedin if a new user tries to login 
    if (req.getRemoteUser() != null && !req.getRemoteUser().equals(email)) {
      logoutAndInvalidateSession(req);
    }
    if (req.getRemoteUser() == null && statusValidator.checkStatus(user.getStatus())) {
      try {
        req.login(email, password);
        authController.registerLogin(user, req);
      } catch (ServletException e) {
        LOGGER.log(Level.WARNING, e.getMessage());
        authController.registerAuthenticationFailure(user, req);
        throw new LoginException(ResponseMessages.AUTHENTICATION_FAILURE);
      }
    }
    return Response.ok().header(AUTHORIZATION, Constants.BEARER + createToken(user)).build();
  }

  private String createToken(Users user) throws NoSuchAlgorithmException, SigningKeyNotFoundException {
    String[] audience = {"service"};
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + settings.getJWTLifetimeMs());
    String issuer = uriInfo.getAbsolutePath().toString();
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    String[] roles = userController.getUserRoles(user).toArray(new String[0]);
    String token = jwtController.createToken(settings.getJWTSigningKeyName(), false, issuer, audience, expiresAt,
        new Date(), user.getUsername(), true, settings.getJWTExpLeewaySec(), roles, alg);
    return token;
  }

  private void logoutAndInvalidateSession(HttpServletRequest req) throws AppException {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    try {
      req.getSession().invalidate();
      req.logout();
      if (user != null) {
        authController.registerLogout(user, req);
      }
    } catch (ServletException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
      accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGOUT.name(), UserAuditActions.FAILED.name(), req);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ResponseMessages.LOGOUT_FAILURE);
    }
  }

  private Response ldapUserLogin(LdapUserState ldapUserState, HttpServletRequest req) throws LoginException,
      NoSuchAlgorithmException, AppException, SigningKeyNotFoundException {
    if (!ldapUserState.isSaved()) {
      return Response.status(Response.Status.PRECONDITION_FAILED).entity(ldapUserState.getUserDTO()).build();
    }
    LdapUser ladpUser = ldapUserState.getLdapUser();
    if (ladpUser == null || ladpUser.getUid() == null) {
      throw new LoginException("Failed to get ldap user from table.");
    }
    Users user = ladpUser.getUid();
    // Do pre cauth realm check 
    String passwordWithSalt = authController.preLdapLoginCheck(user, ladpUser.getAuthKey(), req);
    return login(user, user.getEmail(), passwordWithSalt, req);
  }
}
