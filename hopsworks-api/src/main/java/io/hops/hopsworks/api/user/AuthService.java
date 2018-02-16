/*
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
 *
 */

package io.hops.hopsworks.api.user;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UserDTO;
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
import io.swagger.annotations.Api;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.binary.Base64;

@Path("/auth")
@Stateless
@Api(value = "Auth",
    description = "Authentication service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthService {

  private final static Logger LOGGER = Logger.getLogger(AuthService.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private UsersController userController;
  @EJB
  private UserStatusValidator statusValidator;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @EJB
  private AuthController authController;
  @EJB
  private LdapUserController ldapUserController;

  @GET
  @Path("session")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    try {
      json.setStatus("SUCCESS");
      json.setData(sc.getUserPrincipal().getName());
    } catch (Exception e) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email, @FormParam("password") String password,
      @FormParam("otp") String otp, @Context HttpServletRequest req) throws AppException, MessagingException {
    logUserLogin(req);
    JsonResponse json = new JsonResponse();
    if (email == null || email.isEmpty()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Email address field cannot be empty");
    }
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "Unrecognized email address. Have you registered yet?");
    }
    // Do pre cauth realm check 
    String passwordWithSaltPlusOtp = authController.preCustomRealmLoginCheck(user, password, otp, req);

    // logout any user already loggedin if a new user tries to login 
    if (req.getRemoteUser() != null && !req.getRemoteUser().equals(email)) {
      logoutAndInvalidateSession(req);
    }
    //only login if not already logged...
    if (req.getRemoteUser() == null) {
      login(user, email, passwordWithSaltPlusOtp, req);
    } else {
      req.getServletContext().log("Skip logged because already logged in: " + email);
    }

    //read the user data from db and return to caller
    json.setStatus("SUCCESS");
    json.setSessionID(req.getSession().getId());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("ldapLogin")
  @Produces(MediaType.APPLICATION_JSON)
  public Response ldapLogin(@FormParam("username") String username, @FormParam("password") String password,
      @FormParam("chosenEmail") String chosenEmail, @FormParam("consent") boolean consent,
      @Context HttpServletRequest req) throws LoginException, AppException {
    JsonResponse json = new JsonResponse();
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("Username can not be empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Password can not be empty.");
    }
    LdapUserState ldapUserState = ldapUserController.login(username, password, consent, chosenEmail);
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
    if (req.getRemoteUser() != null && !req.getRemoteUser().equals(user.getEmail())) {
      logoutAndInvalidateSession(req);
    }
    //only login if not already logged...
    if (req.getRemoteUser() == null) {
      login(user, user.getEmail(), passwordWithSalt, req);
    } else {
      req.getServletContext().log("Skip logged because already logged in: " + username);
    }
    //read the user data from db and return to caller
    json.setStatus("SUCCESS");
    json.setSessionID(req.getSession().getId());
    json.setData(user.getEmail());
    return Response.status(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    logoutAndInvalidateSession(req);
    return Response.ok().entity(json).build();
  }

  @GET
  @Path("isAdmin")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public Response login(@Context SecurityContext sc, @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
      throws AppException, MessagingException {
    if (sc.isUserInRole("HOPS_ADMIN")) {
      return Response.ok(true).build();
    }
    return Response.ok(false).build();
  }

  @POST
  @Path("register")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response register(UserDTO newUser, @Context HttpServletRequest req) throws AppException, SocketException,
      NoSuchAlgorithmException {
    byte[] qrCode;
    JsonResponse json = new JsonResponse();
    qrCode = userController.registerUser(newUser, req);
    if (authController.isTwoFactorEnabled() && newUser.isTwoFactor()) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage("We registered your account request. Please validate you email and we will "
          + "review your account within 48 hours.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("recoverPassword")
  @Produces(MediaType.APPLICATION_JSON)
  public Response recoverPassword(@FormParam("email") String email,
      @FormParam("securityQuestion") String securityQuestion,
      @FormParam("securityAnswer") String securityAnswer,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, Exception {
    JsonResponse json = new JsonResponse();
    userController.recoverPassword(email, securityQuestion, securityAnswer, req);
    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.PASSWORD_RESET_SUCCESSFUL);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("/validation/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateUserEmail(@Context HttpServletRequest req, @PathParam("key") String key) throws AppException {
    authController.validateKey(key, req);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("/validation/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateUserMail(@Context HttpServletRequest req, @PathParam("key") String key) throws AppException {
    authController.validateKey(key, req);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("/recovery")
  @Produces(MediaType.TEXT_PLAIN)
  public Response sendNewValidationKey(@Context SecurityContext sc, @Context HttpServletRequest req,
      @FormParam("email") String email) throws AppException, MessagingException {
    Users u = userFacade.findByEmail(email);
    if (u == null || statusValidator.isBlockedAccount(u)) {
      //if account blocked then ignore the request
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    authController.sendNewValidationKey(u, req);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void logoutAndInvalidateSession(HttpServletRequest req) throws AppException {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    try {
      req.getSession().invalidate();
      req.logout();
      if (user != null) {
        authController.registerLogout(user, req);
        //remove zeppelin ticket for user
        TicketContainer.instance.invalidate(user.getEmail());
      }
    } catch (ServletException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
      accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGOUT.name(), UserAuditActions.FAILED.name(), req);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ResponseMessages.LOGOUT_FAILURE);
    }
  }

  private void login(Users user, String email, String password, HttpServletRequest req) throws AppException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (user.getBbcGroupCollection() == null || user.getBbcGroupCollection().isEmpty()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.NO_ROLE_FOUND);
    }
    if (statusValidator.checkStatus(user.getStatus())) {
      try {
        req.login(email, password);
        authController.registerLogin(user, req);
      } catch (ServletException e) {
        LOGGER.log(Level.WARNING, e.getMessage());
        authController.registerAuthenticationFailure(user, req);
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
      }
    } else { // if user == null
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
    }
  }

  private void logUserLogin(HttpServletRequest req) {
    StringBuilder roles = new StringBuilder();
    roles.append(req.isUserInRole("HOPS_USER") ? "{user" : "{");
    roles.append(req.isUserInRole("HOPS_ADMIN") ? " admin" : "");
    roles.append(req.isUserInRole("AGENT") ? " agent" : "");
    roles.append(req.isUserInRole("CLUSTER_AGENT") ? " cluster-agent}" : "}");
    LOGGER.log(Level.INFO, "[/hopsworks-api] login:\n email: {0}\n session: {1}\n in roles: {2}", new Object[]{
      req.getUserPrincipal(), req.getSession().getId(), roles});
  }
}
