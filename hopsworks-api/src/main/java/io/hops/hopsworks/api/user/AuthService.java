package io.hops.hopsworks.api.user;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UserStatusValidator;
import io.hops.hopsworks.common.user.UsersController;
import io.swagger.annotations.Api;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
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

  @GET
  @Path("session")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    try {
      json.setStatus("SUCCESS");
      json.setData(sc.getUserPrincipal().getName());
    } catch (Exception e) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          ResponseMessages.AUTHENTICATION_FAILURE);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }

  @POST
  @Path("validatePassword")
  @Produces(MediaType.APPLICATION_JSON)
  public Response validatePassword(@FormParam("password") String password, @Context SecurityContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
      throws AppException, MessagingException {
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    if (authController.validatePassword(user, password)) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).build();
  }

  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email, @FormParam("password") String password,
      @FormParam("otp") String otp, @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException,
      MessagingException {
    logUserLogin(req);
    JsonResponse json = new JsonResponse();
    if (email == null || email.isEmpty()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Email address field cannot be empty");
    }
    
    // Do pre cauth realm check 
    String newPassword = authController.preCustomRealmLoginCheck(email, password, otp, req);
    
    // logout any user already loggedin if a new user tries to login 
    if (req.getRemoteUser() != null && !req.getRemoteUser().equals(email)) {
      logoutAndInvalidateSession(req);
    }
    //only login if not already logged...
    if (sc.getUserPrincipal() == null) {
      login(email, newPassword, req);
    } else {
      req.getServletContext().log("Skip logged because already logged in: " + email);
    }

    //read the user data from db and return to caller
    json.setStatus("SUCCESS");
    json.setSessionID(req.getSession().getId());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
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
      return Response.ok().build();
    }
    return Response.status(Response.Status.UNAUTHORIZED).build();
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
  @Path("registerYubikey")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response registerYubikey(UserDTO newUser, @Context HttpServletRequest req) throws AppException,
      SocketException, NoSuchAlgorithmException {
    JsonResponse json = new JsonResponse();
    userController.registerYubikeyUser(newUser, req);
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
    Users user = null;
    try {
      req.getSession().invalidate();
      req.logout();
      user = userFacade.findByEmail(req.getRemoteUser());
      if (user != null) {
        authController.setUserOnlineStatus(user, AuthenticationConstants.IS_OFFLINE);
        accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGOUT.name(), UserAuditActions.SUCCESS.name(), 
            req);
        //remove zeppelin ticket for user
        TicketContainer.instance.invalidate(user.getEmail());
      }
    } catch (ServletException e) {
      accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGOUT.name(), UserAuditActions.FAILED.name(), req);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Logout failed on backend");
    }
  }
  
  private void login(String email, String password, HttpServletRequest req) throws AppException, MessagingException {
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "Unrecognized email address. Have you registered yet?");
    }
    if (statusValidator.checkStatus(user.getStatus())) {
      try {
        req.getServletContext().log("going to login. User status: " + user.getStatus());
        req.login(email, password);
        req.getServletContext().log("3 step: " + email);
        authController.resetFalseLogin(user);
        accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.SUCCESS.name(), req);
        authController.setUserOnlineStatus(user, AuthenticationConstants.IS_ONLINE);
      } catch (ServletException e) {
        authController.registerFalseLogin(user, req);
        accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.FAILED.name(), req);
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
      }
    } else { // if user == null
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
    }
  }

  private void logUserLogin(HttpServletRequest req) {
    req.getServletContext().log("SESSIONID@login: " + req.getSession().getId());
    req.getServletContext().log("SecurityContext email: " + req.getUserPrincipal());
    req.getServletContext().log("SecurityContext in user role: " + req.isUserInRole("HOPS_USER"));
    req.getServletContext().log("SecurityContext in sysadmin role: " + req.isUserInRole("HOPS_ADMIN"));
    req.getServletContext().log("SecurityContext in agent role: " + req.isUserInRole("AGENT"));
    req.getServletContext().log("SecurityContext in cluster_agent role: " + req.isUserInRole("CLUSTER_AGENT"));
  }
}
