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
package se.kth.hopsworks.zeppelin.socket;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.websocket.server.ServerEndpoint;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import org.apache.zeppelin.display.AngularObject;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.display.AngularObjectRegistryListener;
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterOutput;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.interpreter.remote.RemoteAngularObjectRegistry;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterProcessListener;
import org.apache.zeppelin.notebook.JobListenerFactory;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.ParagraphJobListener;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.scheduler.Job.Status;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.apache.zeppelin.search.SearchService;
import org.quartz.SchedulerException;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;
import se.kth.hopsworks.zeppelin.socket.Message.OP;

/**
 * Zeppelin websocket service.
 * <p>
 */
@ServerEndpoint(value = "/zeppelin/ws",
        configurator = ZeppelinEndpointConfig.class)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class NotebookServer implements
    SearchService,
    JobListenerFactory, AngularObjectRegistryListener,  RemoteInterpreterProcessListener{

  private static final Logger LOG = Logger.getLogger(NotebookServer.class.
          getName());

  Gson gson = new Gson();
  static final Map<String, List<Session>> noteSocketMap = new HashMap<>();
  static final Queue<Session> connectedSockets = new ConcurrentLinkedQueue<>();
  private String sender;
  private Project project;
  private String userRole;
  private String hdfsUsername;
  private Notebook notebook;
  private Session session;
  private ZeppelinConfig conf;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectFacade projectBean;
  @EJB
  private ZeppelinConfigFactory zeppelin;
  @EJB
  private HdfsUsersController hdfsUsersController;

  public NotebookServer() {
  }

  public Notebook notebook() {
    return this.notebook;
  }

  @OnOpen
  public void open(Session conn, EndpointConfig config) {
    LOG.log(Level.INFO, "Create zeppelin websocket on port {0}:{1}",
            new Object[]{conn.getRequestURI().getHost(), conn.getRequestURI().
              getPort()});
    this.session = conn;
    this.sender = (String) config.getUserProperties().get("user");
    this.project = getProject((String) config.getUserProperties().get(
            "projectID"));
    authenticateUser(conn, this.project, this.sender);
    if (this.userRole == null) {
      LOG.log(Level.INFO, "User not authorized for Zeepelin Access: {0}",
              this.sender);
      return;
    }
    this.conf = zeppelin.getZeppelinConfig(this.project.getName(),
            this.sender, this);
    this.notebook = this.conf.getNotebook();
    synchronized (connectedSockets) {
      connectedSockets.add(conn);
    }
    this.session.getUserProperties().put("projectID", this.project.getId());
  }

  @OnMessage
  public void onMessage(String msg, Session conn) {
    Notebook notebook = notebook();
    try {
      Message messagereceived = deserializeMessage(msg);
      LOG.log(Level.INFO, "RECEIVE << {0}", messagereceived.op);
      LOG.log(Level.INFO, "RECEIVE PRINCIPAL << {0}", messagereceived.principal);
      LOG.log(Level.INFO, "RECEIVE TICKET << {0}", messagereceived.ticket);
      LOG.log(Level.INFO, "RECEIVE ROLES << {0}", messagereceived.roles);

      /**
       * Lets be elegant here
       */
      switch (messagereceived.op) {
        case LIST_NOTES:
          unicastNoteList(conn);
          break;
        case RELOAD_NOTES_FROM_REPO:
          broadcastReloadedNoteList();
          break;
        case GET_HOME_NOTE:
          sendHomeNote(conn, notebook);
          break;
        case GET_NOTE:
          sendNote(conn, notebook, messagereceived);
          break;
        case NEW_NOTE:
          createNote(conn, notebook, messagereceived);
          break;
        case DEL_NOTE:
          removeNote(conn, notebook, messagereceived);
          break;
        case CLONE_NOTE:
          cloneNote(conn, notebook, messagereceived);
          break;
        case IMPORT_NOTE:
          importNote(conn, notebook, messagereceived);
          break;
        case COMMIT_PARAGRAPH:
          updateParagraph(conn, notebook, messagereceived);
          break;
        case RUN_PARAGRAPH:
          runParagraph(conn, notebook, messagereceived);
          break;
        case CANCEL_PARAGRAPH:
          cancelParagraph(conn, notebook, messagereceived);
          break;
        case MOVE_PARAGRAPH:
          moveParagraph(conn, notebook, messagereceived);
          break;
        case INSERT_PARAGRAPH:
          insertParagraph(conn, notebook, messagereceived);
          break;
        case PARAGRAPH_REMOVE:
          removeParagraph(conn, notebook, messagereceived);
          break;
        case PARAGRAPH_CLEAR_OUTPUT:
          clearParagraphOutput(conn, notebook, messagereceived);
          break;
        case NOTE_UPDATE:
          updateNote(conn, notebook, messagereceived);
          break;
        case COMPLETION:
          completion(conn, notebook, messagereceived);
          break;
        case PING:
          break; //do nothing
        case ANGULAR_OBJECT_UPDATED:
          angularObjectUpdated(conn, notebook, messagereceived);
          break;
        case ANGULAR_OBJECT_CLIENT_BIND:
          angularObjectClientBind(conn, notebook, messagereceived);
          break;
        case ANGULAR_OBJECT_CLIENT_UNBIND:
          angularObjectClientUnbind(conn, notebook, messagereceived);
          break;
        case LIST_CONFIGURATIONS:
          sendAllConfigurations(conn, notebook);
          break;
        case CHECKPOINT_NOTEBOOK:
          checkpointNotebook(conn, notebook, messagereceived);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Can't handle message", e);
    }
  }

  @OnClose
  public void onClose(Session conn, CloseReason reason) {
    LOG.log(Level.INFO, "Closed connection to {0} : {1}. Reason: {2}",
            new Object[]{
              conn.getRequestURI().getHost(),
              conn.getRequestURI().getPort(),
              reason});
    removeConnectionFromAllNote(conn);
    connectedSockets.remove(conn);
  }

  @OnError
  public void onError(Session conn, Throwable exc) {
    removeConnectionFromAllNote(conn);
    connectedSockets.remove(conn);
  }

  private Message deserializeMessage(String msg) {
    return gson.fromJson(msg, Message.class);
  }

  private String serializeMessage(Message m) {
    return gson.toJson(m);
  }

  private void addConnectionToNote(String noteId, Session socket) {
    synchronized (noteSocketMap) {
      removeConnectionFromAllNote(socket); // make sure a socket relates only a single note.
      List<Session> socketList = noteSocketMap.get(noteId);
      if (socketList == null) {
        socketList = new LinkedList<>();
        noteSocketMap.put(noteId, socketList);
      }
      if (!socketList.contains(socket)) {
        socketList.add(socket);
      }
    }
  }

  private void removeConnectionFromNote(String noteId, Session socket) {
    synchronized (noteSocketMap) {
      List<Session> socketList = noteSocketMap.get(noteId);
      if (socketList != null) {
        socketList.remove(socket);
      }
    }
  }

  private void removeNote(String noteId) {
    synchronized (noteSocketMap) {
      noteSocketMap.remove(noteId);
    }
  }

  private void removeConnectionFromAllNote(Session socket) {
    synchronized (noteSocketMap) {
      Set<String> keys = noteSocketMap.keySet();
      for (String noteId : keys) {
        removeConnectionFromNote(noteId, socket);
      }
    }
  }

  private String getOpenNoteId(Session socket) {
    String id = null;
    synchronized (noteSocketMap) {
      Set<String> keys = noteSocketMap.keySet();
      for (String noteId : keys) {
        List<Session> sockets = noteSocketMap.get(noteId);
        if (sockets.contains(socket)) {
          id = noteId;
        }
      }
    }

    return id;
  }

  private void broadcastToNoteBindedInterpreter(String interpreterGroupId,
          Message m) {
    Notebook notebook = notebook();
    List<Note> notes = notebook.getAllNotes();
    for (Note note : notes) {
      List<String> ids = note.getNoteReplLoader().getInterpreters();
      for (String id : ids) {
        if (id.equals(interpreterGroupId)) {
          broadcast(note.id(), m);
        }
      }
    }
  }

  //broadcasts to sockets in the noteId list
  private void broadcast(String noteId, Message m) {
    synchronized (noteSocketMap) {
      List<Session> socketLists = noteSocketMap.get(noteId);
      if (socketLists == null || socketLists.isEmpty()) {
        return;
      }
      LOG.log(Level.INFO, "SEND >> {0}", m.op);
      for (Session conn : socketLists) {
        try {
          conn.getBasicRemote().sendText(serializeMessage(m));
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, "Unable to send message " + m, ex);
        }
      }
    }
  }

  private void broadcastExcept(String noteId, Message m, Session exclude) {
    synchronized (noteSocketMap) {
      List<Session> socketLists = noteSocketMap.get(noteId);
      if (socketLists == null || socketLists.isEmpty()) {
        return;
      }
      LOG.log(Level.INFO, "SEND >> {0}", m.op);
      for (Session conn : socketLists) {
        if (exclude.equals(conn)) {
          continue;
        }
        try {
          conn.getBasicRemote().sendText(serializeMessage(m));
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, "Unable to send message " + m, ex);
        }
      }
    }
  }

  //broadcast to every one in the same project as this.project.id
  private void broadcastAll(Message m) {
    for (Session conn : connectedSockets) {
      try {
        if (conn.getUserProperties().get("projectID").equals(
                this.project.getId())) {
          conn.getBasicRemote().sendText(serializeMessage(m));
        }
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Unable to send message " + m, ex);
      }
    }
  }

  private void unicast(Message m, Session conn) {
    try {
      conn.getBasicRemote().sendText(serializeMessage(m));
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "socket error", e);
    }
  }

  public List<Map<String, String>> generateNotebooksInfo(boolean needsReload) {
    Notebook notebook = notebook();

    ZeppelinConfiguration conf = notebook.getConf();
    String homescreenNotebookId = conf.getString(
            ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN);
    boolean hideHomeScreenNotebookFromList = conf
            .getBoolean(ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN_HIDE);

    if (needsReload) {
      try {
        notebook.reloadAllNotes();
      } catch (IOException e) {
        LOG.severe("Fail to reload notes from repository");
      }
    }

    List<Note> notes = notebook.getAllNotes();
    List<Map<String, String>> notesInfo = new LinkedList<>();
    for (Note note : notes) {
      Map<String, String> info = new HashMap<>();

      if (hideHomeScreenNotebookFromList && note.id().equals(
              homescreenNotebookId)) {
        continue;
      }

      info.put("id", note.id());
      info.put("name", note.getName());
      notesInfo.add(info);
    }

    return notesInfo;
  }

  public void broadcastNote(Note note) {
    broadcast(note.id(), new Message(OP.NOTE).put("note", note));
  }

  public void broadcastNoteList() {
    List<Map<String, String>> notesInfo = generateNotebooksInfo(false);
    broadcastAll(new Message(OP.NOTES_INFO).put("notes", notesInfo));
  }

  public void unicastNoteList(Session conn) {
    List<Map<String, String>> notesInfo = generateNotebooksInfo(false);
    unicast(new Message(OP.NOTES_INFO).put("notes", notesInfo), conn);
  }

  public void broadcastReloadedNoteList() {
    List<Map<String, String>> notesInfo = generateNotebooksInfo(true);
    broadcastAll(new Message(OP.NOTES_INFO).put("notes", notesInfo));
  }

  void permissionError(Session conn, String op, String userAndRoles,
          String allowed) throws IOException {
    LOG.log(Level.INFO,
            "Cannot {0}. Connection readers {1}. Allowed readers{2}",
            new Object[]{op,
              userAndRoles, allowed});
    Users user = userBean.findByEmail(this.sender);
    conn.getBasicRemote().sendText(serializeMessage(new Message(OP.AUTH_INFO).
            put("info",
                    "Insufficient privileges to " + op + " notebook.\n\n"
                    + "Allowed users or roles: " + allowed + "\n\n"
                    + "But the user " + user.getFname() + " " + user.getLname()
                    + " belongs to: " + userAndRoles)));
  }

  private void sendNote(Session conn, Notebook notebook,
          Message fromMessage) throws IOException {
    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null) {
      if (this.userRole == null) {
        permissionError(conn, "read", this.userRole, AllowedRoles.DATA_OWNER
                + ", " + AllowedRoles.DATA_SCIENTIST);
        return;
      }
      addConnectionToNote(note.id(), conn);
      conn.getBasicRemote().sendText(serializeMessage(new Message(OP.NOTE).put(
              "note", note)));
      sendAllAngularObjects(note, conn);
    }
  }

  private void sendHomeNote(Session conn, Notebook notebook) throws IOException {
    String noteId = notebook.getConf().getString(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN);

    Note note = null;
    if (noteId != null) {
      note = notebook.getNote(noteId);
    }

    if (note != null) {
      if (this.userRole == null) {
        permissionError(conn, "read", this.userRole, AllowedRoles.DATA_OWNER
                + ", " + AllowedRoles.DATA_SCIENTIST);
        return;
      }
      addConnectionToNote(note.id(), conn);
      conn.getBasicRemote().sendText(serializeMessage(new Message(OP.NOTE).put(
              "note", note)));
      sendAllAngularObjects(note, conn);
    } else {
      removeConnectionFromAllNote(conn);
      conn.getBasicRemote().sendText(serializeMessage(new Message(OP.NOTE).put(
              "note", null)));
    }
  }

  private void updateNote(Session conn, Notebook notebook, Message fromMessage)
          throws SchedulerException, IOException {
    String noteId = (String) fromMessage.get("id");
    String name = (String) fromMessage.get("name");
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");
    if (noteId == null) {
      return;
    }
    if (config == null) {
      return;
    }

    if (this.userRole == null) {
      permissionError(conn, "update", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null) {
      boolean cronUpdated = isCronUpdated(config, note.getConfig());
      note.setName(name);
      note.setConfig(config);
      if (cronUpdated) {
        notebook.refreshCron(note.id());
      }

      note.persist();
      broadcastNote(note);
      broadcastNoteList();
    }
  }

  private boolean isCronUpdated(Map<String, Object> configA,
          Map<String, Object> configB) {
    boolean cronUpdated = false;
    if (configA.get("cron") != null && configB.get("cron") != null
            && configA.get("cron").equals(configB.get("cron"))) {
      cronUpdated = true;
    } else if (configA.get("cron") == null && configB.get("cron") == null) {
      cronUpdated = false;
    } else if (configA.get("cron") != null || configB.get("cron") != null) {
      cronUpdated = true;
    }

    return cronUpdated;
  }

  private void createNote(Session conn, Notebook notebook, Message message)
          throws IOException {
    Note note = notebook.createNote();
    note.addParagraph(); // it's an empty note. so add one paragraph
    if (message != null) {
      String noteName = (String) message.get("name");
      if (noteName == null || noteName.isEmpty()) {
        noteName = "Note " + note.getId();
      }
      note.setName(noteName);
    }

    note.persist();
    addConnectionToNote(note.id(), conn);
    conn.getBasicRemote().sendText(serializeMessage(new Message(OP.NEW_NOTE).
            put("note", note)));
    broadcastNoteList();
  }

  private void removeNote(Session conn, Notebook notebook, Message fromMessage)
          throws IOException {
    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (!this.userRole.equals(AllowedRoles.DATA_OWNER)) {
      permissionError(conn, "remove", this.userRole, AllowedRoles.DATA_OWNER);
      return;
    }

    notebook.removeNote(noteId);
    removeNote(noteId);
    broadcastNoteList();
  }

  private void updateParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    Map<String, Object> params = (Map<String, Object>) fromMessage
            .get("params");
    Map<String, Object> config = (Map<String, Object>) fromMessage
            .get("config");
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);

    if (this.userRole == null) {
      permissionError(conn, "write", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    Paragraph p = note.getParagraph(paragraphId);
    p.settings.setParams(params);
    p.setConfig(config);
    p.setTitle((String) fromMessage.get("title"));
    p.setText((String) fromMessage.get("paragraph"));
    note.persist();
    broadcast(note.id(), new Message(OP.PARAGRAPH).put("paragraph", p));
  }

  private void cloneNote(Session conn, Notebook notebook, Message fromMessage)
          throws IOException, CloneNotSupportedException {
    String noteId = getOpenNoteId(conn);
    String name = (String) fromMessage.get("name");
    Note newNote = notebook.cloneNote(noteId, name);
    addConnectionToNote(newNote.id(), conn);
    conn.getBasicRemote().sendText(serializeMessage(new Message(OP.NEW_NOTE).
            put("note", newNote)));
    broadcastNoteList();
  }

  protected Note importNote(Session conn, Notebook notebook, Message fromMessage)
          throws IOException {
    Note note = null;
    if (fromMessage != null) {
      String noteName = (String) ((Map) fromMessage.get("notebook")).get("name");
      String noteJson = gson.toJson(fromMessage.get("notebook"));
      note = notebook.importNote(noteJson, noteName);
      note.persist();
      broadcastNote(note);
      broadcastNoteList();
    }
    return note;
  }

  private void removeParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {

    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);

    if (!this.userRole.equals(AllowedRoles.DATA_OWNER)) {
      permissionError(conn, "remove", this.userRole, AllowedRoles.DATA_OWNER);
      return;
    }

    /**
     * We dont want to remove the last paragraph
     */
    if (!note.isLastParagraph(paragraphId)) {
      note.removeParagraph(paragraphId);
      note.persist();
      broadcastNote(note);
    }
  }

  private void clearParagraphOutput(Session conn, Notebook notebook,
          Message fromMessage) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);
    if (this.userRole == null) {
      permissionError(conn, "write", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    note.clearParagraphOutput(paragraphId);
    broadcastNote(note);
  }

  private void completion(Session conn, Notebook notebook, Message fromMessage)
          throws IOException {
    String paragraphId = (String) fromMessage.get("id");
    String buffer = (String) fromMessage.get("buf");
    int cursor = (int) Double.parseDouble(fromMessage.get("cursor").toString());
    Message resp = new Message(OP.COMPLETION_LIST).put("id", paragraphId);
    if (paragraphId == null) {
      conn.getBasicRemote().sendText(serializeMessage(resp));
      return;
    }

    final Note note = notebook.getNote(getOpenNoteId(conn));
    List<String> candidates = note.completion(paragraphId, buffer, cursor);
    resp.put("completions", candidates);
    conn.getBasicRemote().sendText(serializeMessage(resp));
  }

  /**
   * When angular object updated from client
   *
   * @param conn the web socket.
   * @param notebook the notebook.
   * @param fromMessage the message.
   */
  private void angularObjectUpdated(Session conn, Notebook notebook,
          Message fromMessage) {
    String noteId = (String) fromMessage.get("noteId");
    String paragraphId = (String) fromMessage.get("paragraphId");
    String interpreterGroupId = (String) fromMessage.get("interpreterGroupId");
    String varName = (String) fromMessage.get("name");
    Object varValue = fromMessage.get("value");
    AngularObject ao = null;
    boolean global = false;
    // propagate change to (Remote) AngularObjectRegistry
    Note note = notebook.getNote(noteId);
    if (note != null) {
      List<InterpreterSetting> settings = note.getNoteReplLoader()
              .getInterpreterSettings();
      for (InterpreterSetting setting : settings) {
        if (setting.getInterpreterGroup(note.id()) == null) {
          continue;
        }
        if (interpreterGroupId.equals(setting.getInterpreterGroup(note.id()).
                getId())) {
          AngularObjectRegistry angularObjectRegistry = setting
                  .getInterpreterGroup(note.id()).getAngularObjectRegistry();
          // first trying to get local registry
          ao = angularObjectRegistry.get(varName, noteId, paragraphId);
          if (ao == null) {
            // then try notebook scope registry
            ao = angularObjectRegistry.get(varName, noteId, null);
            if (ao == null) {
              // then try global scope registry
              ao = angularObjectRegistry.get(varName, null, null);
              if (ao == null) {
                LOG.log(Level.WARNING, "Object {0} is not binded", varName);
              } else {
                // path from client -> server
                ao.set(varValue, false);
                global = true;
              }
            } else {
              // path from client -> server
              ao.set(varValue, false);
              global = false;
            }
          } else {
            ao.set(varValue, false);
            global = false;
          }
          break;
        }
      }
    }

    if (global) { // broadcast change to all web session that uses related
      // interpreter.
      for (Note n : notebook.getAllNotes()) {
        List<InterpreterSetting> settings = note.getNoteReplLoader()
                .getInterpreterSettings();
        for (InterpreterSetting setting : settings) {
          if (setting.getInterpreterGroup(n.id()) == null) {
            continue;
          }
          if (interpreterGroupId.equals(setting.getInterpreterGroup(n.id()).
                  getId())) {
            AngularObjectRegistry angularObjectRegistry = setting
                    .getInterpreterGroup(n.id()).getAngularObjectRegistry();
            this.broadcastExcept(
                    n.id(),
                    new Message(OP.ANGULAR_OBJECT_UPDATE).put("angularObject",
                            ao)
                    .put("interpreterGroupId", interpreterGroupId)
                    .put("noteId", n.id())
                    .put("paragraphId", ao.getParagraphId()),
                    conn);
          }
        }
      }
    } else { // broadcast to all web session for the note
      this.broadcastExcept(
              note.id(),
              new Message(OP.ANGULAR_OBJECT_UPDATE).put("angularObject", ao)
              .put("interpreterGroupId", interpreterGroupId)
              .put("noteId", note.id())
              .put("paragraphId", ao.getParagraphId()),
              conn);
    }
  }

  /**
   * Push the given Angular variable to the target
   * interpreter angular registry given a noteId
   * and a paragraph id
   *
   * @param conn
   * @param notebook
   * @param fromMessage
   * @throws Exception
   */
  protected void angularObjectClientBind(Session conn,
          Notebook notebook, Message fromMessage)
          throws Exception {
    String noteId = fromMessage.getType("noteId");
    String varName = fromMessage.getType("name");
    Object varValue = fromMessage.get("value");
    String paragraphId = fromMessage.getType("paragraphId");
    Note note = notebook.getNote(noteId);

    if (paragraphId == null) {
      throw new IllegalArgumentException("target paragraph not specified for "
              + "angular value bind");
    }

    if (note != null) {
      final InterpreterGroup interpreterGroup
              = findInterpreterGroupForParagraph(note,
                      paragraphId);

      final AngularObjectRegistry registry = interpreterGroup.
              getAngularObjectRegistry();
      if (registry instanceof RemoteAngularObjectRegistry) {

        RemoteAngularObjectRegistry remoteRegistry
                = (RemoteAngularObjectRegistry) registry;
        pushAngularObjectToRemoteRegistry(noteId, paragraphId, varName, varValue,
                remoteRegistry,
                interpreterGroup.getId(), conn);

      } else {
        pushAngularObjectToLocalRepo(noteId, paragraphId, varName, varValue,
                registry,
                interpreterGroup.getId(), conn);
      }
    }
  }

  /**
   * Remove the given Angular variable to the target
   * interpreter(s) angular registry given a noteId
   * and an optional list of paragraph id(s)
   *
   * @param conn
   * @param notebook
   * @param fromMessage
   * @throws Exception
   */
  protected void angularObjectClientUnbind(Session conn,
          Notebook notebook, Message fromMessage)
          throws Exception {
    String noteId = fromMessage.getType("noteId");
    String varName = fromMessage.getType("name");
    String paragraphId = fromMessage.getType("paragraphId");
    Note note = notebook.getNote(noteId);

    if (paragraphId == null) {
      throw new IllegalArgumentException("target paragraph not specified for "
              + "angular value unBind");
    }

    if (note != null) {
      final InterpreterGroup interpreterGroup
              = findInterpreterGroupForParagraph(note,
                      paragraphId);

      final AngularObjectRegistry registry = interpreterGroup.
              getAngularObjectRegistry();

      if (registry instanceof RemoteAngularObjectRegistry) {
        RemoteAngularObjectRegistry remoteRegistry
                = (RemoteAngularObjectRegistry) registry;
        removeAngularFromRemoteRegistry(noteId, paragraphId, varName,
                remoteRegistry,
                interpreterGroup.getId(), conn);
      } else {
        removeAngularObjectFromLocalRepo(noteId, paragraphId, varName, registry,
                interpreterGroup.getId(), conn);
      }
    }
  }

  private InterpreterGroup findInterpreterGroupForParagraph(Note note,
          String paragraphId)
          throws Exception {
    final Paragraph paragraph = note.getParagraph(paragraphId);
    if (paragraph == null) {
      throw new IllegalArgumentException("Unknown paragraph with id : "
              + paragraphId);
    }
    return paragraph.getCurrentRepl().getInterpreterGroup();
  }

  private void pushAngularObjectToRemoteRegistry(String noteId,
          String paragraphId,
          String varName, Object varValue,
          RemoteAngularObjectRegistry remoteRegistry,
          String interpreterGroupId, Session conn) {

    final AngularObject ao = remoteRegistry.addAndNotifyRemoteProcess(varName,
            varValue,
            noteId, paragraphId);

    this.broadcastExcept(
            noteId,
            new Message(OP.ANGULAR_OBJECT_UPDATE).put("angularObject", ao)
            .put("interpreterGroupId", interpreterGroupId)
            .put("noteId", noteId)
            .put("paragraphId", paragraphId),
            conn);
  }

  private void removeAngularFromRemoteRegistry(String noteId, String paragraphId,
          String varName, RemoteAngularObjectRegistry remoteRegistry,
          String interpreterGroupId, Session conn) {
    final AngularObject ao = remoteRegistry.
            removeAndNotifyRemoteProcess(varName, noteId,
                    paragraphId);
    this.broadcastExcept(
            noteId,
            new Message(OP.ANGULAR_OBJECT_REMOVE).put("angularObject", ao)
            .put("interpreterGroupId", interpreterGroupId)
            .put("noteId", noteId)
            .put("paragraphId", paragraphId),
            conn);
  }

  private void pushAngularObjectToLocalRepo(String noteId, String paragraphId,
          String varName,
          Object varValue, AngularObjectRegistry registry,
          String interpreterGroupId, Session conn) {
    AngularObject angularObject = registry.get(varName, noteId, paragraphId);
    if (angularObject == null) {
      angularObject = registry.add(varName, varValue, noteId, paragraphId);
    } else {
      angularObject.set(varValue, true);
    }

    this.broadcastExcept(
            noteId,
            new Message(OP.ANGULAR_OBJECT_UPDATE).put("angularObject",
                    angularObject)
            .put("interpreterGroupId", interpreterGroupId)
            .put("noteId", noteId)
            .put("paragraphId", paragraphId),
            conn);
  }

  private void removeAngularObjectFromLocalRepo(String noteId,
          String paragraphId, String varName,
          AngularObjectRegistry registry, String interpreterGroupId,
          Session conn) {
    final AngularObject removed = registry.remove(varName, noteId, paragraphId);
    if (removed != null) {
      this.broadcastExcept(
              noteId,
              new Message(OP.ANGULAR_OBJECT_REMOVE).
              put("angularObject", removed)
              .put("interpreterGroupId", interpreterGroupId)
              .put("noteId", noteId)
              .put("paragraphId", paragraphId),
              conn);
    }
  }

  private void moveParagraph(Session conn, Notebook notebook,
          Message fromMessage) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    final int newIndex = (int) Double.parseDouble(fromMessage.get("index")
            .toString());
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);
    if (this.userRole == null) {
      permissionError(conn, "write", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    note.moveParagraph(paragraphId, newIndex);
    note.persist();
    broadcastNote(note);
  }

  private void insertParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    final int index = (int) Double.parseDouble(fromMessage.get("index").
            toString());
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);
    if (this.userRole == null) {
      permissionError(conn, "write", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    note.insertParagraph(index);
    note.persist();
    broadcastNote(note);
  }

  private void cancelParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);
    if (this.userRole == null) {
      permissionError(conn, "write", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    Paragraph p = note.getParagraph(paragraphId);
    p.abort();
  }

  private void runParagraph(Session conn, Notebook notebook,
          Message fromMessage) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);
    if (this.userRole == null) {
      permissionError(conn, "write", this.userRole, AllowedRoles.DATA_OWNER
              + ", " + AllowedRoles.DATA_SCIENTIST);
      return;
    }

    Paragraph p = note.getParagraph(paragraphId);
    String text = (String) fromMessage.get("paragraph");
    p.setText(text);
    p.setTitle((String) fromMessage.get("title"));

    // This AuthenticationInfo object is used for secure impersonation by the Livy Interpreter to
    // execute the Spark job as the hdfsUserName. It sets a property called "ProxyUser" in Livy
    // Right now, an empty password is ok as a parameter
    AuthenticationInfo authenticationInfo = new AuthenticationInfo(
            this.hdfsUsername, "");
    p.setAuthenticationInfo(authenticationInfo);

    Map<String, Object> params = (Map<String, Object>) fromMessage.get("params");
    p.settings.setParams(params);
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");
    p.setConfig(config);
    // if it's the last paragraph, let's add a new one
    boolean isTheLastParagraph = note.getLastParagraph().getId().equals(p.
            getId());
    if (!Strings.isNullOrEmpty(text) && isTheLastParagraph) {
      note.addParagraph();
    }

    note.persist();
    try {
      note.run(paragraphId);
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Exception from run", ex);
      if (p != null) {
        p.setReturn(new InterpreterResult(
                InterpreterResult.Code.ERROR, ex.getMessage()), ex);
        p.setStatus(Status.ERROR);
        broadcast(note.id(), new Message(OP.PARAGRAPH).put("paragraph", p));
      }
    }
  }


  private void sendAllConfigurations(Session conn,
          Notebook notebook) throws IOException {
    ZeppelinConfiguration conf = notebook.getConf();
    Map<String, String> configurations = conf.dumpConfigurations(conf,
            new ZeppelinConfiguration.ConfigurationKeyPredicate() {
      @Override
      public boolean apply(String key) {
        return !key.contains("password") && !key.equals(
                ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_AZURE_CONNECTION_STRING.
                getVarName());
      }
    });

    conn.getBasicRemote().sendText(serializeMessage(new Message(
            OP.CONFIGURATIONS_INFO)
            .put("configurations", configurations)));
  }

  private void checkpointNotebook(Session conn, Notebook notebook,
          Message fromMessage) throws IOException {
    String noteId = (String) fromMessage.get("noteId");
    String commitMessage = (String) fromMessage.get("commitMessage");
    notebook.checkpointNote(noteId, commitMessage);
  }
  
  @Override
  public List<Map<String, String>> query(String string) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void updateIndexDoc(Note note) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void addIndexDocs(Collection<Note> clctn) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void addIndexDoc(Note note) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteIndexDocs(Note note) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteIndexDoc(Note note, Paragraph prgrph) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * This callback is for the paragraph that runs on ZeppelinServer
   *
   * @param noteId
   * @param paragraphId
   * @param output output to append
   */
  @Override
  public void onOutputAppend(String noteId, String paragraphId, String output) {
    Message msg = new Message(OP.PARAGRAPH_APPEND_OUTPUT)
            .put("noteId", noteId)
            .put("paragraphId", paragraphId)
            .put("data", output);
    Paragraph paragraph = notebook().getNote(noteId).getParagraph(paragraphId);
    broadcast(noteId, msg);
  }

  /**
   * This callback is for the paragraph that runs on ZeppelinServer
   *
   * @param noteId
   * @param paragraphId
   * @param output output to update (replace)
   */
  @Override
  public void onOutputUpdated(String noteId, String paragraphId, String output) {
    Message msg = new Message(OP.PARAGRAPH_UPDATE_OUTPUT)
            .put("noteId", noteId)
            .put("paragraphId", paragraphId)
            .put("data", output);
    Paragraph paragraph = notebook().getNote(noteId).getParagraph(paragraphId);
    broadcast(noteId, msg);
  }

  /**
   * Need description here.
   * <p>
   */
  public static class ParagraphListenerImpl implements ParagraphJobListener {

    private NotebookServer notebookServer;
    private Note note;

    public ParagraphListenerImpl(NotebookServer notebookServer, Note note) {
      this.notebookServer = notebookServer;
      this.note = note;
    }

    @Override
    public void onProgressUpdate(Job job, int progress) {
      notebookServer.broadcast(
              note.id(),
              new Message(OP.PROGRESS).put("id", job.getId()).put("progress",
              job.progress()));
    }

    @Override
    public void beforeStatusChange(Job job, Status before, Status after) {
    }

    @Override
    public void afterStatusChange(Job job, Status before, Status after) {
      if (after == Status.ERROR) {
        if (job.getException() != null) {
          LOG.log(Level.INFO, "Error", job.getException());
        }
      }

      if (job.isTerminated()) {
        LOG.log(Level.INFO, "Job {0} is finished", job.getId());
        try {
          note.persist();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      notebookServer.broadcastNote(note);
    }

    /**
     * This callback is for praragraph that runs on RemoteInterpreterProcess
     *
     * @param paragraph
     * @param out
     * @param output
     */
    @Override
    public void onOutputAppend(Paragraph paragraph, InterpreterOutput out,
            String output) {
      Message msg = new Message(OP.PARAGRAPH_APPEND_OUTPUT)
              .put("noteId", paragraph.getNote().getId())
              .put("paragraphId", paragraph.getId())
              .put("data", output);

      notebookServer.broadcast(paragraph.getNote().getId(), msg);
    }

    /**
     * This callback is for paragraph that runs on RemoteInterpreterProcess
     *
     * @param paragraph
     * @param out
     * @param output
     */
    @Override
    public void onOutputUpdate(Paragraph paragraph, InterpreterOutput out,
            String output) {
      Message msg = new Message(OP.PARAGRAPH_UPDATE_OUTPUT)
              .put("noteId", paragraph.getNote().getId())
              .put("paragraphId", paragraph.getId())
              .put("data", output);

      notebookServer.broadcast(paragraph.getNote().getId(), msg);
    }

  }

  @Override
  public ParagraphJobListener getParagraphJobListener(Note note) {
    return new ParagraphListenerImpl(this, note);
  }

  private void sendAllAngularObjects(Note note, Session conn) throws IOException {
    List<InterpreterSetting> settings = note.getNoteReplLoader().
            getInterpreterSettings();
    if (settings == null || settings.isEmpty()) {
      return;
    }

    for (InterpreterSetting intpSetting : settings) {
      AngularObjectRegistry registry = intpSetting.
              getInterpreterGroup(note.id())
              .getAngularObjectRegistry();
      List<AngularObject> objects = registry.getAllWithGlobal(note.id());
      for (AngularObject object : objects) {
        conn.getBasicRemote().sendText(serializeMessage(new Message(
                OP.ANGULAR_OBJECT_UPDATE)
                .put("angularObject", object)
                .put("interpreterGroupId",
                        intpSetting.getInterpreterGroup(note.id()).getId())
                .put("noteId", note.id())
                .put("paragraphId", object.getParagraphId())
        ));
      }
    }
  }

  @Override
  public void onAdd(String interpreterGroupId, AngularObject object) {
    onUpdate(interpreterGroupId, object);
  }

  @Override
  public void onUpdate(String interpreterGroupId, AngularObject object) {
    Notebook notebook = notebook();
    if (notebook == null) {
      return;
    }

    List<Note> notes = notebook.getAllNotes();
    for (Note note : notes) {
      if (object.getNoteId() != null && !note.id().equals(object.getNoteId())) {
        continue;
      }

      List<InterpreterSetting> intpSettings = note.getNoteReplLoader()
              .getInterpreterSettings();
      if (intpSettings.isEmpty()) {
        continue;
      }
      for (InterpreterSetting setting : intpSettings) {
        if (setting.getInterpreterGroup(note.id()).getId().equals(
                interpreterGroupId)) {
          broadcast(
                  note.id(),
                  new Message(OP.ANGULAR_OBJECT_UPDATE)
                  .put("angularObject", object)
                  .put("interpreterGroupId", interpreterGroupId)
                  .put("noteId", note.id()));
        }
      }
    }
  }

  @Override
  public void onRemove(String interpreterGroupId, String name, String noteId,
          String paragraphId) {
    Notebook notebook = notebook();
    List<Note> notes = notebook.getAllNotes();
    for (Note note : notes) {
      if (noteId != null && !note.id().equals(noteId)) {
        continue;
      }

      List<String> ids = note.getNoteReplLoader().getInterpreters();
      for (String id : ids) {
        if (id.equals(interpreterGroupId)) {
          broadcast(
                  note.id(),
                  new Message(OP.ANGULAR_OBJECT_REMOVE).put("name", name).put(
                  "noteId", noteId));
        }
      }
    }
  }

  private void authenticateUser(Session session, Project project, String user) {
    //returns the user role in project. Null if the user has no role in project
    this.userRole = projectTeamBean.findCurrentRole(project, user);
    LOG.log(Level.SEVERE, "User role in this project {0}", this.userRole);
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

}