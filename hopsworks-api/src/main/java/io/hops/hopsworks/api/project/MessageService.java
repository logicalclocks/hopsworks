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

package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.dataset.DatasetRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetRequestFacade;
import io.hops.hopsworks.common.dao.message.Message;
import io.hops.hopsworks.common.dao.message.MessageFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.message.MessageController;
import io.swagger.annotations.Api;
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
import org.elasticsearch.common.Strings;

@Path("/message")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Message Service", description = "Message Service")
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
  private DatasetRequestFacade dsReqFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllMessagesByUser(@Context SecurityContext sc) {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    List<Message> list = msgFacade.getAllMessagesTo(user);
    GenericEntity<List<Message>> msgs
            = new GenericEntity<List<Message>>(list) {};

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
            = new GenericEntity<List<Message>>(list) {};
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
    //Delete Dataset request from the database
    if (!Strings.isNullOrEmpty(msg.getSubject())) {
      DatasetRequest dsReq = dsReqFacade.findByMessageId(msg);
      if (dsReq != null) {
        dsReqFacade.remove(dsReq);
      }
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setUnread(false);
    msgFacade.update(msg);
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
    //Delete Dataset request from the database
    if (!Strings.isNullOrEmpty(msg.getSubject())) {
      DatasetRequest dsReq = dsReqFacade.findByMessageId(msg);
      if (dsReq != null) {
        dsReqFacade.remove(dsReq);
      }
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setDeleted(true);
    msgFacade.update(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @PUT
  @Path("restoreFromTrash/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response restoreFromTrash(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Message not found.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setDeleted(false);
    msgFacade.update(msg);
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            msg).build();
  }

  private void checkMsgUser(Message msg, Users user) throws AppException {
    if (!msg.getTo().equals(user)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Can not perform the rquested action.");
    }
  }
}
