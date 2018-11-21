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

package io.hops.hopsworks.api.zeppelin.socket;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.google.common.base.Strings;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.zeppelin.rest.exception.ForbiddenException;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.types.InterpreterSettingsList;
import io.hops.hopsworks.api.zeppelin.util.InterpreterBindingUtils;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.display.AngularObject;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.display.AngularObjectRegistryListener;
import org.apache.zeppelin.display.Input;
import org.apache.zeppelin.helium.ApplicationEventListener;
import org.apache.zeppelin.helium.HeliumPackage;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContextRunner;
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.interpreter.remote.RemoteAngularObjectRegistry;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterProcessListener;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.apache.zeppelin.notebook.Folder;
import org.apache.zeppelin.notebook.JobListenerFactory;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.NotebookAuthorization;
import org.apache.zeppelin.notebook.NotebookEventListener;
import org.apache.zeppelin.notebook.NotebookImportDeserializer;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.ParagraphJobListener;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.notebook.socket.WatcherMessage;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.apache.zeppelin.util.WatcherSecurityKey;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.SchedulerException;
import org.sonatype.aether.RepositoryException;

public class NotebookServerImpl implements
    JobListenerFactory, AngularObjectRegistryListener,
    RemoteInterpreterProcessListener, ApplicationEventListener {

  private static final Logger LOG = Logger.getLogger(NotebookServerImpl.class.getName());
  private static Gson gson = new GsonBuilder()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .registerTypeAdapter(Date.class, new NotebookImportDeserializer())
      .setPrettyPrinting()
      .registerTypeAdapterFactory(Input.TypeAdapterFactory).create();

  private final Map<String, List<Session>> noteSocketMap = new HashMap<>();
  private final Queue<Session> connectedSockets = new ConcurrentLinkedQueue<>();
  private final Map<String, Queue<Session>> userConnectedSockets = new ConcurrentHashMap<>();
  /**
   * This is a special endpoint in the notebook websoket, Every connection in this Queue
   * will be able to watch every websocket event, it doesnt need to be listed into the map of
   * noteSocketMap. This can be used to get information about websocket traffic and watch what
   * is going on.
   */
  private static final Queue<Session> watcherSockets = Queues.
      newConcurrentLinkedQueue();

  private ZeppelinConfig conf;
  private Notebook notebook;
  private Project project;
  private Settings settings;
  private CertsFacade certsFacade;
  private final ProjectTeamFacade projectTeamFacade;
  private final ActivityFacade activityFacade;
  private final CertificatesMgmService certificatesMgmService;

  private String certPwd;

  public NotebookServerImpl(Project project, ZeppelinConfigFactory zeppelin,
      CertsFacade certsFacade, Settings settings, ProjectTeamFacade projectTeamFacade,
      ActivityFacade activityFacade, CertificatesMgmService certificatesMgmService)
    throws IOException, RepositoryException,
    TaskRunnerException, InterruptedException {
    this.project = project;
    this.conf = zeppelin.getZeppelinConfig(project.getName(), this);
    this.notebook = this.conf.getNotebook();
    this.settings = settings;
    this.certsFacade = certsFacade;
    this.projectTeamFacade = projectTeamFacade;
    this.activityFacade = activityFacade;
    this.certificatesMgmService = certificatesMgmService;
  }

  public Notebook notebook() {
    return this.notebook;
  }

  public ZeppelinConfig getConf() {
    return conf;
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

  public void removeConnectionFromAllNote(Session socket) {
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

  private void broadcast(String noteId, Message m) {
    synchronized (noteSocketMap) {
      broadcastToWatchers(noteId, StringUtils.EMPTY, m);
      List<Session> socketLists = noteSocketMap.get(noteId);
      if (socketLists == null || socketLists.isEmpty()) {
        return;
      }
      LOG.log(Level.FINE, "SEND >> {0}", m.op);
      for (Session conn : socketLists) {
        try {
          sendMsg(conn, serializeMessage(m));
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, "Unable to send message " + m, ex);
        }
      }
    }
  }

  private void broadcastToNoteBindedInterpreter(String interpreterGroupId, Message m) {
    Notebook notebook = notebook();
    List<Note> notes = notebook.getAllNotes();
    for (Note note : notes) {
      List<String> ids = notebook.getInterpreterSettingManager().getInterpreters(note.getId());
      for (String id : ids) {
        if (id.equals(interpreterGroupId)) {
          broadcast(note.getId(), m);
        }
      }
    }
  }

  private void broadcastExcept(String noteId, Message m, Session exclude) {
    synchronized (noteSocketMap) {
      broadcastToWatchers(noteId, StringUtils.EMPTY, m);
      List<Session> socketLists = noteSocketMap.get(noteId);
      if (socketLists == null || socketLists.isEmpty()) {
        return;
      }
      LOG.log(Level.FINE, "SEND >> {0}", m.op);
      for (Session conn : socketLists) {
        if (exclude.equals(conn)) {
          continue;
        }
        try {
          sendMsg(conn, serializeMessage(m));
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, "Unable to send message " + m, ex);
        }
      }
    }
  }

  private void multicastToUser(String user, Message m) {
    if (!userConnectedSockets.containsKey(user)) {
      LOG.log(Level.SEVERE, "Multicasting to user {0} that is not in connections map", user);
      return;
    }

    for (Session conn : userConnectedSockets.get(user)) {
      unicast(m, conn);
    }
  }

  public void unicast(Message m, Session conn) {
    try {
      sendMsg(conn, serializeMessage(m));
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "socket error", e);
    }
    broadcastToWatchers(StringUtils.EMPTY, StringUtils.EMPTY, m);
  }

  public void unicastNoteJobInfo(Session conn, Message fromMessage) throws IOException {
    addConnectionToNote(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(), conn);
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    List<Map<String, Object>> noteJobs = notebook().getJobListByUnixTime(false, 0, subject);
    Map<String, Object> response = new HashMap<>();

    response.put("lastResponseUnixTime", System.currentTimeMillis());
    response.put("jobs", noteJobs);

    sendMsg(conn, serializeMessage(new Message(Message.OP.LIST_NOTE_JOBS).put("noteJobs", response)));
  }

  public void broadcastUpdateNoteJobInfo(long lastUpdateUnixTime) throws IOException {
    List<Map<String, Object>> noteJobs = new LinkedList<>();
    Notebook notebookObject = notebook();
    List<Map<String, Object>> jobNotes = null;
    if (notebookObject != null) {
      jobNotes = notebook().getJobListByUnixTime(false, lastUpdateUnixTime, null);
      noteJobs = jobNotes == null ? noteJobs : jobNotes;
    }

    Map<String, Object> response = new HashMap<>();
    response.put("lastResponseUnixTime", System.currentTimeMillis());
    response.put("jobs", noteJobs != null ? noteJobs : new LinkedList<>());

    broadcast(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(),
        new Message(Message.OP.LIST_UPDATE_NOTE_JOBS).put("noteRunningJobs", response));
  }

  public void unsubscribeNoteJobInfo(Session conn) {
    removeConnectionFromNote(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(), conn);
  }

  public void saveInterpreterBindings(Session conn, Message fromMessage, ZeppelinResource zeppelinResource) {
    String noteId = (String) fromMessage.data.get("noteId");
    try {
      List<String> settingIdList = gson.fromJson(String.valueOf(
          fromMessage.data.get("selectedSettingIds")),
          new TypeToken<ArrayList<String>>() {}.getType());
      AuthenticationInfo subject = new AuthenticationInfo(this.project.getName());
      notebook().bindInterpretersToNote(subject.getUser(), noteId, settingIdList);
      broadcastInterpreterBindings(noteId, InterpreterBindingUtils.getInterpreterBindings(notebook(), noteId));
      zeppelinResource.persistToDB(this.project);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error while saving interpreter bindings", e);
    }
  }

  public void getInterpreterBindings(Session conn, Message fromMessage) throws IOException {
    String noteId = (String) fromMessage.data.get("noteId");
    List<InterpreterSettingsList> settingList = InterpreterBindingUtils.
        getInterpreterBindings(notebook(), noteId);
    sendMsg(conn, serializeMessage(new Message(Message.OP.INTERPRETER_BINDINGS).
        put("interpreterBindings", settingList)));
  }

  public List<Map<String, String>> generateNotesInfo(boolean needsReload,
      AuthenticationInfo subject, Set<String> userAndRoles) {

    Notebook notebook = notebook();

    ZeppelinConfiguration conf = notebook.getConf();
    String homescreenNoteId = conf.getString(ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN);
    boolean hideHomeScreenNotebookFromList = conf.getBoolean(
        ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN_HIDE);

    if (needsReload) {
      try {
        notebook.reloadAllNotes(subject);
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Fail to reload notes from repository");
      }
    }

    List<Note> notes = notebook.getAllNotes(userAndRoles);
    List<Map<String, String>> notesInfo = new LinkedList<>();
    for (Note note : notes) {
      Map<String, String> info = new HashMap<>();

      if (hideHomeScreenNotebookFromList && note.getId().equals(homescreenNoteId)) {
        continue;
      }

      info.put("id", note.getId());
      info.put("name", note.getName());
      notesInfo.add(info);
    }

    return notesInfo;
  }

  public void broadcastNote(Note note) {
    broadcast(note.getId(), new Message(Message.OP.NOTE).put("note", note));
  }

  public void broadcastInterpreterBindings(String noteId, List settingList) {
    broadcast(noteId, new Message(Message.OP.INTERPRETER_BINDINGS).put("interpreterBindings", settingList));
  }

  public void unicastParagraph(Note note, Paragraph p, String user) {
    if (!note.isPersonalizedMode() || p == null || user == null) {
      return;
    }

    if (!userConnectedSockets.containsKey(user)) {
      LOG.log(Level.WARNING, "Failed to send unicast. user {} that is not in connections map", user);
      return;
    }

    for (Session conn : userConnectedSockets.get(user)) {
      Message m = new Message(Message.OP.PARAGRAPH).put("paragraph", p);
      unicast(m, conn);
    }
  }

  public void broadcastParagraph(Note note, Paragraph p) {
    if (note.isPersonalizedMode()) {
      broadcastParagraphs(p.getUserParagraphMap(), p);
    } else {
      broadcast(note.getId(), new Message(Message.OP.PARAGRAPH).put("paragraph", p));
    }
  }

  public void broadcastParagraphs(Map<String, Paragraph> userParagraphMap,
      Paragraph defaultParagraph) {
    if (null != userParagraphMap) {
      for (String user : userParagraphMap.keySet()) {
        multicastToUser(user,
            new Message(Message.OP.PARAGRAPH).put("paragraph", userParagraphMap.get(user)));
      }
    }
  }

  private void broadcastNewParagraph(Note note, Paragraph para) {
    LOG.info("Broadcasting paragraph on run call instead of note.");
    int paraIndex = note.getParagraphs().indexOf(para);
    broadcast(note.getId(),
        new Message(Message.OP.PARAGRAPH_ADDED).put("paragraph", para).put("index", paraIndex));
  }

  public void broadcastNoteList(AuthenticationInfo subject, HashSet userAndRoles) {
    if (subject == null) {
      subject = new AuthenticationInfo(StringUtils.EMPTY);
    }
    //send first to requesting user
    List<Map<String, String>> notesInfo = generateNotesInfo(false, subject, userAndRoles);
    multicastToUser(subject.getUser(), new Message(Message.OP.NOTES_INFO).put("notes", notesInfo));
    //to others afterwards
    broadcastNoteListExcept(notesInfo, subject);
  }

  public void unicastNoteList(Session conn, AuthenticationInfo subject,
      HashSet<String> userAndRoles) {
    List<Map<String, String>> notesInfo = generateNotesInfo(false, subject, userAndRoles);
    unicast(new Message(Message.OP.NOTES_INFO).put("notes", notesInfo), conn);
  }

  public void broadcastReloadedNoteList(AuthenticationInfo subject, HashSet userAndRoles) {
    if (subject == null) {
      subject = new AuthenticationInfo(StringUtils.EMPTY);
    }

    //reload and reply first to requesting user
    List<Map<String, String>> notesInfo = generateNotesInfo(true, subject, userAndRoles);
    multicastToUser(subject.getUser(), new Message(Message.OP.NOTES_INFO).put("notes", notesInfo));
    //to others afterwards
    broadcastNoteListExcept(notesInfo, subject);
  }

  private void broadcastNoteListExcept(List<Map<String, String>> notesInfo,
      AuthenticationInfo subject) {
    Set<String> userAndRoles;
    NotebookAuthorization authInfo = NotebookAuthorization.getInstance();
    for (String user : userConnectedSockets.keySet()) {
      if (subject.getUser().equals(user)) {
        continue;
      }
      //reloaded already above; parameter - false
      userAndRoles = authInfo.getRoles(user);
      userAndRoles.add(user);
      notesInfo = generateNotesInfo(false, new AuthenticationInfo(user), userAndRoles);
      multicastToUser(user, new Message(Message.OP.NOTES_INFO).put("notes", notesInfo));
    }
  }

  void permissionError(Session conn, String op, String userName, Set<String> userAndRoles,
      Set<String> allowed, Users user) throws IOException {
    LOG.log(Level.INFO,
        "Cannot {0}. Connection readers {1}. Allowed readers {2}",
        new Object[]{op, userAndRoles, allowed});
    sendMsg(conn, serializeMessage(new Message(Message.OP.AUTH_INFO).
        put("info", "Insufficient privileges to " + op + "note.\n\n"
            + "Allowed users or roles: " + allowed
            .toString() + "\n\n" + "But the user " + user.getLname()
            + " belongs to: " + userAndRoles.toString())));
  }

  /**
   * @return false if user doesn't have reader permission for this paragraph
   */
  private boolean hasParagraphReaderPermission(Session conn,
      Notebook notebook, String noteId,
      HashSet<String> userAndRoles,
      String principal, String op, Users user)
      throws IOException {

    NotebookAuthorization notebookAuthorization = notebook.getNotebookAuthorization();
    if (!notebookAuthorization.isReader(noteId, userAndRoles)) {
      permissionError(conn, op, principal, userAndRoles,
          notebookAuthorization.getOwners(noteId), user);
      return false;
    }

    return true;
  }

  /**
   * @return false if user doesn't have writer permission for this paragraph
   */
  private boolean hasParagraphWriterPermission(Session conn,
      Notebook notebook, String noteId,
      HashSet<String> userAndRoles,
      String principal, String op, Users user)
      throws IOException {

    NotebookAuthorization notebookAuthorization = notebook.getNotebookAuthorization();
    if (!notebookAuthorization.isWriter(noteId, userAndRoles)) {
      permissionError(conn, op, principal, userAndRoles,
          notebookAuthorization.getOwners(noteId), user);
      return false;
    }

    return true;
  }

  /**
   * @return false if user doesn't have owner permission for this paragraph
   */
  private boolean hasParagraphOwnerPermission(Session conn,
      Notebook notebook, String noteId,
      HashSet<String> userAndRoles,
      String principal, String op, Users user)
      throws IOException {

    NotebookAuthorization notebookAuthorization = notebook.getNotebookAuthorization();
    if (!notebookAuthorization.isOwner(noteId, userAndRoles)) {
      permissionError(conn, op, principal, userAndRoles,
          notebookAuthorization.getOwners(noteId), user);
      return false;
    }

    return true;
  }

  public void sendNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user, String hdfsUser) throws IOException {

    LOG.log(Level.FINE, "New operation from {0} : {1} : {2}", new Object[]{
      fromMessage.principal, fromMessage.op, fromMessage.get("id")});

    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null) {

      if (!hasParagraphReaderPermission(conn, notebook, noteId,
          userAndRoles, fromMessage.principal, "read", user)) {
        return;
      }

      addConnectionToNote(note.getId(), conn);

      if (note.isPersonalizedMode()) {
        note = note.getUserNote(hdfsUser);
      }
      sendMsg(conn, serializeMessage(new Message(Message.OP.NOTE).put("note", note)));
      sendAllAngularObjects(note, hdfsUser, conn);
    } else {
      sendMsg(conn, serializeMessage(new Message(Message.OP.NOTE).put("note", null)));
    }
  }

  public void sendHomeNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {
    String noteId = notebook.getConf().getString(ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_HOMESCREEN);
    String projectName = this.project.getName();
    Note note = null;
    if (noteId != null) {
      note = notebook.getNote(noteId);
    }

    if (note != null) {
      if (!hasParagraphReaderPermission(conn, notebook, noteId,
          userAndRoles, fromMessage.principal, "read", user)) {
        return;
      }

      addConnectionToNote(note.getId(), conn);
      sendMsg(conn, serializeMessage(new Message(Message.OP.NOTE).put("note", note)));
      sendAllAngularObjects(note, projectName, conn);
    } else {
      removeConnectionFromAllNote(conn);
      sendMsg(conn, serializeMessage(new Message(Message.OP.NOTE).put("note", null)));
    }
  }

  public void updateNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws SchedulerException, IOException {
    String noteId = (String) fromMessage.get("id");
    String name = (String) fromMessage.get("name");
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");
    if (noteId == null) {
      return;
    }
    if (config == null) {
      return;
    }

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "update", user)) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null) {
      boolean cronUpdated = isCronUpdated(config, note.getConfig());
      note.setName(name);
      note.setConfig(config);
      if (cronUpdated) {
        notebook.refreshCron(note.getId());
      }

      AuthenticationInfo subject = new AuthenticationInfo(this.project.getName());
      note.persist(subject);
      broadcast(note.getId(), new Message(Message.OP.NOTE_UPDATED).put("name", name).put("config", config)
          .put("info", note.getInfo()));
      broadcastNoteList(subject, userAndRoles);
    }
  }

  public void updatePersonalizedMode(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user) throws SchedulerException, IOException {
    String noteId = (String) fromMessage.get("id");
    String personalized = (String) fromMessage.get("personalized");
    boolean isPersonalized = personalized.equals("true") ? true : false;

    if (noteId == null) {
      return;
    }

    if (!hasParagraphOwnerPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "persoanlized", user)) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null) {
      note.setPersonalizedMode(isPersonalized);
      AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
      note.persist(subject);
      broadcastNote(note);
    }
  }

  public void renameNote(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    renameNote(conn, userAndRoles, notebook, fromMessage, "rename", user);
  }

  private void renameNote(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, String op, Users user)
      throws SchedulerException, IOException {
    String noteId = (String) fromMessage.get("id");
    String name = (String) fromMessage.get("name");

    if (noteId == null) {
      return;
    }

    if (!hasParagraphOwnerPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "rename", user)) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null) {
      note.setName(name);

      AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
      note.persist(subject);
      broadcastNote(note);
      broadcastNoteList(subject, userAndRoles);
    }
  }

  public void renameFolder(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    renameFolder(conn, userAndRoles, notebook, fromMessage, "rename", user);
  }

  private void renameFolder(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, String op, Users user)
      throws SchedulerException, IOException {
    String oldFolderId = (String) fromMessage.get("id");
    String newFolderId = (String) fromMessage.get("name");

    if (oldFolderId == null) {
      return;
    }

    for (Note note : notebook.getNotesUnderFolder(oldFolderId)) {
      String noteId = note.getId();
      if (!hasParagraphOwnerPermission(conn, notebook, noteId,
          userAndRoles, fromMessage.principal, op + " folder of '" + note.getName() + "'", user)) {
        return;
      }
    }

    Folder oldFolder = notebook.renameFolder(oldFolderId, newFolderId);

    if (oldFolder != null) {
      AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);

      List<Note> renamedNotes = oldFolder.getNotesRecursively();
      for (Note note : renamedNotes) {
        note.persist(subject);
        broadcastNote(note);
      }

      broadcastNoteList(subject, userAndRoles);
    }
  }

  private boolean isCronUpdated(Map<String, Object> configA, Map<String, Object> configB) {
    boolean cronUpdated = false;
    if (configA.get("cron") != null && configB.get("cron") != null && configA.get("cron")
        .equals(configB.get("cron"))) {
      cronUpdated = true;
    } else if (configA.get("cron") == null && configB.get("cron") == null) {
      cronUpdated = false;
    } else if (configA.get("cron") != null || configB.get("cron") != null) {
      cronUpdated = true;
    }

    return cronUpdated;
  }

  public void createNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message message) throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(message.principal);

    try {
      Note note = null;

      String defaultInterpreterId = (String) message.get("defaultInterpreterId");
      if (!StringUtils.isEmpty(defaultInterpreterId)) {
        List<String> interpreterSettingIds = new LinkedList<>();
        interpreterSettingIds.add(defaultInterpreterId);
        for (String interpreterSettingId : notebook.getInterpreterSettingManager().
            getDefaultInterpreterSettingList()) {
          if (!interpreterSettingId.equals(defaultInterpreterId)) {
            interpreterSettingIds.add(interpreterSettingId);
          }
        }
        note = notebook.createNote(interpreterSettingIds, subject);
      } else {
        note = notebook.createNote(subject);
      }

      note.addNewParagraph(subject); // it's an empty note. so add one paragraph
      if (message != null) {
        String noteName = (String) message.get("name");
        if (StringUtils.isEmpty(noteName)) {
          noteName = "Note " + note.getId();
        }
        note.setName(noteName);
      }

      note.persist(subject);
      addConnectionToNote(note.getId(), (Session) conn);
      sendMsg(conn, serializeMessage(new Message(Message.OP.NEW_NOTE).put("note", note)));
    } catch (FileSystemException e) {
      LOG.log(Level.SEVERE, "Exception from createNote", e);
      sendMsg(conn, serializeMessage(new Message(Message.OP.ERROR_INFO).put("info",
          "Oops! There is something wrong with the notebook file system. "
          + "Please check the logs for more details.")));
      return;
    }
    broadcastNoteList(subject, userAndRoles);
  }

  public void removeNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {
    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }

    if (!hasParagraphOwnerPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "remove", user)) {
      return;
    }

    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    notebook.removeNote(noteId, subject);
    removeNote(noteId);
    broadcastNoteList(subject, userAndRoles);
  }

  public void removeFolder(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    String folderId = (String) fromMessage.get("id");
    if (folderId == null) {
      return;
    }

    String hopsworksUserRole = projectTeamFacade.findCurrentRole(project, user);
    if (hopsworksUserRole == null) {
      String errorMsg = "Hopsworks role for user " + user.getUsername() +
          " should not be null";
      sendMsg(conn,
          serializeMessage(new Message(Message.OP.ERROR_INFO).put
              ("info", errorMsg)));
      throw new IOException(errorMsg);
    }
    
    if (!hopsworksUserRole.equals(AllowedProjectRoles.DATA_OWNER)) {
      String errorMsg = "User " + user.getUsername() + " is not a DATA " +
          "OWNER for this project and is not allowed to empty the Trash";
      sendMsg(conn,
          serializeMessage(new Message(Message.OP.ERROR_INFO).put
              ("info", errorMsg)));
      throw new IOException(errorMsg);
    }
    List<Note> notes = notebook.getNotesUnderFolder(folderId);
    for (Note note : notes) {
      String noteId = note.getId();

      if (!hasParagraphOwnerPermission(conn, notebook, noteId,
          userAndRoles, fromMessage.principal, "remove folder of '" + note.getName() + "'", user)) {
        return;
      }
    }

    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    for (Note note : notes) {
      notebook.removeNote(note.getId(), subject);
      removeNote(note.getId());
    }
    broadcastNoteList(subject, userAndRoles);
  }
  
  
  private void logTrashActivity(String activityMsg, Users user) {
    Activity activity = new Activity();
    Date now = new Date();
    activity.setActivity(activityMsg);
    activity.setFlag(ActivityFacade.FLAG_PROJECT);
    activity.setProject(project);
    activity.setTimestamp(now);
    activity.setUser(user);
    activityFacade.persistActivity(activity);
  }
  
  public void moveNoteToTrash(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    String noteId = (String) fromMessage.get("id");
    if (noteId == null) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null && !note.isTrash()) {
      String noteName = note.getName();
      fromMessage.put("name", Folder.TRASH_FOLDER_ID + "/" + note.getName());
      renameNote(conn, userAndRoles, notebook, fromMessage, "move", user);
      notebook.moveNoteToTrash(note.getId());
      
      logTrashActivity(ActivityFacade.TRASH_NOTEBOOK + "notebook " + noteName,
          user);
    }
  }

  public void moveFolderToTrash(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    String folderId = (String) fromMessage.get("id");
    if (folderId == null) {
      return;
    }

    Folder folder = notebook.getFolder(folderId);
    if (folder != null && !folder.isTrash()) {
      String trashFolderId = Folder.TRASH_FOLDER_ID + "/" + folderId;
      if (notebook.hasFolder(trashFolderId)) {
        DateTime currentDate = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        trashFolderId += Folder.TRASH_FOLDER_CONFLICT_INFIX + formatter.print(currentDate);
      }

      fromMessage.put("name", trashFolderId);
      renameFolder(conn, userAndRoles, notebook, fromMessage, "move", user);
      
      logTrashActivity(ActivityFacade.TRASH_NOTEBOOK + "notebook folder " +
          folderId + "/", user);
    }
  }

  public void restoreNote(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    String noteId = (String) fromMessage.get("id");

    if (noteId == null) {
      return;
    }

    Note note = notebook.getNote(noteId);
    if (note != null && note.isTrash()) {
      fromMessage.put("name", note.getName().replaceFirst(Folder.TRASH_FOLDER_ID + "/", ""));
      renameNote(conn, userAndRoles, notebook, fromMessage, "restore", user);
    }
  }

  public void restoreFolder(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    String folderId = (String) fromMessage.get("id");

    if (folderId == null) {
      return;
    }

    Folder folder = notebook.getFolder(folderId);
    if (folder != null && folder.isTrash()) {
      String restoreName = folder.getId().replaceFirst(Folder.TRASH_FOLDER_ID + "/", "").trim();

      // if the folder had conflict when it had moved to trash before
      Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");
      Matcher m = p.matcher(restoreName);
      restoreName = m.replaceAll("").trim();

      fromMessage.put("name", restoreName);
      renameFolder(conn, userAndRoles, notebook, fromMessage, "restore", user);
    }
  }

  public void restoreAll(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    Folder trashFolder = notebook.getFolder(Folder.TRASH_FOLDER_ID);
    if (trashFolder != null) {
      fromMessage.data = new HashMap<>();
      fromMessage.put("id", Folder.TRASH_FOLDER_ID);
      fromMessage.put("name", Folder.ROOT_FOLDER_ID);
      renameFolder(conn, userAndRoles, notebook, fromMessage, "restore trash", user);
    }
  }

  public void emptyTrash(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws SchedulerException, IOException {
    fromMessage.data = new HashMap<>();
    fromMessage.put("id", Folder.TRASH_FOLDER_ID);
    removeFolder(conn, userAndRoles, notebook, fromMessage, user);
  }

  public void updateParagraph(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user) throws IOException {
    String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    Map<String, Object> params = (Map<String, Object>) fromMessage.get("params");
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");
    String noteId = getOpenNoteId(conn);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    final Note note = notebook.getNote(noteId);
    Paragraph p = note.getParagraph(paragraphId);

    p.settings.setParams(params);
    p.setConfig(config);
    p.setTitle((String) fromMessage.get("title"));
    p.setText((String) fromMessage.get("paragraph"));

    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    if (note.isPersonalizedMode()) {
      p = p.getUserParagraph(subject.getUser());
      p.settings.setParams(params);
      p.setConfig(config);
      p.setTitle((String) fromMessage.get("title"));
      p.setText((String) fromMessage.get("paragraph"));
    }

    note.persist(subject);

    if (note.isPersonalizedMode()) {
      Map<String, Paragraph> userParagraphMap = note.getParagraph(paragraphId).getUserParagraphMap();
      broadcastParagraphs(userParagraphMap, p);
    } else {
      broadcastParagraph(note, p);
    }
  }

  public void cloneNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage) throws IOException, CloneNotSupportedException {
    String noteId = getOpenNoteId(conn);
    String name = (String) fromMessage.get("name");
    Note newNote = notebook.cloneNote(noteId, name, new AuthenticationInfo(fromMessage.principal));
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    addConnectionToNote(newNote.getId(), conn);
    sendMsg(conn, serializeMessage(new Message(Message.OP.NEW_NOTE).put("note", newNote)));
    broadcastNoteList(subject, userAndRoles);
  }

  public void clearAllParagraphOutput(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user) throws IOException {
    final String noteId = (String) fromMessage.get("id");
    if (StringUtils.isBlank(noteId)) {
      return;
    }

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "clear output", user)) {
      return;
    }

    Note note = notebook.getNote(noteId);
    note.clearAllParagraphOutput();
    broadcastNote(note);
  }

  protected Note importNote(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage) throws IOException {
    Note note = null;
    if (fromMessage != null) {
      String noteName = (String) ((Map) fromMessage.get("note")).get("name");
      String noteJson = gson.toJson(fromMessage.get("note"));
      AuthenticationInfo subject = null;
      if (fromMessage.principal != null) {
        subject = new AuthenticationInfo(fromMessage.principal);
      } else {
        subject = new AuthenticationInfo("anonymous");
      }
      note = notebook.importNote(noteJson, noteName, subject);
      note.persist(subject);
      broadcastNote(note);
      broadcastNoteList(subject, userAndRoles);
    }
    return note;
  }

  public void removeParagraph(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }
    String noteId = getOpenNoteId(conn);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    /**
     * We dont want to remove the last paragraph
     */
    final Note note = notebook.getNote(noteId);
    if (!note.isLastParagraph(paragraphId)) {
      AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
      Paragraph para = note.removeParagraph(subject.getUser(), paragraphId);
      note.persist(subject);
      if (para != null) {
        broadcast(note.getId(), new Message(Message.OP.PARAGRAPH_REMOVED).
            put("id", para.getId()));
      }
    }
  }

  public void clearParagraphOutput(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user, String hdfsUser) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    String noteId = getOpenNoteId(conn);
    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    final Note note = notebook.getNote(noteId);
    if (note.isPersonalizedMode()) {
      Paragraph p = note.clearPersonalizedParagraphOutput(paragraphId, hdfsUser);
      unicastParagraph(note, p, hdfsUser);
    } else {
      note.clearParagraphOutput(paragraphId);
      Paragraph paragraph = note.getParagraph(paragraphId);
      broadcastParagraph(note, paragraph);
    }
  }

  public void completion(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage) throws IOException {
    String paragraphId = (String) fromMessage.get("id");
    String buffer = (String) fromMessage.get("buf");
    int cursor = (int) Double.parseDouble(fromMessage.get("cursor").toString());
    Message resp = new Message(Message.OP.COMPLETION_LIST).put("id", paragraphId);
    if (paragraphId == null) {
      sendMsg(conn, serializeMessage(resp));
      return;
    }

    final Note note = notebook.getNote(getOpenNoteId(conn));
    List<InterpreterCompletion> candidates = note.completion(paragraphId, buffer, cursor);
    resp.put("completions", candidates);
    sendMsg(conn, serializeMessage(resp));
  }

  /**
   * When angular object updated from client
   *
   * @param conn the web socket.
   * @param notebook the notebook.
   * @param fromMessage the message.
   */
  public void angularObjectUpdated(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage) {
    String noteId = (String) fromMessage.get("noteId");
    String paragraphId = (String) fromMessage.get("paragraphId");
    String interpreterGroupId = (String) fromMessage.get("interpreterGroupId");
    String varName = (String) fromMessage.get("name");
    Object varValue = fromMessage.get("value");
    String user = this.project.getName();
    AngularObject ao = null;
    boolean global = false;
    // propagate change to (Remote) AngularObjectRegistry
    Note note = notebook.getNote(noteId);
    if (note != null) {
      List<InterpreterSetting> settings = notebook.getInterpreterSettingManager().getInterpreterSettings(note.getId());
      for (InterpreterSetting setting : settings) {
        if (setting.getInterpreterGroup(user, note.getId()) == null) {
          continue;
        }
        if (interpreterGroupId.equals(setting.getInterpreterGroup(user, note.getId()).getId())) {
          AngularObjectRegistry angularObjectRegistry = setting.getInterpreterGroup(user, note.getId()).
              getAngularObjectRegistry();

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
        List<InterpreterSetting> settings = notebook.getInterpreterSettingManager()
            .getInterpreterSettings(note.getId());
        for (InterpreterSetting setting : settings) {
          if (setting.getInterpreterGroup(user, n.getId()) == null) {
            continue;
          }
          if (interpreterGroupId.equals(setting.getInterpreterGroup(user, n.getId()).getId())) {
            AngularObjectRegistry angularObjectRegistry = setting.getInterpreterGroup(user, n.getId()).
                getAngularObjectRegistry();
            this.broadcastExcept(n.getId(),
                new Message(Message.OP.ANGULAR_OBJECT_UPDATE).put("angularObject", ao)
                .put("interpreterGroupId", interpreterGroupId).put("noteId", n.getId())
                .put("paragraphId", ao.getParagraphId()), conn);
          }
        }
      }
    } else { // broadcast to all web session for the note
      this.broadcastExcept(note.getId(),
          new Message(Message.OP.ANGULAR_OBJECT_UPDATE).put("angularObject", ao)
          .put("interpreterGroupId", interpreterGroupId).put("noteId", note.getId())
          .put("paragraphId", ao.getParagraphId()), conn);
    }
  }

  /**
   * Push the given Angular variable to the target
   * interpreter angular registry given a noteId
   * and a paragraph id
   */
  protected void angularObjectClientBind(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage) throws Exception {
    String noteId = fromMessage.getType("noteId");
    String varName = fromMessage.getType("name");
    Object varValue = fromMessage.get("value");
    String paragraphId = fromMessage.getType("paragraphId");
    Note note = notebook.getNote(noteId);

    if (paragraphId == null) {
      throw new IllegalArgumentException(
          "target paragraph not specified for " + "angular value bind");
    }

    if (note != null) {
      final InterpreterGroup interpreterGroup = findInterpreterGroupForParagraph(note, paragraphId);

      final AngularObjectRegistry registry = interpreterGroup.getAngularObjectRegistry();
      if (registry instanceof RemoteAngularObjectRegistry) {

        RemoteAngularObjectRegistry remoteRegistry = (RemoteAngularObjectRegistry) registry;
        pushAngularObjectToRemoteRegistry(noteId, paragraphId, varName, varValue, remoteRegistry,
            interpreterGroup.getId(), conn);

      } else {
        pushAngularObjectToLocalRepo(noteId, paragraphId, varName, varValue, registry,
            interpreterGroup.getId(), conn);
      }
    }
  }

  /**
   * Remove the given Angular variable to the target
   * interpreter(s) angular registry given a noteId
   * and an optional list of paragraph id(s)
   */
  protected void angularObjectClientUnbind(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage) throws Exception {
    String noteId = fromMessage.getType("noteId");
    String varName = fromMessage.getType("name");
    String paragraphId = fromMessage.getType("paragraphId");
    Note note = notebook.getNote(noteId);

    if (paragraphId == null) {
      throw new IllegalArgumentException(
          "target paragraph not specified for " + "angular value unBind");
    }

    if (note != null) {
      final InterpreterGroup interpreterGroup = findInterpreterGroupForParagraph(note, paragraphId);

      final AngularObjectRegistry registry = interpreterGroup.getAngularObjectRegistry();

      if (registry instanceof RemoteAngularObjectRegistry) {
        RemoteAngularObjectRegistry remoteRegistry = (RemoteAngularObjectRegistry) registry;
        removeAngularFromRemoteRegistry(noteId, paragraphId, varName, remoteRegistry,
            interpreterGroup.getId(), conn);
      } else {
        removeAngularObjectFromLocalRepo(noteId, paragraphId, varName, registry,
            interpreterGroup.getId(), conn);
      }
    }
  }

  private InterpreterGroup findInterpreterGroupForParagraph(Note note, String paragraphId)
      throws Exception {
    final Paragraph paragraph = note.getParagraph(paragraphId);
    if (paragraph == null) {
      throw new IllegalArgumentException("Unknown paragraph with id : " + paragraphId);
    }
    return paragraph.getCurrentRepl().getInterpreterGroup();
  }

  private void pushAngularObjectToRemoteRegistry(String noteId, String paragraphId, String varName,
      Object varValue, RemoteAngularObjectRegistry remoteRegistry, String interpreterGroupId,
      Session conn) {

    final AngularObject ao = remoteRegistry.addAndNotifyRemoteProcess(varName, varValue, noteId, paragraphId);

    this.broadcastExcept(noteId, new Message(Message.OP.ANGULAR_OBJECT_UPDATE).put("angularObject", ao)
        .put("interpreterGroupId", interpreterGroupId).put("noteId", noteId)
        .put("paragraphId", paragraphId), conn);
  }

  private void removeAngularFromRemoteRegistry(String noteId, String paragraphId, String varName,
      RemoteAngularObjectRegistry remoteRegistry, String interpreterGroupId, Session conn) {
    final AngularObject ao = remoteRegistry.removeAndNotifyRemoteProcess(varName, noteId, paragraphId);
    this.broadcastExcept(noteId, new Message(Message.OP.ANGULAR_OBJECT_REMOVE).put("angularObject", ao)
        .put("interpreterGroupId", interpreterGroupId).put("noteId", noteId)
        .put("paragraphId", paragraphId), conn);
  }

  private void pushAngularObjectToLocalRepo(String noteId, String paragraphId, String varName,
      Object varValue, AngularObjectRegistry registry, String interpreterGroupId,
      Session conn) {
    AngularObject angularObject = registry.get(varName, noteId, paragraphId);
    if (angularObject == null) {
      angularObject = registry.add(varName, varValue, noteId, paragraphId);
    } else {
      angularObject.set(varValue, true);
    }

    this.broadcastExcept(noteId,
        new Message(Message.OP.ANGULAR_OBJECT_UPDATE).put("angularObject", angularObject)
        .put("interpreterGroupId", interpreterGroupId).put("noteId", noteId)
        .put("paragraphId", paragraphId), conn);
  }

  private void removeAngularObjectFromLocalRepo(String noteId, String paragraphId, String varName,
      AngularObjectRegistry registry, String interpreterGroupId, Session conn) {
    final AngularObject removed = registry.remove(varName, noteId, paragraphId);
    if (removed != null) {
      this.broadcastExcept(noteId,
          new Message(Message.OP.ANGULAR_OBJECT_REMOVE).put("angularObject", removed)
          .put("interpreterGroupId", interpreterGroupId).put("noteId", noteId)
          .put("paragraphId", paragraphId), conn);
    }
  }

  public void moveParagraph(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    final int newIndex = (int) Double.parseDouble(fromMessage.get("index").toString());
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    note.moveParagraph(paragraphId, newIndex);
    note.persist(subject);
    broadcast(note.getId(),
        new Message(Message.OP.PARAGRAPH_MOVED).put("id", paragraphId).put("index", newIndex));
  }

  public String insertParagraph(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user) throws IOException {
    final int index = (int) Double.parseDouble(fromMessage.get("index").toString());
    String noteId = getOpenNoteId(conn);
    final Note note = notebook.getNote(noteId);
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return null;
    }
    Map<String, Object> config;
    if (fromMessage.get("config") != null) {
      config = (Map<String, Object>) fromMessage.get("config");
    } else {
      config = new HashMap<>();
    }

    Paragraph newPara = note.insertNewParagraph(index, subject);
    newPara.setConfig(config);
    note.persist(subject);
    broadcastNewParagraph(note, newPara);

    return newPara.getId();
  }

  public void copyParagraph(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {
    String newParaId = insertParagraph(conn, userAndRoles, notebook, fromMessage, user);

    if (newParaId == null) {
      return;
    }
    fromMessage.put("id", newParaId);

    updateParagraph(conn, userAndRoles, notebook, fromMessage, user);
  }

  public void cancelParagraph(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    String noteId = getOpenNoteId(conn);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    final Note note = notebook.getNote(noteId);
    Paragraph p = note.getParagraph(paragraphId);
    p.abort();
  }

  public void runAllParagraphs(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user, CertificateMaterializer certificateMaterializer,
      Settings settings, DistributedFsService dfsService) throws IOException {
    final String noteId = (String) fromMessage.get("noteId");
    if (StringUtils.isBlank(noteId)) {
      return;
    }

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "run all paragraphs", user)) {
      return;
    }

    List<Map<String, Object>> paragraphs = gson.fromJson(String.valueOf(fromMessage.data.get("paragraphs")),
        new TypeToken<List<Map<String, Object>>>() {}.getType());

    for (Map<String, Object> raw : paragraphs) {
      String paragraphId = (String) raw.get("id");
      if (paragraphId == null) {
        continue;
      }

      String text = (String) raw.get("paragraph");
      String title = (String) raw.get("title");
      Map<String, Object> params = (Map<String, Object>) raw.get("params");
      Map<String, Object> config = (Map<String, Object>) raw.get("config");

      Note note = notebook.getNote(noteId);
      Paragraph p = setParagraphUsingMessage(note, fromMessage,
          paragraphId, text, title, params, config);

      // Hack: in case the interpreter is hopshive, put the keystore/truststore password in the ticket
      // so that it can be retrieved from the zeppelin-hopshive interpreter
      if ((p.getRequiredReplName() != null && p.getRequiredReplName().equals("hopshive")) ||
          (p.getRequiredReplName() == null && notebook().getInterpreterSettingManager().
              getDefaultInterpreterSetting(note.getId()).getName().equals("hopshive"))){
        setCertificatePassword(p, fromMessage);
      }

      persistAndExecuteSingleParagraph(conn, note, p, certificateMaterializer, settings, dfsService);
    }
  }

  public void broadcastSpellExecution(Session conn, HashSet<String> userAndRoles,
      Notebook notebook, Message fromMessage, Users user)
      throws IOException {

    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    String noteId = getOpenNoteId(conn);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    String text = (String) fromMessage.get("paragraph");
    String title = (String) fromMessage.get("title");
    Job.Status status = Job.Status.valueOf((String) fromMessage.get("status"));
    Map<String, Object> params = (Map<String, Object>) fromMessage.get("params");
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");

    final Note note = notebook.getNote(noteId);
    Paragraph p = setParagraphUsingMessage(note, fromMessage, paragraphId,
        text, title, params, config);
    p.setResult(fromMessage.get("results"));
    p.setErrorMessage((String) fromMessage.get("errorMessage"));
    p.setStatusWithoutNotification(status);

    // Spell uses ISO 8601 formatted string generated from moment
    String dateStarted = (String) fromMessage.get("dateStarted");
    String dateFinished = (String) fromMessage.get("dateFinished");
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    try {
      p.setDateStarted(df.parse(dateStarted));
    } catch (ParseException e) {
      LOG.log(Level.SEVERE, "Failed parse dateStarted: {0}", e);
    }

    try {
      p.setDateFinished(df.parse(dateFinished));
    } catch (ParseException e) {
      LOG.log(Level.SEVERE, "Failed parse dateFinished: {0}", e);
    }

    addNewParagraphIfLastParagraphIsExecuted(note, p);
    if (!persistNoteWithAuthInfo(conn, note, p)) {
      return;
    }

    // broadcast to other clients only
    broadcastExcept(note.getId(),
        new Message(Message.OP.RUN_PARAGRAPH_USING_SPELL).put("paragraph", p), conn);
  }

  public void runParagraph(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user, CertificateMaterializer certificateMaterializer,
      Settings settings, DistributedFsService dfsService) throws IOException {
    final String paragraphId = (String) fromMessage.get("id");
    if (paragraphId == null) {
      return;
    }

    String noteId = getOpenNoteId(conn);
    Properties props = notebook.getInterpreterSettingManager().getInterpreterSettings(noteId).get(0).
        getFlatProperties();
    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "write", user)) {
      return;
    }

    // 1. clear paragraph only if personalized,
    // otherwise this will be handed in `onOutputClear`
    final Note note = notebook.getNote(noteId);
    if (note.isPersonalizedMode()) {
      Paragraph p = note.clearPersonalizedParagraphOutput(paragraphId, fromMessage.principal);
      unicastParagraph(note, p, fromMessage.principal);
    }

    // 2. set paragraph values
    String text = (String) fromMessage.get("paragraph");
    String title = (String) fromMessage.get("title");
    Map<String, Object> params = (Map<String, Object>) fromMessage.get("params");
    Map<String, Object> config = (Map<String, Object>) fromMessage.get("config");

    Paragraph p = setParagraphUsingMessage(note, fromMessage, paragraphId,
        text, title, params, config);


    // Hack: in case the interpreter is hopshive, put the keystore/truststore password in the ticket
    // so that it can be retrieved from the zeppelin-hopshive interpreter
    if ((p.getRequiredReplName() != null && p.getRequiredReplName().equals("hopshive")) ||
        (p.getRequiredReplName() == null && notebook().getInterpreterSettingManager().
            getDefaultInterpreterSetting(note.getId()).getName().equals("hopshive"))){
      setCertificatePassword(p, fromMessage);
    }

    persistAndExecuteSingleParagraph(conn, note, p, certificateMaterializer, settings, dfsService);
  }

  private void setCertificatePassword(Paragraph p, Message fromMessage) {
    if (certPwd == null || certPwd.isEmpty()) {
      Users user = project.getOwner();
      try {
        ProjectGenericUserCerts serviceCert =
            certsFacade.findProjectGenericUserCerts(project.getProjectGenericUser());
        certPwd = HopsUtils.decrypt(user.getPassword(), serviceCert.getCertificatePassword(),
            certificatesMgmService.getMasterEncryptionPassword());
      } catch (Exception e) {
        LOG.log(Level.SEVERE, "Cannot retrieve user " + project.getName()  +
            " keystore password. " + e);
        certPwd = "";
      }
    }
    p.setAuthenticationInfo(new AuthenticationInfo(fromMessage.principal, fromMessage.roles, certPwd));
  }

  private void addNewParagraphIfLastParagraphIsExecuted(Note note, Paragraph p) {
    // if it's the last paragraph and empty, let's add a new one
    boolean isTheLastParagraph = note.isLastParagraph(p.getId());
    if (!(Strings.isNullOrEmpty(p.getText()) || p.getText().trim().equals(p.getMagic())) && isTheLastParagraph) {
      Paragraph newPara = note.addNewParagraph(p.getAuthenticationInfo());
      broadcastNewParagraph(note, newPara);
    }
  }

  /**
   * @return false if failed to save a note
   */
  private boolean persistNoteWithAuthInfo(Session conn,
      Note note, Paragraph p) throws IOException {
    try {
      note.persist(p.getAuthenticationInfo());
      return true;
    } catch (FileSystemException ex) {
      LOG.log(Level.SEVERE, "Exception from run", ex);
      sendMsg(conn, serializeMessage(new Message(Message.OP.ERROR_INFO).put("info",
          "Oops! There is something wrong with the notebook file system. "
          + "Please check the logs for more details.")));
      // don't run the paragraph when there is error on persisting the note information
      return false;
    }
  }

  private void persistAndExecuteSingleParagraph(Session conn,
      Note note, Paragraph p, CertificateMaterializer certificateMaterializer,
      Settings settings, DistributedFsService dfsService) throws IOException {
    addNewParagraphIfLastParagraphIsExecuted(note, p);
    if (!persistNoteWithAuthInfo(conn, note, p)) {
      return;
    }

    // Materialize certificates both in local filesystem and
    // in HDFS for the interpreters
    if (certificateMaterializer.openedInterpreter(project.getId())) {
    
      DistributedFileSystemOps dfso = null;
      try {
        dfso = dfsService.getDfsOps();
        HopsUtils.materializeCertificatesForProject(project.getName(),
            settings.getHdfsTmpCertDir(), dfso, certificateMaterializer,
            settings);
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Error while materializing certificates for Zeppelin", ex);
        certificateMaterializer.closedInterpreter(project.getId());
        HopsUtils.cleanupCertificatesForProject(project.getName(), settings.getHdfsTmpCertDir(),
            certificateMaterializer, settings);
        throw ex;
      } finally {
        if (null != dfso) {
          dfso.close();
          dfso = null;
        }
      }
    }

    try {
      note.run(p.getId());
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Exception from run", ex);
      if (p != null) {
        p.setReturn(new InterpreterResult(InterpreterResult.Code.ERROR, ex.getMessage()), ex);
        p.setStatus(Job.Status.ERROR);
        broadcast(note.getId(), new Message(Message.OP.PARAGRAPH).put("paragraph", p));
      }
    }
  }

  private Paragraph setParagraphUsingMessage(Note note, Message fromMessage, String paragraphId,
      String text, String title, Map<String, Object> params,
      Map<String, Object> config) {
    Paragraph p = note.getParagraph(paragraphId);
    p.setText(text);
    p.setTitle(title);
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal, fromMessage.roles, fromMessage.ticket);
    p.setAuthenticationInfo(subject);
    p.settings.setParams(params);
    p.setConfig(config);

    if (note.isPersonalizedMode()) {
      p = note.getParagraph(paragraphId);
      p.setText(text);
      p.setTitle(title);
      p.setAuthenticationInfo(subject);
      p.settings.setParams(params);
      p.setConfig(config);
    }

    return p;
  }

  public void sendAllConfigurations(Session conn, HashSet<String> userAndRoles,
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

    sendMsg(conn, serializeMessage(new Message(Message.OP.CONFIGURATIONS_INFO).put("configurations", configurations)));
  }

  public void checkpointNote(Session conn, Notebook notebook, Message fromMessage)
      throws IOException {
    String noteId = (String) fromMessage.get("noteId");
    String commitMessage = (String) fromMessage.get("commitMessage");
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    NotebookRepo.Revision revision = notebook.checkpointNote(noteId, commitMessage, subject);
    if (!NotebookRepo.Revision.isEmpty(revision)) {
      List<NotebookRepo.Revision> revisions = notebook.listRevisionHistory(noteId, subject);
      sendMsg(conn, serializeMessage(new Message(Message.OP.LIST_REVISION_HISTORY).put("revisionList", revisions)));
    } else {
      sendMsg(conn, serializeMessage(new Message(Message.OP.ERROR_INFO).put("info",
          "Couldn't checkpoint note revision: possibly storage "
          + "doesn't support versioning. Please check the logs for"
          + " more details.")));
    }
  }

  public void listRevisionHistory(Session conn, Notebook notebook, Message fromMessage)
      throws IOException {
    String noteId = (String) fromMessage.get("noteId");
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    List<NotebookRepo.Revision> revisions = notebook.listRevisionHistory(noteId, subject);

    sendMsg(conn, serializeMessage(new Message(Message.OP.LIST_REVISION_HISTORY).put("revisionList", revisions)));
  }

  public void setNoteRevision(Session conn, HashSet<String> userAndRoles, Notebook notebook,
      Message fromMessage, Users user) throws IOException {

    String noteId = (String) fromMessage.get("noteId");
    String revisionId = (String) fromMessage.get("revisionId");
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);

    if (!hasParagraphWriterPermission(conn, notebook, noteId,
        userAndRoles, fromMessage.principal, "update", user)) {
      return;
    }

    Note headNote = null;
    boolean setRevisionStatus;
    try {
      headNote = notebook.setNoteRevision(noteId, revisionId, subject);
      setRevisionStatus = headNote != null;
    } catch (Exception e) {
      setRevisionStatus = false;
      LOG.log(Level.SEVERE, "Failed to set given note revision", e);
    }
    if (setRevisionStatus) {
      notebook.loadNoteFromRepo(noteId, subject);
    }

    sendMsg(conn, serializeMessage(new Message(Message.OP.SET_NOTE_REVISION).put("status", setRevisionStatus)));

    if (setRevisionStatus) {
      Note reloadedNote = notebook.getNote(headNote.getId());
      broadcastNote(reloadedNote);
    } else {
      sendMsg(conn, serializeMessage(new Message(Message.OP.ERROR_INFO).put("info",
          "Couldn't set note to the given revision. "
          + "Please check the logs for more details.")));
    }
  }

  public void getNoteByRevision(Session conn, Notebook notebook, Message fromMessage)
      throws IOException {
    String noteId = (String) fromMessage.get("noteId");
    String revisionId = (String) fromMessage.get("revisionId");
    AuthenticationInfo subject = new AuthenticationInfo(fromMessage.principal);
    Note revisionNote = notebook.getNoteByRevision(noteId, revisionId, subject);
    sendMsg(conn, serializeMessage(new Message(Message.OP.NOTE_REVISION).put("noteId", noteId).put("revisionId",
        revisionId)
        .put("note", revisionNote)));
  }

  /**
   * This callback is for the paragraph that runs on ZeppelinServer
   *
   * @param output output to append
   */
  @Override
  public void onOutputAppend(String noteId, String paragraphId, int index, String output) {
    Message msg = new Message(Message.OP.PARAGRAPH_APPEND_OUTPUT).put("noteId", noteId)
        .put("paragraphId", paragraphId).put("index", index).put("data", output);
    broadcast(noteId, msg);
  }

  /**
   * This callback is for the paragraph that runs on ZeppelinServer
   *
   * @param output output to update (replace)
   */
  @Override
  public void onOutputUpdated(String noteId, String paragraphId, int index,
      InterpreterResult.Type type, String output) {
    Message msg = new Message(Message.OP.PARAGRAPH_UPDATE_OUTPUT).put("noteId", noteId)
        .put("paragraphId", paragraphId).put("index", index).put("type", type).put("data", output);
    Note note = notebook().getNote(noteId);
    if (note.isPersonalizedMode()) {
      String user = note.getParagraph(paragraphId).getUser();
      if (null != user) {
        multicastToUser(user, msg);
      }
    } else {
      broadcast(noteId, msg);
    }
  }

  /**
   * This callback is for the paragraph that runs on ZeppelinServer
   */
  @Override
  public void onOutputClear(String noteId, String paragraphId) {
    Notebook notebook = notebook();
    final Note note = notebook.getNote(noteId);
    note.clearParagraphOutput(paragraphId);
    Paragraph paragraph = note.getParagraph(paragraphId);
    broadcastParagraph(note, paragraph);
  }

  /**
   * When application append output
   */
  @Override
  public void onOutputAppend(String noteId, String paragraphId, int index, String appId,
      String output) {
    Message msg = new Message(Message.OP.APP_APPEND_OUTPUT).put("noteId", noteId).put("paragraphId", paragraphId)
        .put("index", index).put("appId", appId).put("data", output);
    broadcast(noteId, msg);
  }

  /**
   * When application update output
   */
  @Override
  public void onOutputUpdated(String noteId, String paragraphId, int index, String appId,
      InterpreterResult.Type type, String output) {
    Message msg = new Message(Message.OP.APP_UPDATE_OUTPUT).put("noteId", noteId).put("paragraphId", paragraphId)
        .put("index", index).put("type", type).put("appId", appId).put("data", output);
    broadcast(noteId, msg);
  }

  @Override
  public void onLoad(String noteId, String paragraphId, String appId, HeliumPackage pkg) {
    Message msg = new Message(Message.OP.APP_LOAD).put("noteId", noteId).put("paragraphId", paragraphId)
        .put("appId", appId).put("pkg", pkg);
    broadcast(noteId, msg);
  }

  @Override
  public void onStatusChange(String noteId, String paragraphId, String appId, String status) {
    Message msg = new Message(Message.OP.APP_STATUS_CHANGE).put("noteId", noteId).put("paragraphId", paragraphId)
        .put("appId", appId).put("status", status);
    broadcast(noteId, msg);
  }

  @Override
  public void onGetParagraphRunners(String noteId, String paragraphId,
      RemoteInterpreterProcessListener.RemoteWorksEventListener callback) {
    Notebook notebookIns = notebook();
    List<InterpreterContextRunner> runner = new LinkedList<>();

    if (notebookIns == null) {
      LOG.info("intepreter request notebook instance is null");
      callback.onFinished(notebookIns);
    }

    try {
      Note note = notebookIns.getNote(noteId);
      if (note != null) {
        if (paragraphId != null) {
          Paragraph paragraph = note.getParagraph(paragraphId);
          if (paragraph != null) {
            runner.add(paragraph.getInterpreterContextRunner());
          }
        } else {
          for (Paragraph p : note.getParagraphs()) {
            runner.add(p.getInterpreterContextRunner());
          }
        }
      }
      callback.onFinished(runner);
    } catch (NullPointerException e) {
      LOG.log(Level.SEVERE, e.getMessage());
      callback.onError();
    }
  }

  @Override
  public void onRemoteRunParagraph(String noteId, String paragraphId) throws Exception {
    Notebook notebookIns = notebook();
    try {
      if (notebookIns == null) {
        throw new Exception("onRemoteRunParagraph notebook instance is null");
      }
      Note noteIns = notebookIns.getNote(noteId);
      if (noteIns == null) {
        throw new Exception(String.format("Can't found note id %s", noteId));
      }

      Paragraph paragraph = noteIns.getParagraph(paragraphId);
      if (paragraph == null) {
        throw new Exception(String.format("Can't found paragraph %s %s", noteId, paragraphId));
      }

      Set<String> userAndRoles = Sets.newHashSet();
      userAndRoles.add(SecurityUtils.getPrincipal());
      userAndRoles.addAll(SecurityUtils.getRoles());
      if (!notebookIns.getNotebookAuthorization().hasWriteAuthorization(userAndRoles, noteId)) {
        throw new ForbiddenException(String.format("can't execute note %s", noteId));
      }

      AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
      paragraph.setAuthenticationInfo(subject);

      noteIns.run(paragraphId);

    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Notebook Information Change event
   */
  public static class NotebookInformationListener implements NotebookEventListener {

    private NotebookServerImpl notebookServer;

    public NotebookInformationListener(NotebookServerImpl notebookServer) {
      this.notebookServer = notebookServer;
    }

    @Override
    public void onParagraphRemove(Paragraph p) {
      try {
        notebookServer.broadcastUpdateNoteJobInfo(System.currentTimeMillis() - 5000);
      } catch (IOException ioe) {
        LOG.log(Level.SEVERE, "can not broadcast for job manager {}", ioe.getMessage());
      }
    }

    @Override
    public void onNoteRemove(Note note) {
      try {
        notebookServer.broadcastUpdateNoteJobInfo(System.currentTimeMillis() - 5000);
      } catch (IOException ioe) {
        LOG.log(Level.SEVERE, "can not broadcast for job manager {}", ioe.getMessage());
      }

      List<Map<String, Object>> notesInfo = new LinkedList<>();
      Map<String, Object> info = new HashMap<>();
      info.put("noteId", note.getId());
      // set paragraphs
      List<Map<String, Object>> paragraphsInfo = new LinkedList<>();

      // notebook json object root information.
      info.put("isRunningJob", false);
      info.put("unixTimeLastRun", 0);
      info.put("isRemoved", true);
      info.put("paragraphs", paragraphsInfo);
      notesInfo.add(info);

      Map<String, Object> response = new HashMap<>();
      response.put("lastResponseUnixTime", System.currentTimeMillis());
      response.put("jobs", notesInfo);

      notebookServer.broadcast(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(),
          new Message(Message.OP.LIST_UPDATE_NOTE_JOBS).put("noteRunningJobs", response));

    }

    @Override
    public void onParagraphCreate(Paragraph p) {
      Notebook notebook = notebookServer.notebook();
      List<Map<String, Object>> notebookJobs = notebook.getJobListByParagraphId(p.getId());
      Map<String, Object> response = new HashMap<>();
      response.put("lastResponseUnixTime", System.currentTimeMillis());
      response.put("jobs", notebookJobs);

      notebookServer.broadcast(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(),
          new Message(Message.OP.LIST_UPDATE_NOTE_JOBS).put("noteRunningJobs", response));
    }

    @Override
    public void onNoteCreate(Note note) {
      Notebook notebook = notebookServer.notebook();
      List<Map<String, Object>> notebookJobs = notebook.getJobListByNoteId(note.getId());
      Map<String, Object> response = new HashMap<>();
      response.put("lastResponseUnixTime", System.currentTimeMillis());
      response.put("jobs", notebookJobs);

      notebookServer.broadcast(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(),
          new Message(Message.OP.LIST_UPDATE_NOTE_JOBS).put("noteRunningJobs", response));
    }

    @Override
    public void onParagraphStatusChange(Paragraph p, Job.Status status) {
      Notebook notebook = notebookServer.notebook();
      List<Map<String, Object>> notebookJobs = notebook.getJobListByParagraphId(p.getId());

      Map<String, Object> response = new HashMap<>();
      response.put("lastResponseUnixTime", System.currentTimeMillis());
      response.put("jobs", notebookJobs);

      notebookServer.broadcast(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(),
          new Message(Message.OP.LIST_UPDATE_NOTE_JOBS).put("noteRunningJobs", response));
    }

    @Override
    public void onUnbindInterpreter(Note note, InterpreterSetting setting) {
      Notebook notebook = notebookServer.notebook();
      List<Map<String, Object>> notebookJobs = notebook.getJobListByNoteId(note.getId());
      Map<String, Object> response = new HashMap<>();
      response.put("lastResponseUnixTime", System.currentTimeMillis());
      response.put("jobs", notebookJobs);

      notebookServer.broadcast(NotebookServer.JOB_MANAGER_SERVICE.JOB_MANAGER_PAGE.getKey(),
          new Message(Message.OP.LIST_UPDATE_NOTE_JOBS).put("noteRunningJobs", response));
    }
  }

  /**
   * Need description here.
   */
  public static class ParagraphListenerImpl implements ParagraphJobListener {

    private NotebookServerImpl notebookServer;
    private Note note;

    public ParagraphListenerImpl(NotebookServerImpl notebookServer, Note note) {
      this.notebookServer = notebookServer;
      this.note = note;
    }

    @Override
    public void onProgressUpdate(Job job, int progress) {
      notebookServer.broadcast(note.getId(),
          new Message(Message.OP.PROGRESS).put("id", job.getId()).put("progress", progress));
    }

    @Override
    public void beforeStatusChange(Job job, Job.Status before, Job.Status after) {
    }

    @Override
    public void afterStatusChange(Job job, Job.Status before, Job.Status after) {
      if (after == Job.Status.ERROR) {
        if (job.getException() != null) {
          LOG.log(Level.INFO, "Error", job.getException());
        }
      }

      if (job.isTerminated()) {
        if (job.getStatus() == Job.Status.FINISHED) {
          LOG.log(Level.INFO, "Job {0} is finished successfully, status: {1}",
              new Object[]{job.getId(), job.getStatus()});
        } else {
          LOG.log(Level.SEVERE, "Job {0} is finished, status: {1}, exception: {2}, result: {3}",
              new Object[]{job.getId(), job.getStatus(), job.getException(),
                job.getReturn()});
        }

        try {
          //TODO(khalid): may change interface for JobListener and pass subject from interpreter
          note.persist(job instanceof Paragraph ? ((Paragraph) job).getAuthenticationInfo() : null);
        } catch (IOException e) {
          LOG.log(Level.SEVERE, e.toString(), e);
        }
      }
      if (job instanceof Paragraph) {
        Paragraph p = (Paragraph) job;
        p.setStatusToUserParagraph(job.getStatus());
        notebookServer.broadcastParagraph(note, p);
      }
      try {
        notebookServer.broadcastUpdateNoteJobInfo(System.currentTimeMillis() - 5000);
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "can not broadcast for job manager {}", e);
      }
    }

    /**
     * This callback is for paragraph that runs on RemoteInterpreterProcess
     */
    @Override
    public void onOutputAppend(Paragraph paragraph, int idx, String output) {
      Message msg = new Message(Message.OP.PARAGRAPH_APPEND_OUTPUT).put("noteId", paragraph.getNote().getId())
          .put("paragraphId", paragraph.getId()).put("data", output);

      notebookServer.broadcast(paragraph.getNote().getId(), msg);
    }

    /**
     * This callback is for paragraph that runs on RemoteInterpreterProcess
     */
    @Override
    public void onOutputUpdate(Paragraph paragraph, int idx, InterpreterResultMessage result) {
      String output = result.getData();
      Message msg = new Message(Message.OP.PARAGRAPH_UPDATE_OUTPUT).put("noteId", paragraph.getNote().getId())
          .put("paragraphId", paragraph.getId()).put("data", output);

      notebookServer.broadcast(paragraph.getNote().getId(), msg);
    }

    @Override
    public void onOutputUpdateAll(Paragraph paragraph, List<InterpreterResultMessage> msgs) {
      // TODO
    }
  }

  @Override
  public ParagraphJobListener getParagraphJobListener(Note note) {
    return new ParagraphListenerImpl(this, note);
  }

  public NotebookEventListener getNotebookInformationListener() {
    return new NotebookInformationListener(this);
  }

  private void sendAllAngularObjects(Note note, String user, Session conn)
      throws IOException {
    List<InterpreterSetting> settings = notebook().getInterpreterSettingManager().getInterpreterSettings(note.getId());
    if (settings == null || settings.size() == 0) {
      return;
    }

    for (InterpreterSetting intpSetting : settings) {
      AngularObjectRegistry registry = intpSetting.getInterpreterGroup(user, note.getId()).getAngularObjectRegistry();
      List<AngularObject> objects = registry.getAllWithGlobal(note.getId());
      for (AngularObject object : objects) {
        sendMsg(conn, serializeMessage(new Message(Message.OP.ANGULAR_OBJECT_UPDATE).put("angularObject", object)
            .put("interpreterGroupId", intpSetting.getInterpreterGroup(user, note.getId()).getId())
            .put("noteId", note.getId()).put("paragraphId", object.getParagraphId())));
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
      if (object.getNoteId() != null && !note.getId().equals(object.getNoteId())) {
        continue;
      }

      List<InterpreterSetting> intpSettings = notebook.getInterpreterSettingManager().getInterpreterSettings(note.
          getId());
      if (intpSettings.isEmpty()) {
        continue;
      }

      broadcast(note.getId(), new Message(Message.OP.ANGULAR_OBJECT_UPDATE).put("angularObject", object)
          .put("interpreterGroupId", interpreterGroupId).put("noteId", note.getId())
          .put("paragraphId", object.getParagraphId()));
    }
  }

  @Override
  public void onRemove(String interpreterGroupId, String name, String noteId, String paragraphId) {
    Notebook notebook = notebook();
    List<Note> notes = notebook.getAllNotes();
    for (Note note : notes) {
      if (noteId != null && !note.getId().equals(noteId)) {
        continue;
      }

      List<String> settingIds = notebook.getInterpreterSettingManager().getInterpreters(note.getId());
      for (String id : settingIds) {
        if (interpreterGroupId.contains(id)) {
          broadcast(note.getId(),
              new Message(Message.OP.ANGULAR_OBJECT_REMOVE).put("name", name).put("noteId", noteId)
              .put("paragraphId", paragraphId));
          break;
        }
      }
    }
  }

  public void getEditorSetting(Session conn, Message fromMessage) throws IOException {
    String paragraphId = (String) fromMessage.get("paragraphId");
    String replName = (String) fromMessage.get("magic");
    String noteId = getOpenNoteId(conn);
    String user = fromMessage.principal;
    Message resp = new Message(Message.OP.EDITOR_SETTING);
    resp.put("paragraphId", paragraphId);
    Interpreter interpreter = notebook().getInterpreterFactory().getInterpreter(user, noteId, replName);
    resp.put("editor", notebook().getInterpreterSettingManager().getEditorSetting(interpreter, user, noteId, replName));
    sendMsg(conn, serializeMessage(resp));
  }

  public void getInterpreterSettings(Session conn, AuthenticationInfo subject)
      throws IOException {
    List<InterpreterSetting> availableSettings = notebook().getInterpreterSettingManager().get();
    sendMsg(conn, serializeMessage(new Message(Message.OP.INTERPRETER_SETTINGS).put("interpreterSettings",
        availableSettings)));
  }

  @Override
  public void onMetaInfosReceived(String settingId, Map<String, String> metaInfos) {
    InterpreterSetting interpreterSetting = notebook().getInterpreterSettingManager().get(settingId);
    interpreterSetting.setInfos(metaInfos);
  }

  public void switchConnectionToWatcher(Session conn, Message messagereceived, String hdfsUsername,
      NotebookServerImplFactory notebookServerImplFactory)
      throws IOException {
    if (!isSessionAllowedToSwitchToWatcher(conn)) {
      LOG.log(Level.SEVERE, "Cannot switch this client to watcher, invalid security key");
      return;
    }
    LOG.log(Level.INFO, "Going to add {} to watcher socket", conn);
    // add the connection to the watcher.
    if (watcherSockets.contains(conn)) {
      LOG.info("connection alrerady present in the watcher");
      return;
    }
    watcherSockets.add(conn);

    // remove this connection from regular zeppelin ws usage.
    removeConnectionFromAllNote(conn);
    removeConnectedSockets(conn, notebookServerImplFactory);
    removeUserConnection(hdfsUsername, conn);
    removeUserConnection(project.getProjectGenericUser(), conn);
  }

  public synchronized void addConnectedSocket(Session conn) {
    connectedSockets.add(conn);
  }

  public synchronized void removeConnectedSockets(Session conn, NotebookServerImplFactory notebookServerImplFactory) {
    connectedSockets.remove(conn);
    if (connectedSockets.isEmpty()) {
      notebookServerImplFactory.removeNotebookServerImpl(this.project.getName());
    }
  }

  public synchronized boolean connectedSocketsIsEmpty() {
    return connectedSockets.isEmpty();
  }

  public boolean userConnectedSocketsContainsKey(String user) {
    return userConnectedSockets.containsKey(user);
  }

  public Queue<Session> getUserConnectedSocket(String user) {
    return userConnectedSockets.get(user);
  }

  public void putUserConnectedSocket(String user, Queue<Session> conn) {
    userConnectedSockets.put(user, conn);
  }

  public void removeUserConnection(String user, Session conn) {
    if (userConnectedSockets.containsKey(user)) {
      userConnectedSockets.get(user).remove(conn);
    } else {
      LOG.log(Level.SEVERE,
          "Closing connection that is absent in user connections");
    }
  }

  private boolean isSessionAllowedToSwitchToWatcher(Session session) {
    String watcherSecurityKey = (String) session.getUserProperties().get(WatcherSecurityKey.HTTP_HEADER);
    return !(StringUtils.isBlank(watcherSecurityKey) || !watcherSecurityKey
        .equals(WatcherSecurityKey.getKey()));
  }

  private void broadcastToWatchers(String noteId, String subject,
      Message message) {
    synchronized (watcherSockets) {
      if (watcherSockets.isEmpty()) {
        return;
      }
      for (Session watcher : watcherSockets) {
        try {
          sendMsg(watcher, WatcherMessage.builder(noteId).subject(subject).message(serializeMessage(message)).build().
              toJson());
        } catch (IOException e) {
          LOG.log(Level.SEVERE, "Cannot broadcast message to watcher", e);
        }
      }
    }
  }

  @Override
  public void onParaInfosReceived(String noteId, String paragraphId,
      String interpreterSettingId, Map<String, String> metaInfos) {
    Note note = notebook().getNote(noteId);
    if (note != null) {
      Paragraph paragraph = note.getParagraph(paragraphId);
      if (paragraph != null) {
        InterpreterSetting setting = notebook().getInterpreterSettingManager()
            .get(interpreterSettingId);
        setting.addNoteToPara(noteId, paragraphId);
        String label = metaInfos.get("label");
        String tooltip = metaInfos.get("tooltip");
        List<String> keysToRemove = Arrays.asList("noteId", "paraId", "label", "tooltip");
        for (String removeKey : keysToRemove) {
          metaInfos.remove(removeKey);
        }
        paragraph
            .updateRuntimeInfos(label, tooltip, metaInfos, setting.getGroup(), setting.getId());
        broadcast(
            note.getId(),
            new Message(Message.OP.PARAS_INFO).put("id", paragraphId).put("infos",
            paragraph.getRuntimeInfos()));
      }
    }
  }

  public void clearParagraphRuntimeInfo(InterpreterSetting setting) {
    Map<String, Set<String>> noteIdAndParaMap = setting.getNoteIdAndParaMap();
    if (noteIdAndParaMap != null && !noteIdAndParaMap.isEmpty()) {
      for (String noteId : noteIdAndParaMap.keySet()) {
        Set<String> paraIdSet = noteIdAndParaMap.get(noteId);
        if (paraIdSet != null && !paraIdSet.isEmpty()) {
          for (String paraId : paraIdSet) {
            Note note = notebook().getNote(noteId);
            if (note != null) {
              Paragraph paragraph = note.getParagraph(paraId);
              if (paragraph != null) {
                paragraph.clearRuntimeInfo(setting.getId());
                broadcast(noteId, new Message(Message.OP.PARAGRAPH).put("paragraph", paragraph));
              }
            }
          }
        }
      }
    }
    setting.clearNoteIdAndParaMap();
  }

  public void sendMsg(Session conn, String msg) throws IOException {
    if (conn == null || !conn.isOpen()) {
      LOG.log(Level.SEVERE, "Can't handle message. The connection has been closed.");
      return;
    }
    conn.getBasicRemote().sendText(msg);
  }

  protected String serializeMessage(Message m) {
    return gson.toJson(m);
  }

  public void closeConnections(NotebookServerImplFactory notebookServerImplFactory) {
    for (Map.Entry<String, Queue<Session>> entry : userConnectedSockets.entrySet()) {
      for (Session session : entry.getValue()) {
        closeConnection(session, entry.getKey(), notebookServerImplFactory);
      }
    }
  }

  public void closeConnection(Session session, String hdfsUsername,
      NotebookServerImplFactory notebookServerImplFactory) {
    try {
      if (session.isOpen()) {
        session.getBasicRemote().sendText("Restarting zeppelin.");
        session.close(new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART, "Restarting zeppelin."));
      }
      removeConnectionFromAllNote(session);
      removeConnectedSockets(session, notebookServerImplFactory);
      removeUserConnection(hdfsUsername, session);
      removeUserConnection(project.getProjectGenericUser(), session);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }
}
