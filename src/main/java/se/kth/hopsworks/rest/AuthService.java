package se.kth.hopsworks.rest;

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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.binary.Base64;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.LoginAuditActions;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.controller.UserStatusValidator;
import se.kth.hopsworks.controller.UsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserDTO;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.util.Settings;

@Path("/auth")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthService {

  @EJB
  private UserFacade userBean;
  @EJB
  private UsersController userController;
  @EJB
  private UserStatusValidator statusValidator;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  Settings settings;
  @EJB
  AuditManager am;

  @GET
  @Path("session")
  @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    req.getServletContext().log("SESSIONID: " + req.getSession().getId());
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
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email,
          @FormParam("password") String password, @FormParam("otp") String otp,
          @Context SecurityContext sc,
          @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
          throws AppException, MessagingException {

    JsonResponse json = new JsonResponse();
    Users user = userBean.findByEmail(email);

    // Add padding if custom realm is disabled
    if (otp == null || otp.isEmpty()) {
      otp = AuthenticationConstants.MOBILE_OTP_PADDING;
    }

    if (otp.length() == AuthenticationConstants.MOBILE_OTP_PADDING.length()) {
      password = password + otp;
    } else if (otp.length() == AuthenticationConstants.YUBIKEY_OTP_PADDING.
            length()) {
      password = password + otp + AuthenticationConstants.YUBIKEY_USER_MARKER;
    }

    //only login if not already logged in...
    if (sc.getUserPrincipal() == null) {
      if (user != null && statusValidator.checkStatus(user.getStatus())) {
        try {

          req.login(email, password);
          userController.resetFalseLogin(user);
          am.registerLoginInfo(user, LoginAuditActions.LOGIN.name(), "SUCCESS", req);

          //if the logedin user has no supported role logout
          if (!sc.isUserInRole("BBC_USER") && !sc.isUserInRole("SYS_ADMIN")) {
            req.logout();
            throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                    "No valid role found for this user");
          }

        } catch (ServletException e) {
          userController.registerFalseLogin(user);
          am.registerLoginInfo(user, LoginAuditActions.LOGIN.name(), "FAILED", req);
          throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                  ResponseMessages.AUTHENTICATION_FAILURE);
        }
      } else { // if user == null
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                ResponseMessages.AUTHENTICATION_FAILURE);
      }
    } else {
      req.getServletContext().log("Skip logged because already logged in: "
              + email);
    }

    //read the user data from db and return to caller
    json.setStatus("SUCCESS");
    json.setSessionID(req.getSession().getId());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    try {
      req.logout();
      json.setStatus("SUCCESS");
      req.getSession().invalidate();
    } catch (ServletException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Logout failed on backend");
    }
    return Response.ok().entity(json).build();
  }

  @POST
  @Path("register")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response register(UserDTO newUser, @Context HttpServletRequest req)
          throws AppException {

    byte[] qrCode = null;

    JsonResponse json = new JsonResponse();

    String domain = req.getRequestURL().toString();
    String cpath = req.getContextPath().toString();

    String url = domain.substring(0, domain.indexOf(cpath));

    url = url + cpath;

    String ip = req.getRemoteAddr();
    String browser = "Fix this";
    String mac = "Fix this";
    String os = "Fix this";

    qrCode = userController.registerUser(newUser, url, ip, browser, os, mac);

    if (settings.findById("twofactor_auth").getValue().equals("true")) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage(
              "We registered your account request. Please validate you email and we will review your account within 48 hours.");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("registerYubikey")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response registerYubikey(UserDTO newUser,
          @Context HttpServletRequest req)
          throws AppException {

    req.getServletContext().log("Registering..." + newUser.getEmail() + ", "
            + newUser.getFirstName());

    JsonResponse json = new JsonResponse();

    String domain = req.getRequestURL().toString();
    String cpath = req.getContextPath().toString();

    String url = domain.substring(0, domain.indexOf(cpath));

    url = url + cpath;

    String ip = req.getRemoteAddr();
    String browser = "Fix this";
    String mac = "Fix this";
    String os = "Fix this";
    userController.registerYubikeyUser(newUser, url, ip, browser, os, mac);

    req.getServletContext().log("successfully registered new user: '" + newUser.
            getEmail() + "'");

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("recoverPassword")
  @Produces(MediaType.APPLICATION_JSON)
  public Response recoverPassword(@FormParam("email") String email,
          @FormParam("securityQuestion") String securityQuestion,
          @FormParam("securityAnswer") String securityAnswer,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    userController.recoverPassword(email, securityQuestion, securityAnswer, req);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.PASSWORD_RESET_SUCCESSFUL);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

}
