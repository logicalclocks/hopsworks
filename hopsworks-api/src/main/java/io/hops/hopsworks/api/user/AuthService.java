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
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.FormatUtils;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.JsonWebToken;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UserStatusValidator;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.swagger.annotations.Api;
import org.apache.commons.codec.binary.Base64;

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
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import javax.ws.rs.core.SecurityContext;

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

  @GET
  @Path("session")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context HttpServletRequest req) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setData(req.getRemoteUser());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("jwt/session")
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response jwtSession(@Context SecurityContext sc) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    String remoteUser = user != null ? user.getEmail() : sc.getUserPrincipal().getName();
    json.setData(remoteUser);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email, @FormParam("password") String password,
    @FormParam("otp") String otp, @Context HttpServletRequest req) throws UserException, SigningKeyNotFoundException,
    NoSuchAlgorithmException,
    LoginException, DuplicateSigningKeyException {

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
    if (!needLogin(req, user)) {
      return Response.ok().build();
    }

    // A session needs to be create explicitly before doing to the login operation
    req.getSession();

    // Do pre cauth realm check
    String passwordWithSaltPlusOtp = authController.preCustomRealmLoginCheck(user, password, otp);

    // Do login
    Response response = login(user, passwordWithSaltPlusOtp, req);

    if (LOGGER.isLoggable(Level.FINEST)) {
      logUserLogin(req);
    }

    return response;
  }

  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@Context HttpServletRequest req) throws UserException, InvalidationException {
    logoutAndInvalidateSession(req);
    return Response.ok().build();
  }

  @POST
  @Path("/service")
  @Produces(MediaType.APPLICATION_JSON)
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
    if (!needLogin(request, user)) {
      return Response.ok().build();
    }
    if (!userController.isUserInRole(user, "AGENT")) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.REST_ACCESS_CONTROL, Level.FINE,
          "Users are not allowed to access this endpoint, use auth/login instead",
          "User " + user.getUsername() + " tried to login but they don't have AGENT role");
    }
    request.getSession();
    
    Collection roles = user.getBbcGroupCollection();
    if (roles == null || roles.isEmpty()) {
      throw new UserException(RESTCodes.UserErrorCode.NO_ROLE_FOUND, Level.FINE);
    }
    
    statusValidator.checkStatus(user.getStatus());
    String saltedPassword = authController.preCustomRealmLoginCheck(user, password, null);
    
    try {
      request.login(user.getEmail(), saltedPassword);
    } catch (ServletException ex) {
      authController.registerAuthenticationFailure(user);
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.FINE, null, ex.getMessage(), ex);
    }
  
    // First generate the one-time tokens for renewal of master token
    String renewalKeyName = jwtController.getServiceOneTimeJWTSigningKeyname(user.getUsername(),
        request.getRemoteHost());
    LocalDateTime masterExpiration = DateUtils.getNow().plus(settings.getServiceJWTLifetimeMS(), ChronoUnit.MILLIS);
    LocalDateTime notBefore = jwtController.computeNotBefore4ServiceRenewalTokens(masterExpiration);
    LocalDateTime expiresAt = notBefore.plus(settings.getServiceJWTLifetimeMS(), ChronoUnit.MILLIS);
    List<String> userRoles = userController.getUserRoles(user);
    
    JsonWebToken renewalJWTSpec = new JsonWebToken();
    renewalJWTSpec.setSubject(user.getUsername());
    renewalJWTSpec.setIssuer(settings.getJWTIssuer());
    renewalJWTSpec.setAudience(JWTHelper.SERVICE_RENEW_JWT_AUDIENCE);
    renewalJWTSpec.setKeyId(renewalKeyName);
    renewalJWTSpec.setNotBefore(DateUtils.localDateTime2Date(notBefore));
    renewalJWTSpec.setExpiresAt(DateUtils.localDateTime2Date(expiresAt));
    
    Map<String, Object> claims = new HashMap<>(4);
    claims.put(Constants.RENEWABLE, false);
    claims.put(Constants.EXPIRY_LEEWAY, 3600);
    claims.put(Constants.ROLES, userRoles.toArray(new String[1]));
    
    String[] oneTimeRenewalTokens = jwtController.generateOneTimeTokens4ServiceJWTRenewal(renewalJWTSpec, claims,
        settings.getJWTSigningKeyName());
  
    // Then generate the master service token
    try {
      String signingKeyID = jwtController.getSignKeyID(oneTimeRenewalTokens[0]);
      claims.clear();
      // The rest of JWT claims will be added by JWTHelper
      claims.put(Constants.RENEWABLE, false);
      claims.put(Constants.SERVICE_JWT_RENEWAL_KEY_ID, signingKeyID);
      String token = jWTHelper.createToken(user, settings.getJWTIssuer(), claims);
      
      ServiceJWTDTO renewTokensResponse = new ServiceJWTDTO();
      renewTokensResponse.setRenewTokens(oneTimeRenewalTokens);
      return Response.ok().header(AUTHORIZATION, Constants.BEARER + token).entity(renewTokensResponse).build();
    } catch (Exception ex) {
      jwtController.deleteSigningKey(renewalKeyName);
      throw ex;
    }
  }
  
  @DELETE
  @Path("/service")
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceLogout(@Context HttpServletRequest request) throws UserException, InvalidationException {
    if (jWTHelper.validToken(request, settings.getJWTIssuer())) {
      jwtController.invalidateServiceToken(jWTHelper.getAuthToken(request), settings.getJWTSigningKeyName());
    }
    logoutAndInvalidateSession(request);
    return Response.ok().build();
  }
  
  @GET
  @Path("isAdmin")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response isAdmin(@Context SecurityContext sc) {
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
  public Response register(UserDTO newUser, @Context HttpServletRequest req) throws UserException {
    byte[] qrCode;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    String linkUrl = FormatUtils.getUserURL(req) + settings.getEmailVerificationEndpoint();
    qrCode = userController.registerUser(newUser, linkUrl);
    if (authController.isTwoFactorEnabled() && newUser.isTwoFactor()) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage("We registered your account request. Please validate you email and we will "
          + "review your account within 48 hours.");
    }
    return Response.ok(json).build();
  }

  @POST
  @Path("/recover/password")
  @Produces(MediaType.APPLICATION_JSON)
  public Response recoverPassword(@FormParam("email") String email,
    @FormParam("securityQuestion") String securityQuestion, @FormParam("securityAnswer") String securityAnswer,
    @Context HttpServletRequest req) throws UserException,
    MessagingException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    String reqUrl = FormatUtils.getUserURL(req);
    userController.sendPasswordRecoveryEmail(email, securityQuestion, securityAnswer, reqUrl);
    json.setSuccessMessage(ResponseMessages.PASSWORD_RESET);
    return Response.ok(json).build();
  }
  
  @POST
  @Path("/recover/qrCode")
  @Produces(MediaType.APPLICATION_JSON)
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
  public Response validatePasswordRecovery(@FormParam("key") String key, @Context HttpServletRequest req)
    throws UserException {
    userController.checkRecoveryKey(key);
    return Response.ok().build();
  }
  
  @POST
  @Path("/reset/password")
  @Produces(MediaType.APPLICATION_JSON)
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
  public Response qrCodeRecovery(@FormParam("key") String key, @Context HttpServletRequest req) throws UserException,
    MessagingException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setQRCode(userController.recoverQRCode(key));
    return Response.ok(json).build();
  }

  @POST
  @Path("/validate/email")
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateUserMail(@FormParam("key") String key, @Context HttpServletRequest req) throws UserException {
    authController.validateEmail(key);
    return Response.ok().build();
  }

  private void logoutAndInvalidateSession(HttpServletRequest req) throws UserException, InvalidationException {
    jWTHelper.invalidateToken(req);//invalidate iff req contains jwt token
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

  private Response login(Users user, String password, HttpServletRequest req) throws UserException,
      SigningKeyNotFoundException, NoSuchAlgorithmException, DuplicateSigningKeyException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
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

    json.setSessionID(req.getSession().getId());
    json.setData(user.getEmail());
    // JWT claims will be added by JWTHelper
    String token = jWTHelper.createToken(user, settings.getJWTIssuer(), null);
    return Response.ok().header(AUTHORIZATION, Constants.BEARER + token).entity(json).build();
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

  private boolean needLogin(HttpServletRequest req, Users user) {
    String remoteUser = req.getRemoteUser();
    Users tokenUser = jWTHelper.getUserPrincipal(req);
    boolean validToken = jWTHelper.validToken(req, settings.getJWTIssuer());

    if (isUserLoggedIn(remoteUser, tokenUser, validToken, user)) {
      return false;
    } else if (isSomeoneElseLoggedIn(remoteUser, tokenUser, validToken, user)) {
      try {
        logoutAndInvalidateSession(req);
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
    roles.append(req.isUserInRole("CLUSTER_AGENT") ? " cluster-agent}" : "}");
    LOGGER.log(Level.FINEST, "[/hopsworks-api] login:\n email: {0}\n session: {1}\n in roles: {2}", new Object[]{
      req.getUserPrincipal(), req.getSession().getId(), roles});
  }
}
