package se.kth.hopsworks.rest;

import java.util.List;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.hopsworks.message.Message;
import se.kth.hopsworks.message.MessageFacade;
import se.kth.hopsworks.message.controller.MessageController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;

@Path("/message")
@Stateless
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MessageService {

  private final static Logger logger = Logger.getLogger(MessageService.class.
          getName());
  @EJB
  private MessageController msgController;
  @EJB
  private MessageFacade msgFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllMessagesByUser(@Context SecurityContext sc) {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    List<Message> list = msgFacade.getAllMessagesTo(user);
    GenericEntity<List<Message>> msgs
            = new GenericEntity<List<Message>>(list) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            msgs).build();
  }

  @GET
  @Path("deleted")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllDeletedMessagesByUser(@Context SecurityContext sc) {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    List<Message> list = msgFacade.getAllDeletedMessagesTo(user);
    GenericEntity<List<Message>> msgs
            = new GenericEntity<List<Message>>(list) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            msgs).build();
  }

  @GET
  @Path("countUnread")
  @Produces(MediaType.APPLICATION_JSON)
  public Response countUnreadMessagesByUser(@Context SecurityContext sc) {
    JsonResponse json = new JsonResponse();
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Long unread = msgFacade.countUnreadMessagesTo(user);
    json.setData(unread);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @PUT
  @Path("markAsRead/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response markAsRead(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Message not found.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setUnread(false);
    msgFacade.edit(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @PUT
  @Path("moveToTrash/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response moveToTrash(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Message not found.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setDeleted(true);
    msgFacade.edit(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @DELETE
  @Path("{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteMessage(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Message not found.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msgFacade.remove(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @DELETE
  @Path("empty")
  @Produces(MediaType.APPLICATION_JSON)
  public Response emptyTrash(@Context SecurityContext sc) throws AppException {
    JsonResponse json = new JsonResponse();
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    int rowsAffected = msgFacade.emptyTrash(user);
    json.setSuccessMessage(rowsAffected + " messages deleted.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("reply/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.TEXT_PLAIN)
  public Response reply(@PathParam("msgId") Integer msgId,
          String content,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Message not found.");
    }
    if (content == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "No content.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    try {
      msgController.reply(user, msg, content);
    } catch (IllegalArgumentException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              e.getMessage());
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void checkMsgUser(Message msg, Users user) throws AppException {
    if (!msg.getTo().equals(user)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Can not perform the rquested action.");
    }
  }
}
