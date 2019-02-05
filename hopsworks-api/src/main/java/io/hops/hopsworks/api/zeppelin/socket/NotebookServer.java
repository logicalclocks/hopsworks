/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.zeppelin.socket;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.hops.hopsworks.api.zeppelin.socket.Message.OP;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.display.Input;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.NotebookImportDeserializer;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.apache.zeppelin.util.WatcherSecurityKey;
import org.sonatype.aether.RepositoryException;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zeppelin websocket service.
 */
@ServerEndpoint(value = "/zeppelin/ws/{projectID}",
    configurator = ZeppelinEndpointConfig.class)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class NotebookServer {

  /**
   * Job manager service type
   */
  protected enum JOB_MANAGER_SERVICE {
    JOB_MANAGER_PAGE("JOB_MANAGER_PAGE");
    private String serviceTypeKey;

    JOB_MANAGER_SERVICE(String serviceType) {
      this.serviceTypeKey = serviceType;
    }

    String getKey() {
      return this.serviceTypeKey;
    }
  }

  private final Logger LOG = Logger.getLogger(NotebookServer.class.getName());
  private static Gson gson = new GsonBuilder()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .registerTypeAdapter(Date.class, new NotebookImportDeserializer())
      .setPrettyPrinting()
      .registerTypeAdapterFactory(Input.TypeAdapterFactory).create();

  private String sender;
  private Project project;
  private String userRole;
  private String hdfsUsername;
  private Session session;
  private NotebookServerImpl impl;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectFacade projectBean;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private NotebookServerImplFactory notebookServerImplFactory;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  
  public NotebookServer() {
  }

  @OnOpen
  public void open(Session conn, EndpointConfig config, @PathParam("projectID") String projectId)
    throws GenericException {
    try {
      this.session = conn;
      this.sender = (String) config.getUserProperties().get("user");
      this.project = getProject(projectId);
      authenticateUser(conn, this.project, this.sender);
      if (this.userRole == null) {
        LOG.log(Level.INFO, "User not authorized for Zeppelin Access: {0}", this.sender);
        return;
      }
      if (project.getPaymentType().equals(PaymentType.PREPAID)) {
        YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(project.getName());
        if (projectQuota == null || projectQuota.getQuotaRemaining() < 0) {
          session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "This project is out of credits."));
          return;
        }
      }
      this.impl = notebookServerImplFactory.getNotebookServerImps(project.getName(), conn);
      if (impl.getConf() == null) {
        impl.removeConnectedSockets(conn, notebookServerImplFactory);
        LOG.log(Level.INFO, "Could not create Zeppelin config for user: {0}, project: {1}", new Object[]{this.sender,
          project.getName()});
        return;
      }
      addUserConnection(this.hdfsUsername, conn);
      addUserConnection(project.getProjectGenericUser(), conn);
      this.session.getUserProperties().put("projectID", this.project.getId());
      String httpHeader = (String) config.getUserProperties().get(WatcherSecurityKey.HTTP_HEADER);
      this.session.getUserProperties().put(WatcherSecurityKey.HTTP_HEADER, httpHeader);
      impl.unicast(new Message(OP.CREATED_SOCKET), conn);
    } catch (IOException | RepositoryException | TaskRunnerException | InterruptedException ex) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null, ex.getMessage(), ex);
    }
  }

  @OnMessage
  public void onMessage(String msg, Session conn) {
    Notebook notebook = impl.notebook();
    try {
      Message messagereceived = deserializeMessage(msg);
      LOG.log(Level.FINE, "RECEIVE << {0}", messagereceived.op);
      LOG.log(Level.FINE, "RECEIVE PRINCIPAL << {0}", messagereceived.principal);
      LOG.log(Level.FINE, "RECEIVE TICKET << {0}", messagereceived.ticket);
      LOG.log(Level.FINE, "RECEIVE ROLES << {0}", messagereceived.roles);

      String ticket = TicketContainer.instance.getTicket(
          messagereceived.principal);
      Users user = userBean.findByEmail(this.sender);
      if (ticket != null && (messagereceived.ticket == null || !ticket.equals(messagereceived.ticket))) {

        /*
         * not to pollute logs, log instead of exception
         */
        if (StringUtils.isEmpty(messagereceived.ticket)) {
          LOG.log(Level.INFO, "{0} message: invalid ticket {1} != {2}", new Object[]{
            messagereceived.op, messagereceived.ticket, ticket});
        } else if (!messagereceived.op.equals(OP.PING)) {
          impl.sendMsg(conn, serializeMessage(new Message(OP.SESSION_LOGOUT).put("info",
              "Your ticket is invalid possibly due to server restart. Please login again.")));
        }
        try {
          session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,
              "Invalid ticket " + messagereceived.ticket + " != "
              + ticket));
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, null, ex);
        }
      }

      boolean allowAnonymous = impl.getConf().getConf().isAnonymousAllowed();
      if (!allowAnonymous && messagereceived.principal.equals("anonymous")) {
        throw new Exception("Anonymous access not allowed ");
      }

      messagereceived.principal = this.project.getProjectGenericUser();
      HashSet<String> userAndRoles = new HashSet<>();
      userAndRoles.add(messagereceived.principal);
      if (!messagereceived.roles.equals("")) {
        HashSet<String> roles = gson.fromJson(messagereceived.roles,
            new TypeToken<HashSet<String>>() {}.getType());
        if (roles != null) {
          userAndRoles.addAll(roles);
        }
      }

      AuthenticationInfo subject = new AuthenticationInfo(messagereceived.principal, messagereceived.roles,
          messagereceived.ticket);
      /**
       * Lets be elegant here
       */
      switch (messagereceived.op) {
        case LIST_NOTES:
          impl.unicastNoteList(conn, subject, userAndRoles);
          break;
        case RELOAD_NOTES_FROM_REPO:
          impl.broadcastReloadedNoteList(subject, userAndRoles);
          break;
        case GET_HOME_NOTE:
          impl.sendHomeNote(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case GET_NOTE:
          impl.sendNote(conn, userAndRoles, notebook, messagereceived, user, this.hdfsUsername);
          break;
        case NEW_NOTE:
          impl.createNote(conn, userAndRoles, notebook, messagereceived);
          break;
        case DEL_NOTE:
          impl.removeNote(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case REMOVE_FOLDER:
          impl.removeFolder(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case MOVE_NOTE_TO_TRASH:
          impl.moveNoteToTrash(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case MOVE_FOLDER_TO_TRASH:
          impl.moveFolderToTrash(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case EMPTY_TRASH:
          impl.emptyTrash(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case RESTORE_FOLDER:
          impl.restoreFolder(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case RESTORE_NOTE:
          impl.restoreNote(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case RESTORE_ALL:
          impl.restoreAll(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case CLONE_NOTE:
          impl.cloneNote(conn, userAndRoles, notebook, messagereceived);
          break;
        case IMPORT_NOTE:
          impl.importNote(conn, userAndRoles, notebook, messagereceived);
          break;
        case COMMIT_PARAGRAPH:
          impl.updateParagraph(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case RUN_PARAGRAPH:
          impl.runParagraph(conn, userAndRoles, notebook, messagereceived, user, certificateMaterializer, settings,
              dfsService);
          break;
        case PARAGRAPH_EXECUTED_BY_SPELL:
          impl.broadcastSpellExecution(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case RUN_ALL_PARAGRAPHS:
          impl.runAllParagraphs(conn, userAndRoles, notebook, messagereceived, user, certificateMaterializer, settings,
              dfsService);
          break;
        case CANCEL_PARAGRAPH:
          impl.cancelParagraph(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case MOVE_PARAGRAPH:
          impl.moveParagraph(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case INSERT_PARAGRAPH:
          impl.insertParagraph(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case COPY_PARAGRAPH:
          impl.copyParagraph(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case PARAGRAPH_REMOVE:
          impl.removeParagraph(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case PARAGRAPH_CLEAR_OUTPUT:
          impl.clearParagraphOutput(conn, userAndRoles, notebook, messagereceived, user, this.hdfsUsername);
          break;
        case PARAGRAPH_CLEAR_ALL_OUTPUT:
          impl.clearAllParagraphOutput(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case NOTE_UPDATE:
          impl.updateNote(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case NOTE_RENAME:
          impl.renameNote(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case FOLDER_RENAME:
          impl.renameFolder(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case UPDATE_PERSONALIZED_MODE:
          impl.updatePersonalizedMode(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case COMPLETION:
          impl.completion(conn, userAndRoles, notebook, messagereceived);
          break;
        case PING:
          break; //do nothing
        case ANGULAR_OBJECT_UPDATED:
          impl.angularObjectUpdated(conn, userAndRoles, notebook, messagereceived);
          break;
        case ANGULAR_OBJECT_CLIENT_BIND:
          impl.angularObjectClientBind(conn, userAndRoles, notebook, messagereceived);
          break;
        case ANGULAR_OBJECT_CLIENT_UNBIND:
          impl.angularObjectClientUnbind(conn, userAndRoles, notebook, messagereceived);
          break;
        case LIST_CONFIGURATIONS:
          impl.sendAllConfigurations(conn, userAndRoles, notebook);
          break;
        case CHECKPOINT_NOTE:
          impl.checkpointNote(conn, notebook, messagereceived);
          break;
        case LIST_REVISION_HISTORY:
          impl.listRevisionHistory(conn, notebook, messagereceived);
          break;
        case SET_NOTE_REVISION:
          impl.setNoteRevision(conn, userAndRoles, notebook, messagereceived, user);
          break;
        case NOTE_REVISION:
          impl.getNoteByRevision(conn, notebook, messagereceived);
          break;
        case LIST_NOTE_JOBS:
          impl.unicastNoteJobInfo(conn, messagereceived);
          break;
        case UNSUBSCRIBE_UPDATE_NOTE_JOBS:
          impl.unsubscribeNoteJobInfo(conn);
          break;
        case GET_INTERPRETER_BINDINGS:
          impl.getInterpreterBindings(conn, messagereceived);
          break;
        case SAVE_INTERPRETER_BINDINGS:
          impl.saveInterpreterBindings(conn, messagereceived, zeppelinResource);
          break;
        case EDITOR_SETTING:
          impl.getEditorSetting(conn, messagereceived);
          break;
        case GET_INTERPRETER_SETTINGS:
          impl.getInterpreterSettings(conn, subject);
          break;
        case WATCHER:
          impl.switchConnectionToWatcher(conn, messagereceived, this.hdfsUsername, notebookServerImplFactory);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      Level logLevel = Level.SEVERE;
      if (e.getMessage().contains("is not allowed to empty the Trash")) {
        logLevel = Level.INFO;
      }
      LOG.log(logLevel, "Can't handle message", e);
    }
  }

  @OnClose
  public void onClose(Session conn, CloseReason reason) {
    LOG.log(Level.INFO, "Closed connection to {0} : {1}. Reason: {2}",
        new Object[]{conn.getRequestURI().getHost(), conn.getRequestURI().getPort(), reason});
    impl.removeConnectionFromAllNote(conn);
    impl.removeConnectedSockets(conn, notebookServerImplFactory);
    impl.removeUserConnection(this.hdfsUsername, conn);
    impl.removeUserConnection(project.getProjectGenericUser(), conn);
  }

  @OnError
  public void onError(Session conn, Throwable exc) {
    if (impl != null) {
      impl.removeConnectionFromAllNote(conn);
      impl.removeConnectedSockets(conn, notebookServerImplFactory);
    }
  }

  private void addUserConnection(String user, Session conn) {
    if (impl.userConnectedSocketsContainsKey(user)) {
      impl.getUserConnectedSocket(user).add(conn);
    } else {
      Queue<Session> socketQueue = new ConcurrentLinkedQueue<>();
      socketQueue.add(conn);
      impl.putUserConnectedSocket(user, socketQueue);
    }
  }

  protected Message deserializeMessage(String msg) {
    return gson.fromJson(msg, Message.class);
  }

  protected String serializeMessage(Message m) {
    return gson.toJson(m);
  }

  public void closeConnection() {
    try {
      if (this.session.isOpen()) {
        this.session.getBasicRemote().sendText("Restarting zeppelin.");
        this.session.close(new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART, "Restarting zeppelin."));
      }
      impl.removeConnectionFromAllNote(this.session);
      impl.removeConnectedSockets(this.session, notebookServerImplFactory);
      impl.removeUserConnection(this.hdfsUsername, this.session);
      impl.removeUserConnection(project.getProjectGenericUser(), this.session);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  private Project getProject(String projectId) {
    Integer pId;
    Project proj;
    try {
      pId = Integer.valueOf(projectId);
      proj = projectBean.find(pId);
    } catch (NumberFormatException e) {
      return null;
    }
    return proj;
  }

  private void authenticateUser(Session session, Project project, String user) {
    //returns the user role in project. Null if the user has no role in project
    this.userRole = projectTeamBean.findCurrentRole(project, user);
    LOG.log(Level.FINEST, "User role in this project {0}", this.userRole);
    Users users = userBean.findByEmail(user);
    if (users == null || this.userRole == null) {
      try {
        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
            "You do not have a role in this project."));
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, null, ex);
      }
    }
    this.hdfsUsername = hdfsUsersController.getHdfsUserName(project, users);
  }
}
