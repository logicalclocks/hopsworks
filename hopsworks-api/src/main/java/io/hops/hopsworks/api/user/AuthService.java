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
package io.hops.hopsworks.api.user;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.auth.UserStatusValidator;
import io.hops.hopsworks.api.auth.UserUtilities;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.JWTNotRequired;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UsersDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.QrCode;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.utils.ProxyAuthHelper;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.util.FormatUtils;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_NAME;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Path("/auth")
@Stateless
@Api(value = "Auth",
    description = "Authentication service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthService {

  private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private UsersController userController;
  @EJB
  private UserStatusValidator statusValidator;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private AuthController authController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;
  @EJB
  private JWTController jwtController;
  @EJB
  private UserUtilities userUtilities;

  @GET
  @Path("session")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context HttpServletRequest req) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setData(req.getRemoteUser());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("jwt/session")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response jwtSession(@Context SecurityContext sc,
                             @Context HttpServletRequest req) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    String remoteUser = user != null ? user.getEmail() : sc.getUserPrincipal().getName();
    json.setData(remoteUser);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response login(@FormParam("email") String email,
    @FormParam("password") String password, @FormParam("otp") String otp,
    @Context HttpServletResponse res,
    @Context HttpServletRequest req, @CookieParam(PROXY_JWT_COOKIE_NAME) Cookie cookie) throws UserException,
    SigningKeyNotFoundException, NoSuchAlgorithmException,
    LoginException, DuplicateSigningKeyException {

    if (settings.isPasswordLoginDisabled()) {
      throw new LoginException("Password login not allowed");
    }
    if (email == null || email.isEmpty()) {
      throw new IllegalArgumentException("Email was not provided");
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Password can not be empty.");
    }
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new LoginException("Unrecognized email address. Have you registered yet?");
    }
    if (!needLogin(req, cookie, user)) {
      return Response.ok().build();
    }

    // A session needs to be created explicitly before going to the login operation
    req.getSession();

    // Do otp check and get salted password
    String passwordWithSaltPlusOtp = authController.preLoginCheck(user, password, otp);

    // Do login
    Response response = login(user, passwordWithSaltPlusOtp, req, res);

    if (LOGGER.isLoggable(Level.FINEST)) {
      logUserLogin(req);
    }

    return response;
  }

  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response logout(@Context HttpServletRequest req, @CookieParam(PROXY_JWT_COOKIE_NAME) Cookie cookie)
    throws UserException, InvalidationException {
    logoutAndInvalidateSession(req, cookie);
    NewCookie newCookie = ProxyAuthHelper.getNewCookieForLogout();
    return Response.ok().cookie(newCookie).build();
  }

  @POST
  @Path("/service")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response serviceLogin(@FormParam("email") String email, @FormParam("password") String password,
    @Context HttpServletRequest request) throws UserException, GeneralSecurityException, SigningKeyNotFoundException,
    DuplicateSigningKeyException, HopsSecurityException {
    if (Strings.isNullOrEmpty(email)) {
      throw new IllegalArgumentException("Email cannot be null or empty");
    }
    if (Strings.isNullOrEmpty(password)) {
      throw new IllegalArgumentException("Password cannot be null or empty");
    }
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new LoginException("Could not find registered user with email " + email);
    }
    if (!needLogin(request, null, user)) {
      return Response.ok().build();
    }

    if (!userController.isUserInRole(user, "AGENT")) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.REST_ACCESS_CONTROL, Level.FINE,
          "Users are not allowed to access this endpoint, use auth/login instead",
          "User " + user.getUsername() + " tried to login but they don't have AGENT role");
    }
    request.getSession();
    
    statusValidator.checkStatus(user.getStatus());
    String saltedPassword = authController.preLoginCheck(user, password, null);
    
    try {
      request.login(user.getEmail(), saltedPassword);
    } catch (ServletException ex) {
      authController.registerAuthenticationFailure(user);
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.FINE, null, ex.getMessage(), ex);
    }

    String token = jWTHelper.createToken(user, settings.getJWTIssuer(), null);
    return Response.ok().header(AUTHORIZATION, Constants.BEARER + token).build();
  }

  @GET
  @Path("isAdmin")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response isAdmin(@Context SecurityContext sc,
                          @Context HttpServletRequest req) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setData(false);
    if (sc.isUserInRole("HOPS_ADMIN")) {
      json.setData(true);
      return Response.ok(json).build();
    }
    return Response.ok(json).build();
  }

  @POST
  @Path("register")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response register(UsersDTO newUser, @Context HttpServletRequest req) throws UserException {
    if (settings.isRegistrationDisabled()) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_REGISTRATION_ERROR, Level.FINE, "Registration not " +
        "allowed.");
    }
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    String linkUrl = FormatUtils.getUserURL(req) + Settings.VERIFICATION_PATH;
    QrCode qrCode = userController.registerUser(newUser, linkUrl);
    if (authController.isTwoFactorEnabled(newUser.isTwoFactor())) {
      return Response.ok(qrCode).build();
    } else {
      json.setSuccessMessage("We registered your account request. Please validate you email and we will "
        + "review your account within 48 hours.");
    }
    return Response.ok(json).build();
  }

  @POST
  @Path("/validate/otp")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response validateOTP(@FormParam("email") String email, @FormParam("password") String password,
    @FormParam("otp") String otp, @Context HttpServletRequest req) throws UserException {
    authController.validateOTP(email, password, otp);
    return Response.ok().build();
  }

  @POST
  @Path("/recover/password")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response recoverPassword(@FormParam("email") String email, @Context HttpServletRequest req)
    throws UserException, MessagingException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    String reqUrl = FormatUtils.getUserURL(req);
    userController.sendPasswordRecoveryEmail(email, reqUrl);
    json.setSuccessMessage(ResponseMessages.PASSWORD_RESET);
    return Response.ok(json).build();
  }
  
  @POST
  @Path("/recover/qrCode")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response recoverQRCode(@FormParam("email") String email, @FormParam("password") String password,
    @Context HttpServletRequest req) throws UserException, MessagingException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    String reqUrl = FormatUtils.getUserURL(req);
    userController.sendQRRecoveryEmail(email, password, reqUrl);
    json.setSuccessMessage(ResponseMessages.QR_CODE_RESET);
    return Response.ok(json).build();
  }
  
  @POST
  @Path("/reset/validate")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response validatePasswordRecovery(@FormParam("key") String key, @Context HttpServletRequest req)
    throws UserException {
    userController.checkRecoveryKey(key);
    return Response.ok().build();
  }
  
  @POST
  @Path("/reset/password")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response passwordRecovery(@FormParam("key") String key, @FormParam("newPassword") String newPassword,
    @FormParam("confirmPassword") String confirmPassword, @Context HttpServletRequest req) throws UserException,
    MessagingException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    userController.changePassword(key, newPassword, confirmPassword);
    json.setSuccessMessage(ResponseMessages.PASSWORD_CHANGED);
    return Response.ok(json).build();
  }
  
  @POST
  @Path("/reset/qrCode")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response qrCodeRecovery(@FormParam("key") String key, @Context HttpServletRequest req) throws UserException,
    MessagingException {
    QrCode qrCode = userController.recoverQRCode(key);
    return Response.ok(qrCode).build();
  }
  
  @POST
  @Path("/validate/email")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTNotRequired
  public Response validateUserMail(@FormParam("key") String key, @Context HttpServletRequest req) throws UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    authController.validateEmail(key);
    json.setSuccessMessage(ResponseMessages.EMAIL_VERIFIED);
    return Response.ok(json).build();
  }

  private void logoutAndInvalidateSession(HttpServletRequest req, Cookie cookie) throws UserException,
    InvalidationException {
    jWTHelper.invalidateToken(req);//invalidate iff req contains jwt token
    if (cookie != null) {
      // if there is a proxy jwt in cookie invalidate
      jWTHelper.invalidateToken(cookie.getValue());
    }
    logoutSession(req);
  }

  private void logoutSession(HttpServletRequest req) throws UserException {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    try {
      req.getSession().invalidate();
      req.logout();
      if (user != null) {
        authController.registerLogout(user);
      }
    } catch (ServletException e) {
      //accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGOUT.name(), UserAuditActions.FAILED.name(), req);
      throw new UserException(RESTCodes.UserErrorCode.LOGOUT_FAILURE, Level.SEVERE, null, e.getMessage(), e);
    }
  }

  private Response login(Users user, String password, HttpServletRequest req, HttpServletResponse res)
    throws UserException, SigningKeyNotFoundException, NoSuchAlgorithmException, DuplicateSigningKeyException {
    if (user.getBbcGroupCollection() == null || user.getBbcGroupCollection().isEmpty()) {
      throw new UserException(RESTCodes.UserErrorCode.NO_ROLE_FOUND, Level.FINE,
        null, RESTCodes.UserErrorCode.NO_ROLE_FOUND.getMessage());
    }

    statusValidator.checkStatus(user.getStatus());
    try {
      req.login(user.getEmail(), password);
      authController.registerLogin(user, req);
    } catch (ServletException e) {
      authController.registerAuthenticationFailure(user);
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.FINE, null, e.getMessage(), e);
    }

    return sendLoginResponse(req, user, res);
  }

  private Response sendLoginResponse(HttpServletRequest req, Users user, HttpServletResponse res)
    throws DuplicateSigningKeyException, SigningKeyNotFoundException, NoSuchAlgorithmException {
    Response.ResponseBuilder responseBuilder = Response.ok();
    // JWT claims will be added by JWTHelper
    String token = jWTHelper.createToken(user, settings.getJWTIssuer(), null);
    String proxyToken = jWTHelper.createTokenForProxy(user);

    // add proxy jwt cookie
    // responseBuilder.cookie(newCookie); removes SESSION and JSESSIONSSO cookies
    // can be replaced with the above after jsf admin ui is removed
    res.addCookie(ProxyAuthHelper.getCookie(proxyToken,  settings.getJWTLifetimeMsPlusLeeway()));

    // add api jwt auth header
    responseBuilder.header(AUTHORIZATION, Constants.BEARER + token);

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSessionID(req.getSession().getId());
    json.setData(user.getEmail());
    return responseBuilder.entity(json).build();
  }

  private boolean isUserLoggedIn(String remoteUser, Users tokenUser, boolean validToken, Users user) {
    if (user == null) {
      return false;
    }
    boolean sessionLoggedIn = remoteUser != null && remoteUser.equals(user.getEmail());
    boolean jwtLoggedIn = tokenUser != null && tokenUser.equals(user) && validToken;
    return sessionLoggedIn && jwtLoggedIn;
  }

  private boolean isSomeoneElseLoggedIn(String remoteUser, Users tokenUser, boolean validToken, Users user) {
    if (user == null) {
      return false;
    }
    boolean sessionLoggedIn = remoteUser != null && !remoteUser.equals(user.getEmail());
    boolean jwtLoggedIn = tokenUser != null && !tokenUser.equals(user) && validToken;
    return sessionLoggedIn && jwtLoggedIn;
  }

  private boolean needLogin(HttpServletRequest req, Cookie cookie, Users user) {
    String remoteUser = req.getRemoteUser();
    Users tokenUser = jWTHelper.getUserPrincipal(req);
    boolean validToken = jWTHelper.validToken(req, settings.getJWTIssuer());

    if (isUserLoggedIn(remoteUser, tokenUser, validToken, user)) {
      return false;
    } else if (isSomeoneElseLoggedIn(remoteUser, tokenUser, validToken, user)) {
      try {
        logoutAndInvalidateSession(req, cookie);
      } catch (InvalidationException | UserException ex) {
        LOGGER.log(Level.SEVERE, null, ex.getMessage());
      }
    } else if (validToken || remoteUser != null) {
      if (remoteUser != null) {
        try {
          logoutSession(req);
        } catch (UserException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
        }
      }
      if (validToken) {
        try {
          jWTHelper.invalidateToken(req);
        } catch (InvalidationException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
        }
      }
    }
    return true;
  }

  private void logUserLogin(HttpServletRequest req) {
    StringBuilder roles = new StringBuilder();
    roles.append(req.isUserInRole("HOPS_USER") ? "{user" : "{");
    roles.append(req.isUserInRole("HOPS_ADMIN") ? " admin" : "");
    roles.append(req.isUserInRole("AGENT") ? " agent" : "");
    roles.append(req.isUserInRole("HOPS_SERVICE_USER") ? " as a service user}" : "}");
    LOGGER.log(Level.FINEST, "[/hopsworks-api] login:\n email: {0}\n session: {1}\n in roles: {2}", new Object[]{
      req.getUserPrincipal(), req.getSession().getId(), roles});
  }
}
