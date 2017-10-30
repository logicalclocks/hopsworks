package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.core.SecurityContext;

@Path("/admin")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersAdmin {

  @EJB
  private UserFacade userFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private AuditManager auditManager;
  @EJB
  private EmailBean emailBean;
  @EJB
  private Settings settings;

  @GET
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUsers(@Context SecurityContext sc, @Context HttpServletRequest req,
      @QueryParam("status") String filter) throws AppException{
    List<Users> list = new ArrayList<>();
    if (filter == null) {
      list = userFacade.findAllUsers();
    } else {
      String[] filterStrings = filter.split(",");
      for (String filterString : filterStrings) {
        PeopleAccountStatus status;
        try{
          status = PeopleAccountStatus.valueOf(filterString);
        } catch (IllegalArgumentException ex) {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "the folloing status does not exist: "
              + filterString);
        }
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
  public Response getUser(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("email") String email) throws AppException {
    Users u = userFacade.findByEmail(email);
    if (u == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "no user corresponding to this email");
    }
    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateUser(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("email") String email, Users user) throws AppException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      if (user.getStatus() != null) {
        u.setStatus(user.getStatus());
        u = userFacade.update(u);
        String initiatorEmail = sc.getUserPrincipal().getName();
        Users initiator = userFacade.findByEmail(initiatorEmail);
        auditManager.registerRoleChange(initiator,
            AccountsAuditActions.CHANGEDSTATUS.name(), AccountsAuditActions.SUCCESS.
            name(), u.getStatusName(), u, req);

      }
      if (user.getBbcGroupCollection() != null) {
        u.setBbcGroupCollection(user.getBbcGroupCollection());
        u = userFacade.update(u);
        String result = "";
        for (BbcGroup group : u.getBbcGroupCollection()) {
          result = result + group.getGroupName() + ", ";
        }
        String initiatorEmail = sc.getUserPrincipal().getName();
        Users initiator = userFacade.findByEmail(initiatorEmail);
        auditManager.registerRoleChange(initiator,
            RolesAuditActions.UPDATEROLES.name(), RolesAuditActions.SUCCESS.
            name(), result, u, req);
      }
      if (user.getMaxNumProjects() != null) {
        u.setMaxNumProjects(user.getMaxNumProjects());
        u = userFacade.update(u);

      }
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user does not exist");
    }
    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}/accepted")
  @Produces(MediaType.APPLICATION_JSON)
  public Response acceptUser(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("email") String email, Users user) throws AppException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      if (u.getStatus().equals(PeopleAccountStatus.VERIFIED_ACCOUNT)) {
        Collection<BbcGroup> groups = user.getBbcGroupCollection();
        if (groups == null || groups.isEmpty()) {
          BbcGroup bbcGroup = bbcGroupFacade.findByGroupName("HOPS_USER");
          groups = new ArrayList<BbcGroup>();
          groups.add(bbcGroup);
        }
        u.setStatus(PeopleAccountStatus.ACTIVATED_ACCOUNT);
        u.setBbcGroupCollection(groups);
        u = userFacade.update(u);
        String result = "";
        for (BbcGroup group : u.getBbcGroupCollection()) {
          result = result + group.getGroupName() + ", ";
        }
        String initiatorEmail = sc.getUserPrincipal().getName();
        Users initiator = userFacade.findByEmail(initiatorEmail);
        auditManager.registerRoleChange(initiator,
            RolesAuditActions.UPDATEROLES.name(), RolesAuditActions.SUCCESS.
            name(), result, u, req);
        auditManager.registerRoleChange(initiator, PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
            AccountsAuditActions.SUCCESS.name(), "", u, req);
        sendConfirmationMail(u);
      } else {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The user can't transition from status "
            + u.getStatus().name() + " to status " + PeopleAccountStatus.ACTIVATED_ACCOUNT.name());
      }
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user does not exist");
    }

    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}/rejected")
  @Produces(MediaType.APPLICATION_JSON)
  public Response rejectUser(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("email") String email) throws AppException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      u.setStatus(PeopleAccountStatus.SPAM_ACCOUNT);
      u = userFacade.update(u);
      String initiatorEmail = sc.getUserPrincipal().getName();
      Users initiator = userFacade.findByEmail(initiatorEmail);

      auditManager.registerRoleChange(initiator, PeopleAccountStatus.SPAM_ACCOUNT.name(),
          AccountsAuditActions.SUCCESS.name(), "", u, req);
      sendRejectionEmail(u);
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user does not exist");
    }

    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @POST
  @Path("/users/{email}/pending")
  @Produces(MediaType.APPLICATION_JSON)
  public Response pendingUser(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("email") String email) throws AppException {
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      if (u.getStatus().equals(PeopleAccountStatus.NEW_MOBILE_ACCOUNT) || u.getStatus().equals(
          PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT)) {
        u = resendAccountVerificationEmail(u, req);
      } else {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "The user can't transition from status "
            + u.getStatus().name() + " to a pending status");
      }
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "This user does not exist");
    }

    GenericEntity<Users> result = new GenericEntity<Users>(u) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(result).build();
  }

  @GET
  @Path("/usergroups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllGroups(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<BbcGroup> list = bbcGroupFacade.findAll();
    GenericEntity<List<BbcGroup>> groups = new GenericEntity<List<BbcGroup>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(groups).build();
  }

  private void sendConfirmationMail(Users user) throws AppException {
    try {
      //send confirmation email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
          UserAccountsEmailMessages.
          accountActivatedMessage(user.getEmail()));
    } catch (MessagingException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "we did not manage to send the email, the error was: " + e.getMessage());
    }
  }

  private Users resendAccountVerificationEmail(Users user, HttpServletRequest req) throws AppException {
    try {
      String activationKey = SecurityUtils.getRandomPassword(64);
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
          UserAccountsEmailMessages.buildMobileRequestMessageRest(settings.getVerificationEndpoint(), user.getUsername()
              + activationKey));
      user.setValidationKey(activationKey);
      return userFacade.update(user);
    } catch (MessagingException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "we did not manage to send the email, the error was: " + e.getMessage());
    }
  }

  private void sendRejectionEmail(Users user) throws AppException {
    try {
      // Send rejection email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_REJECT,
          UserAccountsEmailMessages.accountRejectedMessage());
    } catch (MessagingException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "we did not manage to send the email, the error was: " + e.getMessage());
    }
  }

}
