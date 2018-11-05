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

package io.hops.hopsworks.api.metadata.wscomm;

import io.hops.hopsworks.api.metadata.wscomm.message.Message;
import io.hops.hopsworks.api.metadata.wscomm.message.TextMessage;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.MetadataException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/wspoint/{projectID}",
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class,
        configurator = ServletAwareConfig.class)
public class WebSocketEndpoint {

  private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.
          getName());

  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ProjectFacade projectFacade;
  private String sender;
  private Project project;
  private String userRole;
  private HttpSession httpSession;//this might be used to check the underlying http session
  @EJB
  private MetadataProtocol protocol;

  @OnOpen
  public void open(Session session, EndpointConfig config,
          @PathParam("projectID") String projectId) {

    this.sender = (String) config.getUserProperties().get("user");
    this.httpSession = (HttpSession) config.getUserProperties().get(
            "httpSession");

    this.project = this.getProject(projectId);

    if (this.project == null) {
      try {
        sendError(session, "Project does not exist.");
        session.close();
        return;
      } catch (IOException ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }

    Users user = this.projectTeamFacade.findUserByEmail(this.sender);
    //returns the user role in project. Null if the user has no role in project
    this.userRole = this.projectTeamFacade.findCurrentRole(this.project, user);
    logger.log(Level.FINEST, "User role in this project {0}", this.userRole);

    if (this.userRole == null) {
      try {
        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                "You do not have a role in this project."));
      } catch (IOException ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
    session.getUserProperties().put("projectID", this.project.getId());
  }

  @OnMessage
  public void message(Session session, Message msg) throws GenericException, MetadataException {
    //query string is the client I want to communicate with
    String receiver = session.getQueryString();

    logger.log(Level.FINEST, "RECEIVED MESSAGE: {0}", msg.toString());
    Message response = this.protocol.GFR(msg);
    //broadcast the response back to everybody in the same project
    this.broadcast(response, session);
  }

  @OnClose
  public void onClose(Session session) {
    logger.log(Level.INFO,
            "HOPSWORKS: USER {0} SESSION DESTROYED sessions {1}",
            new Object[]{this.sender, session.getOpenSessions().size()});
    Message message = new TextMessage(this.sender, " Left");
    message.setStatus("INFO");
    this.broadcast(message, session);
  }

  //broadcast to every one connected to the same project
  private void broadcast(Message msg, Session session) {
    for (Session s : session.getOpenSessions()) {
      try {
        if (s.isOpen() && s.getUserProperties().get("projectID").equals(
                session.getUserProperties().get("projectID"))) {
          s.getBasicRemote().sendObject(msg);//RemoteEndpoint.Basic interface provides blocking methods to send
          //messages; the RemoteEndpoint.Async interface provides nonblocking methods.
          //logger.log(Level.INFO, "Sending >>> {0} to session: {1}",
          //new Object[]{msg, s.getUserPrincipal()});
        }
      } catch (IOException | EncodeException ex) {
        this.sendError(session, ex.getMessage());
      }
    }
  }

  @OnError
  public void error(Session session, Throwable t) {
    logger.log(Level.SEVERE, t.getMessage(), t);
  }

  private void sendClient(Session session, Message message) {
    try {
      session.getBasicRemote().sendObject(message);
    } catch (IOException | EncodeException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private void sendError(Session session, String err) {
    String error = String.format("error: %s", err);
    Message message = new TextMessage("Server", error);
    message.setStatus("ERROR");
    this.sendClient(session, message);
  }

  private Project getProject(String projectId) {
    Integer pId;
    Project proj;
    try {
      pId = Integer.valueOf(projectId);
      proj = this.projectFacade.find(pId);
    } catch (NumberFormatException e) {
      return null;
    }
    return proj;
  }

}
