package io.hops.hopsworks.api.user;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.user.UserStatusValidator;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

@Path("/auth")
@Stateless
@Api(value = "Auth", description = "Authentication service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthService {

  @EJB
  private UserFacade userBean;
  @EJB
  private UserManager userManager;
  @EJB
  private UsersController userController;
  @EJB
  private UserStatusValidator statusValidator;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private AuditManager am;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private EmailBean emailBean;

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
    Users user = userManager.getUserByEmail(sc.getUserPrincipal().getName());
    if (user.getPassword().equals(DigestUtils.sha256Hex(password))) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).build();
  }
  
  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email,
          @FormParam("password") String password, @FormParam("otp") String otp,
          @Context SecurityContext sc,
          @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
          throws AppException, MessagingException {

    req.getServletContext().log("email: " + email);
    req.getServletContext().log("SESSIONID@login: " + req.getSession().getId());
    req.getServletContext().log("SecurityContext: " + sc.getUserPrincipal());
    req.getServletContext().log("SecurityContext in user role: " + sc.isUserInRole("HOPS_USER"));
    req.getServletContext().log("SecurityContext in sysadmin role: " + sc.isUserInRole("HOPS_ADMIN"));
    req.getServletContext().log("SecurityContext in agent role: " + sc.isUserInRole("AGENT"));
    req.getServletContext().log("SecurityContext in cluster_agent role: " + sc.isUserInRole("CLUSTER_AGENT"));
    JsonResponse json = new JsonResponse();
    if (email == null || email.isEmpty()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),"Email address field cannot be empty");
    }
    Users user = userBean.findByEmail(email);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              "Unrecognized email address. Have you registered yet?");
    }
    String newPassword = null;
    Variables varTwoFactor = settings.findById("twofactor_auth");
    Variables varExclude = settings.findById("twofactor-excluded-groups");
    String twoFactorMode = (varTwoFactor != null ? varTwoFactor.getValue() : "");
    String excludes = (varExclude != null ? varExclude.getValue() : null);
    String[] groups = (excludes != null && !excludes.isEmpty() ? excludes.split(";") : null);
    if (!isInGroup(user, groups)) {
      if ((twoFactorMode.equals("mandatory") || (twoFactorMode.equals("true") && user.getTwoFactor()))) {
        if (otp == null || otp.isEmpty() && user.getMode().equals(PeopleAccountType.M_ACCOUNT_TYPE)) {
          if (user.getPassword().equals(DigestUtils.sha256Hex(password))) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Second factor required.");
          }
        }
      }
    }
    // Add padding if custom realm is disabled
    if (otp == null || otp.isEmpty() && user.getMode().equals(PeopleAccountType.M_ACCOUNT_TYPE)) {
      otp = AuthenticationConstants.MOBILE_OTP_PADDING;
    }
    if (otp.length() == AuthenticationConstants.MOBILE_OTP_PADDING.length()
            && user.getMode().equals(PeopleAccountType.M_ACCOUNT_TYPE)) {
      newPassword = password + otp;
    } else if (otp.length() == AuthenticationConstants.YUBIKEY_OTP_PADDING.
            length() && user.getMode().equals(PeopleAccountType.Y_ACCOUNT_TYPE)) {
      newPassword = password + otp + AuthenticationConstants.YUBIKEY_USER_MARKER;
    } else {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not recognize the account type. Report a bug.");
    }
    // logout any user already loggedin if a new user tries to login 
    if (sc.getUserPrincipal() != null && !sc.getUserPrincipal().getName().equals(email)) {
      am.registerLoginInfo(user, UserAuditActions.UNAUTHORIZED.getValue(), UserAuditActions.FAILED.name(), req);
      userController.setUserIsOnline(user, AuthenticationConstants.IS_OFFLINE);
      try {
        req.getServletContext().log("logging out. User: " + sc.getUserPrincipal().getName());
        req.logout();
      } catch (ServletException e) {
        userController.registerFalseLogin(user);
        am.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.FAILED.name(), req);
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
      }
    }
    //only login if not already logged...
    if (sc.getUserPrincipal() == null) {
      if (statusValidator.checkStatus(user.getStatus())) {
        try {
          req.getServletContext().log("going to login. User status: " + user.getStatus());
          req.login(email, newPassword);
          req.getServletContext().log("3 step: " + email);
          userController.resetFalseLogin(user);
          am.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.SUCCESS.name(), req);
        } catch (ServletException e) {
          userController.registerFalseLogin(user);
          am.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.FAILED.name(), req);
          throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
        }
      } else { // if user == null
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), ResponseMessages.AUTHENTICATION_FAILURE);
      }
    } else {
      req.getServletContext().log("Skip logged because already logged in: " + email);
    }

    userController.setUserIsOnline(user, AuthenticationConstants.IS_ONLINE);
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

    Users user = null;
    JsonResponse json = new JsonResponse();

    try {
      req.getSession().invalidate();
      req.logout();
      json.setStatus("SUCCESS");
      user = userBean.findByEmail(req.getRemoteUser());
      if (user != null) {
        userController.setUserIsOnline(user, AuthenticationConstants.IS_OFFLINE);
        am.registerLoginInfo(user, UserAuditActions.LOGOUT.name(),
                UserAuditActions.SUCCESS.name(), req);
        //remove zeppelin ticket for user
        TicketContainer.instance.invalidate(user.getEmail());
      }
    } catch (ServletException e) {

      am.registerLoginInfo(user, UserAuditActions.LOGOUT.name(),
              UserAuditActions.FAILED.name(), req);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Logout failed on backend");
    }
    return Response.ok().entity(json).build();
  }

  @GET
  @Path("isAdmin")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public Response login(@Context SecurityContext sc,
          @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
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
  public Response register(UserDTO newUser, @Context HttpServletRequest req)
          throws AppException, SocketException, NoSuchAlgorithmException {

    byte[] qrCode = null;

    JsonResponse json = new JsonResponse();

    qrCode = userController.registerUser(newUser, req);

    if (settings.findById("twofactor_auth").getValue().equals("mandatory")
            || (settings.findById("twofactor_auth").getValue().equals("true")
            && newUser.isTwoFactor())) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage(
              "We registered your account request. "
              + "Please validate you email and we will "
              + "review your account within 48 hours.");
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
          throws AppException, SocketException, NoSuchAlgorithmException {

    JsonResponse json = new JsonResponse();

    userController.registerYubikeyUser(newUser, req);

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

  @GET
  @Path("/validation/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSecurityQuestion(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("key") String key) throws AppException {
    if (key == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the validation key should not be null");
    }
    if (key.length() <= AuthenticationConstants.USERNAME_LENGTH) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the validation key is invalid");
    }
    String userName = key.substring(0, AuthenticationConstants.USERNAME_LENGTH);
    // get the 8 char username
    String secret = key.substring(AuthenticationConstants.USERNAME_LENGTH);

    Users user = userBean.findByUsername(userName);

    if (user == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the user does not exist");
    }

    if (secret.equals(user.getValidationKey())) {
      switch (user.getStatus()) {
        case ACTIVATED_ACCOUNT:
          GenericEntity<SecurityQuestion> result = new GenericEntity<SecurityQuestion>(user.getSecurityQuestion()) {
          };
          return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
        case NEW_MOBILE_ACCOUNT:
        case NEW_YUBIKEY_ACCOUNT:
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user has not been validated");
        default:
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user has been blocked");
      }
    } else {
      int nbTry = user.getFalseLogin() + 1;
      user.setFalseLogin(nbTry);
      // if more than 5 times false logins set as spam
      if (nbTry > AuthenticationConstants.ACCOUNT_VALIDATION_TRIES) {
        user.setStatus(PeopleAccountStatus.SPAM_ACCOUNT);
      }
      userBean.update(user);
      String email = sc.getUserPrincipal().getName();
      Users initiator = userBean.findByEmail(email);
      am.registerRoleChange(initiator,
          PeopleAccountStatus.SPAM_ACCOUNT.name(), RolesAuditActions.SUCCESS.
          name(), Integer.toString(nbTry), user, req);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Wrong vallidation key");
    }
  }
  
  @POST
  @Path("/validation/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateUserMail(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("key") String key) throws AppException {
    if (key == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the validation key should not be null");
    }
    if (key.length() <= AuthenticationConstants.USERNAME_LENGTH) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the validation key is invalid");
    }
    String userName = key.substring(0, AuthenticationConstants.USERNAME_LENGTH);
    // get the 8 char username
    String secret = key.substring(AuthenticationConstants.USERNAME_LENGTH);

    Users user = userBean.findByUsername(userName);

    if (user == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the user does not exist");
    }

    if (secret.equals(user.getValidationKey())) {
      if (!user.getStatus().equals(PeopleAccountStatus.NEW_MOBILE_ACCOUNT)
          && !user.getStatus().equals(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT)) {
        switch (user.getStatus()) {
          case VERIFIED_ACCOUNT:
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "This user is already verified, but still need to be activated by the administrator");
          case ACTIVATED_ACCOUNT:
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user is already verified");
          default:
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user has been blocked");
        }
      }

      user.setStatus(PeopleAccountStatus.VERIFIED_ACCOUNT);
      user = userBean.update(user);
      String email = sc.getUserPrincipal().getName();
      Users initiator = userBean.findByEmail(email);
      am.registerRoleChange(initiator,
          PeopleAccountStatus.VERIFIED_ACCOUNT.name(), RolesAuditActions.SUCCESS.
          name(), "", user, req);
      GenericEntity<Users> result = new GenericEntity<Users>(user) {
      };

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(result).build();
    } else {
      int nbTry = user.getFalseLogin() + 1;
      user.setFalseLogin(nbTry);
      // if more than 5 times false logins set as spam
      if (nbTry > AuthenticationConstants.ACCOUNT_VALIDATION_TRIES) {
        user.setStatus(PeopleAccountStatus.SPAM_ACCOUNT);
      }
      userBean.update(user);
      String email = sc.getUserPrincipal().getName();
      Users initiator = userBean.findByEmail(email);
      am.registerRoleChange(initiator,
          PeopleAccountStatus.SPAM_ACCOUNT.name(), RolesAuditActions.SUCCESS.
          name(), Integer.toString(nbTry), user, req);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Wrong vallidation key");
    }
  }
  
  @POST
  @Path("/recovery")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getSecurityQuestionMail(@Context SecurityContext sc, @Context HttpServletRequest req,
      @FormParam("email") String email) throws AppException {
    Users u = userBean.findByEmail(email);
    if (u == null || u.getStatus().equals(PeopleAccountStatus.DEACTIVATED_ACCOUNT) || u.getStatus().equals(
        PeopleAccountStatus.BLOCKED_ACCOUNT) || u.getStatus().equals(PeopleAccountStatus.SPAM_ACCOUNT)) {
      //if account blocked then ignore the request
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    } else if (u.getStatus().equals(PeopleAccountStatus.NEW_MOBILE_ACCOUNT) || u.getStatus().equals(
        PeopleAccountStatus.NEW_MOBILE_ACCOUNT)) {
      //if new account resend validation eamil
      String activationKey = SecurityUtils.getRandomPassword(64);
      try {
        emailBean.sendEmail(u.getEmail(), Message.RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
            UserAccountsEmailMessages.buildMobileRequestMessageRest(settings.getVerificationEndpoint(), u.getUsername()
                + activationKey));
      } catch (MessagingException e) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "we did not manage to send the email, the error was: " + e.getMessage());
      }
      u.setValidationKey(activationKey);
      userBean.update(u);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }else{
      String activationKey = SecurityUtils.getRandomPassword(64);
      try {
        emailBean.sendEmail(u.getEmail(), Message.RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_PASSWORD_RECOVERY_SUBJECT,
            UserAccountsEmailMessages.buildPasswordRecoveryMessage(settings.getRecoveryEndpoint(), u.getUsername()
                + activationKey));
      } catch (MessagingException e) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "we did not manage to send the email, the error was: " + e.getMessage());
      }
      u.setValidationKey(activationKey);
      userBean.update(u);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
  }
  
  private boolean isInGroup(Users user, String[] groups) {
    Collection<BbcGroup> userGroups = user.getBbcGroupCollection();
    if (userGroups == null || userGroups.isEmpty()) {
      return false;
    }
    if (groups == null || groups.length == 0) {
      return false;
    }
    BbcGroup group;
    for (String groupName : groups) {
      if (groupName.isEmpty()) {
        continue;
      }
      group = bbcGroupFacade.findByGroupName(groupName);
      if (userGroups.contains(group)) {
        return true;
      }
    }
    return false;
  }

}
