/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.auth.api;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;
import io.hops.hopsworks.common.project.CertsDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UserStatusValidator;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.remote.user.auth.RemoteUserAuthController;
import io.hops.hopsworks.remote.user.auth.jwt.JWTHelper;
import io.hops.hopsworks.remote.user.auth.ldap.LdapUserController;
import io.hops.hopsworks.remote.user.auth.oauth2.OAuthController;
import io.swagger.annotations.Api;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.PathParam;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import javax.ws.rs.core.SecurityContext;

@Path("/auth")
@Stateless
@Api(value = "Auth",
    description = "Authentication Resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthResource {

  private static final Logger LOGGER = Logger.getLogger(AuthResource.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private UserStatusValidator statusValidator;
  @EJB
  private AuthController authController;
  @EJB
  private ProjectController projectController;
  @EJB
  private RemoteUserAuthController remoteUserAuthController;
  @EJB
  private LdapUserController ldapUserController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;
  @EJB
  private OAuthController oAuthController;

  @GET
  @Path("krb/session")
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context HttpServletRequest req) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setData(req.getRemoteUser());
    return Response.ok().entity(json).build();
  }

  @POST
  @Path("ldap/login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response ldapLogin(@FormParam("username") String username, @FormParam("password") String password,
      @FormParam("chosenEmail") String chosenEmail, @FormParam("consent") boolean consent,
      @Context HttpServletRequest req) throws LoginException, UserException, NoSuchAlgorithmException,
      SigningKeyNotFoundException, DuplicateSigningKeyException {
    if (!settings.isLdapEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("Username can not be empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Password can not be empty.");
    }
    RemoteUserStateDTO ldapUserState = ldapUserController.login(username, password, consent, chosenEmail);
    return remoteUserLogin(ldapUserState, req);
  }

  @POST
  @Path("krb/login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response krbLogin(@FormParam("chosenEmail") String chosenEmail, @FormParam("consent") boolean consent,
      @Context SecurityContext sc, @Context HttpServletRequest req) throws LoginException {
    if (!settings.isKrbEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    String principalName = sc.getUserPrincipal() == null ? "" : sc.getUserPrincipal().getName();
    if (principalName.isEmpty()) {
      throw new IllegalArgumentException("User Principal Name not set.");
    }
    RemoteUserStateDTO krbLdapUserState = ldapUserController.getKrbLdapUser(principalName, consent, chosenEmail);
    try {
      return remoteUserLogin(krbLdapUserState, req);
    } catch (NoSuchAlgorithmException | SigningKeyNotFoundException | DuplicateSigningKeyException e) {
      RESTException ex = new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, Level.FINE, null,
          e.getMessage(), e);
      return Response.status(Response.Status.EXPECTATION_FAILED)
          .entity(ex.buildJsonResponse(new RESTApiJsonResponse(), settings.getHopsworksRESTLogLevel()))
          .type(MediaType.APPLICATION_JSON)
          .build();
    } catch (UserException ue) {
      return Response.status(Response.Status.EXPECTATION_FAILED)
          .entity(ue.buildJsonResponse(new RESTApiJsonResponse(), settings.getHopsworksRESTLogLevel()))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }
  
  @POST
  @Path("oauth/login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("code") String code, @FormParam("state") String state,
    @FormParam("chosenEmail") String chosenEmail,
    @FormParam("consent") boolean consent,
    @Context HttpServletRequest req) throws SigningKeyNotFoundException, NoSuchAlgorithmException,
    DuplicateSigningKeyException, UserException, LoginException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    RemoteUserStateDTO oAuthUserState = oAuthController.login(code, state, consent, chosenEmail);
    return remoteUserLogin(oAuthUserState, req);
  }

  @POST
  @Path("ldap/downloadCert/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response downloadCerts(@PathParam("projectId") Integer id, @FormParam("password") String password,
      @Context HttpServletRequest req) throws ProjectException, HopsSecurityException, DatasetException, UserException,
    SigningKeyNotFoundException, VerificationException, LoginException {
    Users user = jWTHelper.validateToken(req);
    RemoteUserStateDTO ldapUserState = ldapUserController.getLdapUser(user, password, false, "");
    return downloadCerts(ldapUserState, id);
  }

  @POST
  @Path("krb/downloadCert/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response downloadCerts(@PathParam("projectId") Integer id, @Context SecurityContext sc) throws
      ProjectException, HopsSecurityException, DatasetException, LoginException, UserException {
    String principalName = sc.getUserPrincipal() == null ? "" : sc.getUserPrincipal().getName();
    if (principalName.isEmpty()) {
      throw new IllegalArgumentException("User Principal Name not set.");
    }
    RemoteUserStateDTO ldapUserState = ldapUserController.getKrbLdapUser(principalName, false, "");
    return downloadCerts(ldapUserState, id);
  }
  
  @POST
  @Path("oauth/downloadCert/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response downloadCerts(@PathParam("projectId") Integer id, @Context HttpServletRequest req)
    throws HopsSecurityException, DatasetException, ProjectException, UserException, VerificationException,
    SigningKeyNotFoundException {
    Users user = jWTHelper.validateToken(req);
    RemoteUserStateDTO oAuthUserState = oAuthController.getOAuthUser(user);// no auth
    return downloadCerts(oAuthUserState, id);
  }
  
  private Response downloadCerts(RemoteUserStateDTO remoteUserState, Integer id) throws HopsSecurityException,
    ProjectException, UserException, DatasetException {
    Users user = remoteUserState.isSaved() && remoteUserState.getRemoteUser() != null ? remoteUserState.getRemoteUser()
      .getUid() : null;
    if (user == null || user.getEmail().equals(Settings.AGENT_EMAIL) ||
      !user.getMode().equals(UserAccountType.REMOTE_ACCOUNT_TYPE)) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.FINE);
    }
    remoteUserAuthController.checkProjectMembership(user, id);
    CertsDTO certsDTO = projectController.downloadCert(id, user);
    return Response.ok().entity(certsDTO).build();
  }

  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@Context HttpServletRequest req) throws UserException, InvalidationException {
    logoutAndInvalidateSession(req);
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
      throw new UserException(RESTCodes.UserErrorCode.LOGOUT_FAILURE, Level.SEVERE, null, e.getMessage(), e);
    }
  }

  private Response login(Users user, String password, HttpServletRequest req) throws UserException,
      SigningKeyNotFoundException, NoSuchAlgorithmException, DuplicateSigningKeyException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (user.getBbcGroupCollection() == null || user.getBbcGroupCollection().isEmpty()) {
      throw new UserException(RESTCodes.UserErrorCode.NO_ROLE_FOUND, Level.FINE, RESTCodes.UserErrorCode.NO_ROLE_FOUND.
          getMessage());
    }

    statusValidator.checkStatus(user.getStatus());
    try {
      req.login(user.getEmail(), password);
      authController.registerLogin(user, req);
    } catch (ServletException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
      authController.registerAuthenticationFailure(user);
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.SEVERE, null, e.getMessage(), e);
    }

    json.setSessionID(req.getSession().getId());
    json.setData(user.getEmail());
    String token = jWTHelper.createToken(user, settings.getJWTIssuer(), new HashMap<>());
    return Response.ok().header(AUTHORIZATION, Constants.BEARER + token).entity(json).build();
  }

  private Response remoteUserLogin(RemoteUserStateDTO remoteUserState, HttpServletRequest req) throws
      NoSuchAlgorithmException, SigningKeyNotFoundException, UserException, DuplicateSigningKeyException {
    if (remoteUserState.isSaved() && !needLogin(req, remoteUserState.getRemoteUser().getUid())) {
      return Response.ok().build();
    }
    if (!remoteUserState.isSaved()) {
      return Response.status(Response.Status.PRECONDITION_FAILED).entity(remoteUserState.getRemoteUserDTO()).build();
    }
    RemoteUser remoteUser = remoteUserState.getRemoteUser();
    if (remoteUser == null || remoteUser.getUid() == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.SEVERE,
          "Failed to get remote user from table.");
    }
    Users user = remoteUser.getUid();
    // Do pre cauth realm check 
    String passwordWithSalt = remoteUserAuthController.preRemoteUserLoginCheck(user, remoteUser.getAuthKey());
    return login(user, passwordWithSalt, req);
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

}
