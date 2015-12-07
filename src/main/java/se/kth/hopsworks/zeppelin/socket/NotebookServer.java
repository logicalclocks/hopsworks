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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.apache.zeppelin.display.Input;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.JobListenerFactory;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.scheduler.Job.Status;
import org.apache.zeppelin.scheduler.JobListener;
import org.quartz.SchedulerException;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;
import se.kth.hopsworks.zeppelin.socket.Message.OP;

/**
 * Zeppelin websocket service.
 *
 */
@ServerEndpoint(value = "/websocket",
        configurator = ZeppelinEndpointConfig.class)
public class NotebookServer implements
        JobListenerFactory, AngularObjectRegistryListener {

  private static final Logger logger = Logger.getLogger(NotebookServer.class.
          getName());
  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;

  Gson gson = new Gson();
  private static final Map<String, List<Session>> noteSocketMap
          = new HashMap<>();
  private static final List<Session> connectedSockets = new LinkedList<>();
  private String sender;
  private Project project;
  private String userRole;
  private Notebook notebook;
  private Session session;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @EJB
  private ProjectFacade projectBean;

  public NotebookServer() {
  }

  public Notebook notebook() {
    return this.notebook;
  }

  @OnOpen
  public void open(Session conn, EndpointConfig config) {
    logger.log(Level.INFO, "Create zeppelin websocket on port {0}:{1}",
            new Object[]{conn.getRequestURI().getHost(), conn.getRequestURI().
              getPort()});
    this.session = conn;
    this.sender = (String) config.getUserProperties().get("user");
    this.project = getProject((String) config.getUserProperties().get(
            "projectID"));
    authenticateUser(conn, this.project, this.sender);
    if (this.userRole == null) {
      logger.log(Level.INFO, "User not authorized for Zeepelin Access: {0}",
              this.sender);
      return;
    }
    this.notebook = setupNotebook(this.project);
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
      logger.log(Level.INFO, "RECEIVE << {0}", messagereceived.op);
      /**
       * Lets be elegant here
       */
      switch (messagereceived.op) {
        case LIST_NOTES:
          sendNoteList(conn);
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
          pong();
          break;
        case ANGULAR_OBJECT_UPDATED:
          angularObjectUpdated(conn, notebook, messagereceived);
          break;
        default:
          sendNoteList(conn);
          break;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Can't handle message", e);
    }
  }

  @OnClose
  public void onClose(Session conn, CloseReason reason) {
    logger.log(Level.INFO, "Closed connection to {0} : {1}. Reason: {2}",
            new Object[]{
              conn.getRequestURI().getHost(),
              conn.getRequestURI().getPort(),
              reason});
    removeConnectionFromAllNote(conn);
    synchronized (connectedSockets) {
      connectedSockets.remove(conn);
    }
  }

  @OnError
  public void onError(Session conn, Throwable exc) {
    removeConnectionFromAllNote(conn);
    synchronized (connectedSockets) {
      connectedSockets.remove(conn);
    }
  }

  private Message deserializeMessage(String msg) {
    Message m = gson.fromJson(msg, Message.class);
    return m;
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

      if (socketList.contains(socket) == false) {
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

      logger.log(Level.INFO, "SEND >> {0}", m.op);

      for (Session conn : socketLists) {
        try {
          conn.getBasicRemote().sendText(serializeMessage(m));
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Unable to send message " + m, ex);
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
      logger.log(Level.INFO, "SEND >> {0}", m.op);
      for (Session conn : socketLists) {
        if (exclude.equals(conn)) {
          continue;
        }
        try {
          conn.getBasicRemote().sendText(serializeMessage(m));
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Unable to send message " + m, ex);
        }
      }
    }
  }

  //broadcast to every one in the same project as this.project.id
  private void broadcastAll(Message m) {
    synchronized (connectedSockets) {
      for (Session conn : connectedSockets) {
        try {
          if (conn.getUserProperties().get("projectID").equals(
                  this.project.getId())) {
            conn.getBasicRemote().sendText(serializeMessage(m));
          }
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Unable to send message " + m, ex);
        }
      }
    }
  }

  private void sendNoteList(Session conn) {
    Notebook notebook = notebook();
    List<Note> notes = notebook.getAllNotes();
    List<Map<String, String>> notesInfo = new LinkedList<>();
    for (Note note : notes) {
      Map<String, String> info = new HashMap<>();
      info.put("id", note.id());
      info.put("name", note.getName());
      notesInfo.add(info);
    }
    Message m = new Message(OP.NOTES_INFO).put("notes", notesInfo);
    try {
      conn.getBasicRemote().sendText(serializeMessage(m));
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Unable to send message " + m, ex);
    }
  }

  public List<Map<String, String>> generateNotebooksInfo() {
    Notebook notebook = notebook();

    ZeppelinConfiguration conf = notebook.getConf();
    String homescreenNotebookId = conf.getString(
            ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN);
    boolean hideHomeScreenNotebookFromList = conf
            .getBoolean(ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN_HIDE);

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

  private void sendHomeNote(Session conn, Notebook notebook) throws IOException {
    String noteId = notebook.getConf().getString(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN);

    Note note = null;
    if (noteId != null) {
      note = notebook.getNote(noteId);
    }

    if (note != null) {
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

  public void broadcastNoteList() {
    Notebook notebook = notebook();
    List<Note> notes = notebook.getAllNotes();//returns notes in project
    List<Map<String, String>> notesInfo = new LinkedList<>();
    for (Note note : notes) {
      Map<String, String> info = new HashMap<>();
      info.put("id", note.id());
      info.put("name", note.getName());
      notesInfo.add(info);
    }
    broadcastAll(new Message(OP.NOTES_INFO).put("notes", notesInfo));
  }

  private void sendNote(Session conn, Notebook notebook, Message fromMessage) {
    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }
    Note note = notebook.getNoteInProject(noteId);

    if (note != null) {
      addConnectionToNote(note.id(), conn);
      try {
        conn.getBasicRemote().sendText(serializeMessage(new Message(OP.NOTE).
                put("note", note)));
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Unable to send message " + new Message(
                Message.OP.NOTE).put("note",
                        note), ex);
      }
      sendAllAngularObjects(note, conn);
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
    Note note = notebook.getNoteInProject(noteId);
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
    broadcastNote(note);
    broadcastNoteList();
  }

  private void removeNote(Session conn, Notebook notebook, Message fromMessage)
          throws IOException {
    if (!this.userRole.equals(AllowedRoles.DATA_OWNER)) {
      return;
    }
    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }
    Note note = notebook.getNoteInProject(noteId);
    note.unpersist();
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
    Map<String, Object> params = (Map<String, Object>) fromMessage.get("params");
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");
    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
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
    broadcastNote(newNote);
    broadcastNoteList();
  }

  protected Note importNote(Session conn, Notebook notebook, Message fromMessage)
          throws IOException {

    Note note = notebook.createNote();
    if (fromMessage != null) {
      String noteName = (String) ((Map) fromMessage.get("notebook")).get("name");
      if (noteName == null || noteName.isEmpty()) {
        noteName = "Note " + note.getId();
      }
      note.setName(noteName);
      ArrayList<Map> paragraphs = ((Map<String, ArrayList>) fromMessage.get(
              "notebook"))
              .get("paragraphs");
      if (paragraphs.size() > 0) {
        for (Map paragraph : paragraphs) {
          try {
            Paragraph p = note.addParagraph();
            String text = (String) paragraph.get("text");
            p.setText(text);
            p.setTitle((String) paragraph.get("title"));
            Map<String, Object> params = (Map<String, Object>) ((Map) paragraph
                    .get("settings")).get("params");
            Map<String, Input> forms = (Map<String, Input>) ((Map) paragraph
                    .get("settings")).get("forms");
            if (params != null) {
              p.settings.setParams(params);
            }
            if (forms != null) {
              p.settings.setForms(forms);
            }
            Map<String, Object> result = (Map) paragraph.get("result");
            if (result != null) {
              InterpreterResult.Code code = InterpreterResult.Code
                      .valueOf((String) result.get("code"));
              InterpreterResult.Type type = InterpreterResult.Type
                      .valueOf((String) result.get("type"));
              String msg = (String) result.get("msg");
              p.setReturn(new InterpreterResult(code, type, msg), null);
            }

            Map<String, Object> config = (Map<String, Object>) paragraph
                    .get("config");
            p.setConfig(config);
          } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Exception while setting parameter in paragraph", e);
          }
        }
      }
    }

    note.persist();
    broadcastNote(note);
    broadcastNoteList();
    return note;
  }

  private void removeParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    if (!this.userRole.equals(AllowedRoles.DATA_OWNER)) {
      return;
    }
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }
    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
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
    final Note note = notebook.getNote(getOpenNoteId(conn));
    note.clearParagraphOutput(paragraphId);
    broadcastNote(note);
  }

  private void completion(Session conn, Notebook notebook, Message fromMessage) {
    String paragraphId = (String) fromMessage.get("id");
    String buffer = (String) fromMessage.get("buf");
    int cursor = (int) Double.parseDouble(fromMessage.get("cursor").toString());
    Message resp = new Message(OP.COMPLETION_LIST).put("id", paragraphId);
    if (paragraphId == null) {
      try {
        conn.getBasicRemote().sendText(serializeMessage(resp));
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Unable to send message " + resp, ex);
      }
      return;
    }

    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
    List<String> candidates = note.completion(paragraphId, buffer, cursor);
    resp.put("completions", candidates);
    try {
      conn.getBasicRemote().sendText(serializeMessage(resp));
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Unable to send message " + resp, ex);
    }
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
    String interpreterGroupId = (String) fromMessage.get("interpreterGroupId");
    String varName = (String) fromMessage.get("name");
    Object varValue = fromMessage.get("value");
    AngularObject ao = null;
    boolean global = false;
    // propagate change to (Remote) AngularObjectRegistry
    Note note = notebook.getNoteInProject(noteId);
    if (note != null) {
      List<InterpreterSetting> settings = note.getNoteReplLoader().
              getInterpreterSettings();
      for (InterpreterSetting setting : settings) {
        if (setting.getInterpreterGroup() == null) {
          continue;
        }
        if (interpreterGroupId.equals(setting.getInterpreterGroup().getId())) {
          AngularObjectRegistry angularObjectRegistry = setting
                  .getInterpreterGroup().getAngularObjectRegistry();
          // first trying to get local registry
          ao = angularObjectRegistry.get(varName, noteId);
          if (ao == null) {
            logger.log(Level.WARNING, "Object {} is not binded", varName);
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
        break;
      }
    }
    if (global) {
      // broadcast change to all web session that uses related
      // interpreter.
      for (Note n : notebook.getAllNotes()) {
        List<InterpreterSetting> settings = note.getNoteReplLoader().
                getInterpreterSettings();
        for (InterpreterSetting setting : settings) {
          if (setting.getInterpreterGroup() == null) {
            continue;
          }
          if (interpreterGroupId.equals(setting.getInterpreterGroup().getId())) {
            AngularObjectRegistry angularObjectRegistry = setting
                    .getInterpreterGroup().getAngularObjectRegistry();
            ao = angularObjectRegistry.get(varName, noteId);
            this.broadcastExcept(n.id(),
                    new Message(OP.ANGULAR_OBJECT_UPDATE)
                    .put("angularObject", ao)
                    .put("interpreterGroupId", interpreterGroupId)
                    .put("noteId", n.id()), conn);
          }
        }
      }
    } else { // broadcast to all web session for the note
      this.broadcastExcept(note.id(),
              new Message(OP.ANGULAR_OBJECT_UPDATE).put("angularObject", ao)
              .put("interpreterGroupId", interpreterGroupId)
              .put("noteId", note.id()),
              conn);
    }
  }

  private void moveParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    final int newIndex = (int) Double.parseDouble(fromMessage.get("index").
            toString());
    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
    note.moveParagraph(paragraphId, newIndex);
    note.persist();
    broadcastNote(note);
  }

  private void insertParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    final int index = (int) Double.parseDouble(fromMessage.get("index").
            toString());

    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
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

    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
    Paragraph p = note.getParagraph(paragraphId);
    p.abort();
  }

  private void runParagraph(Session conn, Notebook notebook,
          Message fromMessage)
          throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }
    final Note note = notebook.getNoteInProject(getOpenNoteId(conn));
    Paragraph p = note.getParagraph(paragraphId);
    String text = (String) fromMessage.get("paragraph");
    p.setText(text);
    p.setTitle((String) fromMessage.get("title"));
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
      logger.log(Level.SEVERE, "Exception from run", ex);
      if (p != null) {
        p.setReturn(new InterpreterResult(
                InterpreterResult.Code.ERROR, ex.getMessage()), ex);
        p.setStatus(Status.ERROR);
      }
    }
  }

  /**
   * Need description here.
   *
   */
  public static class ParagraphJobListener implements JobListener {

    private final NotebookServer notebookServer;
    private final Note note;

    public ParagraphJobListener(NotebookServer notebookServer, Note note) {
      this.notebookServer = notebookServer;
      this.note = note;
    }

    @Override
    public void onProgressUpdate(Job job, int progress) {
      notebookServer.broadcast(note.id(),
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
          logger.log(Level.INFO, "Error", job.getException());
        }
      }

      if (job.isTerminated()) {
        logger.log(Level.INFO, "Job {0} is finished", job.getId());
        try {
          note.persist();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      notebookServer.broadcastNote(note);
    }
  }

  @Override
  public JobListener getParagraphJobListener(Note note) {
    return new ParagraphJobListener(this, note);
  }

  private void pong() {
  }

  private void sendAllAngularObjects(Note note, Session conn) {
    List<InterpreterSetting> settings = note.getNoteReplLoader().
            getInterpreterSettings();
    if (settings == null || settings.isEmpty()) {
      return;
    }

    for (InterpreterSetting intpSetting : settings) {
      AngularObjectRegistry registry = intpSetting.getInterpreterGroup()
              .getAngularObjectRegistry();
      List<AngularObject> objects = registry.getAllWithGlobal(note.id());
      for (AngularObject object : objects) {
        try {
          conn.getBasicRemote().sendText(serializeMessage(new Message(
                  OP.ANGULAR_OBJECT_UPDATE)
                  .put("angularObject", object)
                  .put("interpreterGroupId", intpSetting.getInterpreterGroup().
                          getId())
                  .put("noteId", note.id())));
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Unable to send message " + new Message(
                  Message.OP.NOTE).put("note",
                          note), ex);
        }
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
        if (setting.getInterpreterGroup().getId().equals(interpreterGroupId)) {
          broadcast(note.id(), new Message(OP.ANGULAR_OBJECT_UPDATE)
                  .put("angularObject", object)
                  .put("interpreterGroupId", interpreterGroupId)
                  .put("noteId", note.id()));
        }
      }
    }
  }

  @Override
  public void onRemove(String interpreterGroupId, String name, String noteId) {
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

  private Notebook setupNotebook(Project project) {
    ZeppelinConfiguration conf = zeppelin.getConf();
    Class<?> notebookStorageClass;
    NotebookRepo notebookRepo;
    Notebook notebook = null;
    try {
      notebookStorageClass = Class.forName(conf.getString(
              ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE));
      Constructor<?> constructor = notebookStorageClass.getConstructor(
              ZeppelinConfiguration.class, Project.class
      );
      notebookRepo = (NotebookRepo) constructor.newInstance(conf, project);

      notebook = new Notebook(notebookRepo);
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Could not instantiate notebook", ex);
    }

    return notebook;
  }

  private void authenticateUser(Session session, Project project, String user) {
    //returns the user role in project. Null if the user has no role in project
    this.userRole = projectTeamBean.findCurrentRole(project, user);
    logger.log(Level.SEVERE, "User role in this projuct {0}", this.userRole);

    if (this.userRole == null) {
      try {
        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                "You do not have a role in this project."));
      } catch (IOException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
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
}
