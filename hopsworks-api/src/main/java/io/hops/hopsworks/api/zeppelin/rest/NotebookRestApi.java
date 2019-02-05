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

package io.hops.hopsworks.api.zeppelin.rest;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.enterprise.context.RequestScoped;

import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ZeppelinException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.common.collect.Sets;
import io.hops.hopsworks.api.zeppelin.rest.exception.BadRequestException;
import io.hops.hopsworks.api.zeppelin.rest.exception.ForbiddenException;
import io.hops.hopsworks.api.zeppelin.rest.exception.NotFoundException;
import io.hops.hopsworks.api.zeppelin.rest.message.CronRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.NewNoteRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.NewNotebookRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.NewParagraphRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.RunParagraphWithParametersRequest;
import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServerImpl;
import io.hops.hopsworks.api.zeppelin.types.InterpreterSettingsList;
import io.hops.hopsworks.api.zeppelin.util.InterpreterBindingUtils;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.project.Project;
import java.util.LinkedList;
import java.util.logging.Level;

import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.NotebookAuthorization;
import org.apache.zeppelin.search.SearchService;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.quartz.CronExpression;

/**
 * Rest api endpoint for the noteBook.
 */
@RequestScoped
public class NotebookRestApi {

  @EJB
  private DistributedFsService dfsService;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private Settings settings;
  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private HdfsUsersController hdfsUsersController;
  
  private static final Logger LOG = LoggerFactory.getLogger(
          NotebookRestApi.class);
  
  Gson gson = new Gson();
  private Notebook notebook;
  private NotebookServerImpl notebookServer;
  private SearchService noteSearchService;
  private NotebookAuthorization notebookAuthorization;
  private Project project;
  private String hdfsUserName;

  public NotebookRestApi() {
  }

  public void setParms(Project project, String hdfsUserName,
          ZeppelinConfig zeppelinConf) {
    this.project = project;
    this.hdfsUserName = hdfsUserName;
    this.notebook = zeppelinConf.getNotebook();
    this.notebookServer = zeppelinConf.getNotebookServer();
    this.noteSearchService = zeppelinConf.getNotebookIndex();
    if (this.notebook != null) {
      this.notebookAuthorization = this.notebook.getNotebookAuthorization();
    }
  }

  /**
   * get note authorization information
   *
   * @param noteId
   * @return
   */
  @GET
  @Path("{noteId}/permissions")
  public Response getNotePermissions(@PathParam("noteId") String noteId) throws IOException {

    checkIfUserIsAnon(getBlockNotAuthenticatedUserErrorMsg());
    checkIfUserCanRead(noteId,
            "Insufficient privileges you cannot get the list of permissions for this note");
    HashMap<String, Set<String>> permissionsMap = new HashMap<>();
    permissionsMap.put("owners", notebookAuthorization.getOwners(noteId));
    permissionsMap.put("readers", notebookAuthorization.getReaders(noteId));
    permissionsMap.put("writers", notebookAuthorization.getWriters(noteId));
    return new JsonResponse<>(Status.OK, "", permissionsMap).build();
  }

  private String ownerPermissionError(Set<String> current, Set<String> allowed) throws IOException {
    LOG.info("Cannot change permissions. Connection owners {}. Allowed owners {}",
            current.toString(), allowed.toString());
    return "Insufficient privileges to change permissions.\n\n"
            + "Allowed owners: " + allowed.toString() + "\n\n"
            + "User belongs to: " + current.toString();
  }

  private String getBlockNotAuthenticatedUserErrorMsg() throws IOException {
    return "Only authenticated user can set the permission.";
  }

  /**
   * Set of utils method to check if current user can perform action to the note.
   * Since we only have security on notebook level, from now we keep this logic in this class.
   * In the future we might want to generalize this for the rest of the api enmdpoints.
   */
  /**
   * Check if the current user is not authenticated(anonymous user) or not
   */
  private void checkIfUserIsAnon(String errorMsg) {
    boolean isAuthenticated = SecurityUtils.isAuthenticated();
    if (isAuthenticated && SecurityUtils.getPrincipal().equals("anonymous")) {
      LOG.info("Anonymous user cannot set any permissions for this note.");
      throw new ForbiddenException(errorMsg);
    }
  }

  /**
   * Check if the current user own the given note.
   */
  private void checkIfUserIsOwner(String noteId, String errorMsg) {
    Set<String> userAndRoles = Sets.newHashSet();
    userAndRoles.add(SecurityUtils.getPrincipal());
    userAndRoles.addAll(SecurityUtils.getRoles());
    if (!notebookAuthorization.isOwner(userAndRoles, noteId)) {
      throw new ForbiddenException(errorMsg);
    }
  }

  /**
   * Check if the current user is either Owner or Writer for the given note.
   */
  private void checkIfUserCanWrite(String noteId, String errorMsg) {
    Set<String> userAndRoles = Sets.newHashSet();
    userAndRoles.add(SecurityUtils.getPrincipal());
    userAndRoles.addAll(SecurityUtils.getRoles());
    if (!notebookAuthorization.hasWriteAuthorization(userAndRoles, noteId)) {
      throw new ForbiddenException(errorMsg);
    }
  }

  /**
   * Check if the current user can access (at least he have to be reader) the given note.
   */
  private void checkIfUserCanRead(String noteId, String errorMsg) {
    Set<String> userAndRoles = Sets.newHashSet();
    userAndRoles.add(SecurityUtils.getPrincipal());
    userAndRoles.addAll(SecurityUtils.getRoles());
    if (!notebookAuthorization.hasReadAuthorization(userAndRoles, noteId)) {
      throw new ForbiddenException(errorMsg);
    }
  }

  private void checkIfNoteIsNotNull(Note note) {
    if (note == null) {
      throw new NotFoundException("note not found");
    }
  }

  private void checkIfParagraphIsNotNull(Paragraph paragraph) {
    if (paragraph == null) {
      throw new NotFoundException("paragraph not found");
    }
  }

  /**
   * set note authorization information
   */
  @PUT
  @Path("{noteId}/permissions")
  public Response putNotePermissions(@PathParam("noteId") String noteId, String req)
          throws IOException {
    String principal = SecurityUtils.getPrincipal();
    HashSet<String> roles = SecurityUtils.getRoles();
    HashSet<String> userAndRoles = new HashSet<>();
    userAndRoles.add(principal);
    userAndRoles.addAll(roles);

    checkIfUserIsAnon(getBlockNotAuthenticatedUserErrorMsg());
    checkIfUserIsOwner(noteId,
            ownerPermissionError(userAndRoles, notebookAuthorization.getOwners(noteId)));

    HashMap<String, HashSet<String>> permMap = gson.fromJson(req,
            new TypeToken<HashMap<String, HashSet<String>>>() {}.getType());
    Note note = notebook.getNote(noteId);

    LOG.info("Set permissions {} {} {} {} {}", noteId, principal, permMap.get("owners"),
            permMap.get("readers"), permMap.get("writers"));

    HashSet<String> readers = permMap.get("readers");
    HashSet<String> owners = permMap.get("owners");
    HashSet<String> writers = permMap.get("writers");
    // Set readers, if writers and owners is empty -> set to user requesting the change
    if (readers != null && !readers.isEmpty()) {
      if (owners.isEmpty()) {
        owners = Sets.newHashSet(SecurityUtils.getPrincipal());
      }
    }
    // Set writers, if owners is empty -> set to user requesting the change
    if (writers != null && !writers.isEmpty()) {
      if (owners.isEmpty()) {
        owners = Sets.newHashSet(SecurityUtils.getPrincipal());
      }
    }

    notebookAuthorization.setReaders(noteId, readers);
    notebookAuthorization.setWriters(noteId, writers);
    notebookAuthorization.setOwners(noteId, owners);
    LOG.debug("After set permissions {} {} {}", notebookAuthorization.getOwners(noteId),
            notebookAuthorization.getReaders(noteId), notebookAuthorization.getWriters(noteId));
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    note.persist(subject);
    notebookServer.broadcastNote(note);
    notebookServer.broadcastNoteList(subject, userAndRoles);
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * bind a setting to note
   * <p/>
   * @param noteId
   * @param req
   * @return
   * @throws IOException
   */
  @PUT
  @Path("interpreter/bind/{noteId}")
  public Response bind(@PathParam("noteId") String noteId, String req) throws IOException {
    checkIfUserCanWrite(noteId,
            "Insufficient privileges you cannot bind any interpreters to this note");

    List<String> settingIdList = gson.fromJson(req, new TypeToken<List<String>>() {}.getType());
    notebook.bindInterpretersToNote(this.hdfsUserName, noteId, settingIdList);
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * list binded setting
   * <p/>
   * @param noteId
   * @return
   */
  @GET
  @Path("interpreter/bind/{noteId}")
  public Response bind(@PathParam("noteId") String noteId) {
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot get any interpreters settings");

    List<InterpreterSettingsList> settingList = InterpreterBindingUtils.
            getInterpreterBindings(notebook, noteId);
    notebookServer.broadcastInterpreterBindings(noteId, settingList);
    return new JsonResponse<>(Status.OK, "", settingList).build();
  }

  @GET
  public Response getNoteList() throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    HashSet<String> userAndRoles = SecurityUtils.getRoles();
    userAndRoles.add(subject.getUser());
    List<Map<String, String>> notesInfo = notebookServer.generateNotesInfo(false, subject,
            userAndRoles);
    return new JsonResponse<>(Status.OK, "", notesInfo).build();
  }

  @GET
  @Path("/reloadedNotebookList")
  public Response getReloadedNoteList() throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.
            getPrincipal());
    HashSet<String> userAndRoles = SecurityUtils.getRoles();
    userAndRoles.add(subject.getUser());
    List<Map<String, String>> notesInfo = notebookServer.
            generateNotesInfo(true, subject,
                    userAndRoles);
    return new JsonResponse<>(Status.OK, "", notesInfo).build();
  }

  @GET
  @Path("{noteId}")
  public Response getNote(@PathParam("noteId") String noteId) throws IOException {
    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot get this note");

    return new JsonResponse<>(Status.OK, "", note).build();
  }

  /**
   * export note REST API
   *
   * @param noteId ID of Note
   * @return note JSON with status.OK
   * @throws IOException
   */
  @GET
  @Path("export/{noteId}")
  public Response exportNote(@PathParam("noteId") String noteId) throws IOException {
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot export this note");
    String exportJson = notebook.exportNote(noteId);
    return new JsonResponse<>(Status.OK, "", exportJson).build();
  }

  /**
   * import new note REST API
   *
   * @param req - note Json
   * @return JSON with new note ID
   * @throws IOException
   */
  @POST
  @Path("import")
  public Response importNote(String req) throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    Note newNote = notebook.importNote(req, null, subject);
    return new JsonResponse<>(Status.OK, "", newNote.getId()).build();
  }

  /**
   * Create new note REST API
   *
   * @param message - JSON with new note name
   * @return JSON with new note ID
   * @throws IOException
   */
  @POST
  public Response createNote(String message) throws IOException {
    String user = SecurityUtils.getPrincipal();
    LOG.info("Create new note by JSON {}", message);
    NewNoteRequest request = NewNoteRequest.fromJson(message);
    AuthenticationInfo subject = new AuthenticationInfo(user);
    Note note = notebook.createNote(subject);
    if (request != null) {
      List<NewParagraphRequest> initialParagraphs = request.getParagraphs();
      if (initialParagraphs != null) {
        for (NewParagraphRequest paragraphRequest : initialParagraphs) {
          Paragraph p = note.addNewParagraph(subject);
          initParagraph(p, paragraphRequest, user);
        }
      }
    }
    note.addNewParagraph(subject); // add one paragraph to the last
    String noteName = request.getName();
    if (noteName.isEmpty()) {
      noteName = "Note " + note.getId();
    }

    note.setName(noteName);
    note.persist(subject);
    notebookServer.broadcastNote(note);
    notebookServer.broadcastNoteList(subject, SecurityUtils.getRoles());
    return new JsonResponse<>(Status.OK, "", note.getId()).build();
  }

  /**
   * Delete note REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException
   */
  @DELETE
  @Path("{noteId}")
  public Response deleteNote(@PathParam("noteId") String noteId) throws IOException {
    LOG.info("Delete note {} ", noteId);
    checkIfUserIsOwner(noteId, "Insufficient privileges you cannot delete this note");
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    if (!(noteId.isEmpty())) {
      Note note = notebook.getNote(noteId);
      if (note != null) {
        notebook.removeNote(noteId, subject);
      }
    }

    notebookServer.broadcastNoteList(subject, SecurityUtils.getRoles());
    return new JsonResponse<>(Status.OK, "").build();
  }

  /**
   * Clone note REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException, CloneNotSupportedException, IllegalArgumentException
   */
  @POST
  @Path("{noteId}")
  public Response cloneNote(@PathParam("noteId") String noteId, String message)
          throws IOException, CloneNotSupportedException, IllegalArgumentException {
    LOG.info("clone note by JSON {}", message);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot clone this note");
    NewNoteRequest request = NewNoteRequest.fromJson(message);
    String newNoteName = null;
    if (request != null) {
      newNoteName = request.getName();
    }
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    Note newNote = notebook.cloneNote(noteId, newNoteName, subject);
    notebookServer.broadcastNote(newNote);
    notebookServer.broadcastNoteList(subject, SecurityUtils.getRoles());
    return new JsonResponse<>(Status.OK, "", newNote.getId()).build();
  }

  /**
   * Insert paragraph REST API
   *
   * @param message - JSON containing paragraph's information
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{noteId}/paragraph")
  public Response insertParagraph(@PathParam("noteId") String noteId, String message)
          throws IOException {
    String user = SecurityUtils.getPrincipal();
    LOG.info("insert paragraph {} {}", noteId, message);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot add paragraph to this note");

    NewParagraphRequest request = NewParagraphRequest.fromJson(message);
    AuthenticationInfo subject = new AuthenticationInfo(user);
    Paragraph p;
    Double indexDouble = request.getIndex();
    if (indexDouble == null) {
      p = note.addNewParagraph(subject);
    } else {
      p = note.insertNewParagraph(indexDouble.intValue(), subject);
    }
    initParagraph(p, request, user);
    note.persist(subject);
    notebookServer.broadcastNote(note);
    return new JsonResponse<>(Status.OK, "", p.getId()).build();
  }

  /**
   * Get paragraph REST API
   *
   * @param noteId ID of Note
   * @return JSON with information of the paragraph
   * @throws IOException
   */
  @GET
  @Path("{noteId}/paragraph/{paragraphId}")
  public Response getParagraph(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId) throws IOException {
    LOG.info("get paragraph {} {}", noteId, paragraphId);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot get this paragraph");
    Paragraph p = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(p);

    return new JsonResponse<>(Status.OK, "", p).build();
  }

  @PUT
  @Path("{noteId}/paragraph/{paragraphId}/config")
  public Response updateParagraphConfig(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId, String message) throws IOException {
    String user = SecurityUtils.getPrincipal();
    LOG.info("{} will update paragraph config {} {}", user, noteId, paragraphId);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot update this paragraph config");
    Paragraph p = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(p);

    Map<String, Object> newConfig = gson.fromJson(message, HashMap.class);
    configureParagraph(p, newConfig, user);
    AuthenticationInfo subject = new AuthenticationInfo(user);
    note.persist(subject);

    return new JsonResponse<>(Status.OK, "", p).build();
  }

  /**
   * Move paragraph REST API
   *
   * @param newIndex - new index to move
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{noteId}/paragraph/{paragraphId}/move/{newIndex}")
  public Response moveParagraph(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId,
          @PathParam("newIndex") String newIndex)
          throws IOException {
    LOG.info("move paragraph {} {} {}", noteId, paragraphId, newIndex);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot move paragraph");

    Paragraph p = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(p);

    try {
      note.moveParagraph(paragraphId, Integer.parseInt(newIndex), true);

      AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
      note.persist(subject);
      notebookServer.broadcastNote(note);
      return new JsonResponse(Status.OK, "").build();
    } catch (IndexOutOfBoundsException e) {
      LOG.error("Exception in NotebookRestApi while moveParagraph ", e);
      return new JsonResponse(Status.BAD_REQUEST, "paragraph's new index is out of bound").build();
    }
  }

  /**
   * Delete paragraph REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException
   */
  @DELETE
  @Path("{noteId}/paragraph/{paragraphId}")
  public Response deleteParagraph(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId) throws IOException {
    LOG.info("delete paragraph {} {}", noteId, paragraphId);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanRead(noteId,
            "Insufficient privileges you cannot remove paragraph from this note");

    Paragraph p = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(p);

    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    note.removeParagraph(SecurityUtils.getPrincipal(), paragraphId);
    note.persist(subject);
    notebookServer.broadcastNote(note);

    return new JsonResponse(Status.OK, "").build();
  }

  /**
   * Clear result of all paragraphs REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.ok
   */
  @PUT
  @Path("{noteId}/clear")
  public Response clearAllParagraphOutput(@PathParam("noteId") String noteId)
          throws IOException {
    LOG.info("clear all paragraph output of note {}", noteId);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot clear this note");

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    note.clearAllParagraphOutput();

    return new JsonResponse(Status.OK, "").build();
  }
  
  private void materializeCertificates(List<Paragraph> paragraphs)
    throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      for (Paragraph p : paragraphs) {
        Interpreter interpreter = p.getCurrentRepl();
        if (null != interpreter) {
          String username = hdfsUsersController.getUserName(hdfsUserName);
          if (certificateMaterializer.openedInterpreter(project.getId())) {
            try {
              HopsUtils.materializeCertificatesForProject(project.getName(),
                  settings.getHdfsTmpCertDir(), dfso, certificateMaterializer, settings);
            } catch (IOException ex) {
              LOG.warn("Could not materialize certificates for user: " +
                  project.getName() + "__" + username);
              certificateMaterializer.closedInterpreter(project.getId());
              HopsUtils.cleanupCertificatesForProject(project.getName(),
                  settings.getHdfsTmpCertDir(), certificateMaterializer, settings);
              throw ex;
            }
          }
        }
      }
    } finally {
      if (null != dfso) {
        dfso.close();
      }
    }
  }
  
  /**
   * Run note jobs REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{noteId}")
  public Response runNoteJobs(@PathParam("noteId") String noteId)
          throws IOException, IllegalArgumentException {
    LOG.info("run note jobs {} ", noteId);
    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot run job for this note");
    
    materializeCertificates(note.getParagraphs());
  
    try {
      note.runAll();
    } catch (Exception ex) {
      LOG.error("Exception from run", ex);
      return new JsonResponse<>(Status.PRECONDITION_FAILED,
              ex.getMessage() + "- Not selected or Invalid Interpreter bind").build();
    }
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Stop(delete) note jobs REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{noteId}")
  public Response stopNoteJobs(@PathParam("noteId") String noteId)
          throws IOException, IllegalArgumentException {
    LOG.info("stop note jobs {} ", noteId);
    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot stop this job for this note");

    for (Paragraph p : note.getParagraphs()) {
      if (!p.isTerminated()) {
        p.abort();
      }
    }
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Get note job status REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("job/{noteId}")
  public Response getNoteJobStatus(@PathParam("noteId") String noteId)
          throws IOException, IllegalArgumentException {
    LOG.info("get note job status.");
    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot get job status");

    return new JsonResponse<>(Status.OK, null, note.generateParagraphsInfo()).build();
  }

  /**
   * Get note paragraph job status REST API
   *
   * @param noteId ID of Note
   * @param paragraphId ID of Paragraph
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("job/{noteId}/{paragraphId}")
  public Response getNoteParagraphJobStatus(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId)
          throws IOException, IllegalArgumentException {
    LOG.info("get note paragraph job status.");
    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot get job status");

    Paragraph paragraph = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(paragraph);

    return new JsonResponse<>(Status.OK, null, note.generateSingleParagraphInfo(paragraphId)).
            build();
  }

  /**
   * Run asynchronously paragraph job REST API
   *
   * @param message - JSON with params if user wants to update dynamic form's
   * value
   * null, empty string, empty json if user doesn't want to update
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{noteId}/{paragraphId}")
  public Response runParagraph(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId, String message)
          throws IOException, IllegalArgumentException {
    LOG.info("run paragraph job asynchronously {} {} {}", noteId, paragraphId, message);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot run job for this note");
    Paragraph paragraph = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(paragraph);

    // handle params if presented
    handleParagraphParams(message, note, paragraph);

    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());

    paragraph.setAuthenticationInfo(subject);
    note.persist(subject);

    note.run(paragraph.getId());
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Run synchronously a paragraph REST API
   *
   * @param noteId - noteId
   * @param paragraphId - paragraphId
   * @param message - JSON with params if user wants to update dynamic form's value
   * null, empty string, empty json if user doesn't want to update
   *
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("run/{noteId}/{paragraphId}")
  public Response runParagraphSynchronously(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId,
          String message) throws
          IOException, IllegalArgumentException {
    LOG.info("run paragraph synchronously {} {} {}", noteId, paragraphId, message);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot run paragraph");
    Paragraph paragraph = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(paragraph);

    // handle params if presented
    handleParagraphParams(message, note, paragraph);

    if (paragraph.getListener() == null) {
      note.initializeJobListenerForParagraph(paragraph);
    }

    paragraph.run();

    final InterpreterResult result = paragraph.getResult();

    if (result.code() == InterpreterResult.Code.SUCCESS) {
      return new JsonResponse<>(Status.OK, result).build();
    } else {
      return new JsonResponse<>(Status.INTERNAL_SERVER_ERROR, result).build();
    }
  }

  /**
   * Stop(delete) paragraph job REST API
   *
   * @param noteId ID of Note
   * @param paragraphId ID of Paragraph
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{noteId}/{paragraphId}")
  public Response stopParagraph(@PathParam("noteId") String noteId,
          @PathParam("paragraphId") String paragraphId) throws IOException, IllegalArgumentException {
    LOG.info("stop paragraph job {} ", noteId);
    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot stop paragraph");
    Paragraph p = note.getParagraph(paragraphId);
    checkIfParagraphIsNotNull(p);
    p.abort();
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Register cron job REST API
   *
   * @param message - JSON with cron expressions.
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("cron/{noteId}")
  public Response registerCronJob(@PathParam("noteId") String noteId, String message)
          throws IOException, IllegalArgumentException {
    LOG.info("Register cron job note={} request cron msg={}", noteId, message);

    CronRequest request = CronRequest.fromJson(message);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanWrite(noteId, "Insufficient privileges you cannot set a cron job for this note");

    if (!CronExpression.isValidExpression(request.getCronString())) {
      return new JsonResponse<>(Status.BAD_REQUEST, "wrong cron expressions.").build();
    }

    Map<String, Object> config = note.getConfig();
    config.put("cron", request.getCronString());
    note.setConfig(config);
    notebook.refreshCron(note.getId());

    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Remove cron job REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("cron/{noteId}")
  public Response removeCronJob(@PathParam("noteId") String noteId)
          throws IOException, IllegalArgumentException {
    LOG.info("Remove cron job note {}", noteId);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserIsOwner(noteId,
            "Insufficient privileges you cannot remove this cron job from this note");

    Map<String, Object> config = note.getConfig();
    config.put("cron", null);
    note.setConfig(config);
    notebook.refreshCron(note.getId());

    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Get cron job REST API
   *
   * @param noteId ID of Note
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("cron/{noteId}")
  public Response getCronJob(@PathParam("noteId") String noteId)
          throws IOException, IllegalArgumentException {
    LOG.info("Get cron job note {}", noteId);

    Note note = notebook.getNote(noteId);
    checkIfNoteIsNotNull(note);
    checkIfUserCanRead(noteId, "Insufficient privileges you cannot get cron information");

    return new JsonResponse<>(Status.OK, note.getConfig().get("cron")).build();
  }

  /**
   * Get note jobs for job manager
   *
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("jobmanager/")
  public Response getJobListforNote() throws IOException, IllegalArgumentException {
    LOG.info("Get note jobs for job manager");

    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    List<Map<String, Object>> noteJobs = notebook
            .getJobListByUnixTime(false, 0, subject);
    Map<String, Object> response = new HashMap<>();

    response.put("lastResponseUnixTime", System.currentTimeMillis());
    response.put("jobs", noteJobs);

    return new JsonResponse<>(Status.OK, response).build();
  }

  /**
   * Get updated note jobs for job manager
   * <p>
   * Return the `Note` change information within the post unix timestamp.
   *
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("jobmanager/{lastUpdateUnixtime}/")
  public Response getUpdatedJobListforNote(
          @PathParam("lastUpdateUnixtime") long lastUpdateUnixTime)
          throws IOException, IllegalArgumentException {
    LOG.info("Get updated note jobs lastUpdateTime {}", lastUpdateUnixTime);

    List<Map<String, Object>> noteJobs;
    AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
    noteJobs = notebook.getJobListByUnixTime(false, lastUpdateUnixTime, subject);
    Map<String, Object> response = new HashMap<>();

    response.put("lastResponseUnixTime", System.currentTimeMillis());
    response.put("jobs", noteJobs);

    return new JsonResponse<>(Status.OK, response).build();
  }

  /**
   * Search for a Notes with permissions
   */
  @GET
  @Path("search")
  public Response search(@QueryParam("q") String queryTerm) {
    LOG.info("Searching notes for: {}", queryTerm);
    String principal = SecurityUtils.getPrincipal();
    HashSet<String> roles = SecurityUtils.getRoles();
    HashSet<String> userAndRoles = new HashSet<>();
    userAndRoles.add(principal);
    userAndRoles.addAll(roles);
    List<Map<String, String>> notesFound = noteSearchService.query(queryTerm);
    for (int i = 0; i < notesFound.size(); i++) {
      String[] Id = notesFound.get(i).get("id").split("/", 2);
      String noteId = Id[0];
      if (!notebookAuthorization.isOwner(noteId, userAndRoles) && !notebookAuthorization.isReader(noteId, userAndRoles)
              && !notebookAuthorization.isWriter(noteId, userAndRoles)) {
        notesFound.remove(i);
        i--;
      }
    }
    LOG.info("{} notes found", notesFound.size());
    return new JsonResponse<>(Status.OK, notesFound).build();
  }

  private void handleParagraphParams(String message, Note note, Paragraph paragraph)
          throws IOException {
    // handle params if presented
    if (!StringUtils.isEmpty(message)) {
      RunParagraphWithParametersRequest request = RunParagraphWithParametersRequest.fromJson(message);
      Map<String, Object> paramsForUpdating = request.getParams();
      if (paramsForUpdating != null) {
        paragraph.settings.getParams().putAll(paramsForUpdating);
        AuthenticationInfo subject = new AuthenticationInfo(SecurityUtils.getPrincipal());
        note.persist(subject);
      }
    }
  }

  private void initParagraph(Paragraph p, NewParagraphRequest request, String user)
          throws IOException {
    LOG.info("Init Paragraph for user {}", user);
    checkIfParagraphIsNotNull(p);
    p.setTitle(request.getTitle());
    p.setText(request.getText());
    Map< String, Object> config = request.getConfig();
    if (config != null && !config.isEmpty()) {
      configureParagraph(p, config, user);
    }
  }

  private void configureParagraph(Paragraph p, Map< String, Object> newConfig, String user)
          throws IOException {
    LOG.info("Configure Paragraph for user {}", user);
    if (newConfig == null || newConfig.isEmpty()) {
      LOG.warn("{} is trying to update paragraph {} of note {} with empty config",
              user, p.getId(), p.getNote().getId());
      throw new BadRequestException("paragraph config cannot be empty");
    }
    Map<String, Object> origConfig = p.getConfig();
    for (final Map.Entry<String, Object> entry : newConfig.entrySet()) {
      origConfig.put(entry.getKey(), entry.getValue());
    }

    p.setConfig(origConfig);
  }

  /**
   * Create new note in a project
   * <p/>
   * @param newNote
   * @return note info if successful.
   */
  @POST
  @Path("/new")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response createNew(NewNotebookRequest newNote) throws ZeppelinException {
    Note note;
    NoteInfo noteInfo;
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    try {
      String defaultInterpreterId = newNote.getDefaultInterpreterId();
      if (defaultInterpreterId != null && !defaultInterpreterId.isEmpty()) {
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
      String noteName = newNote.getName();
      if (noteName == null || noteName.isEmpty()) {
        noteName = "Note " + note.getId();
      }
      note.setName(noteName);
      Set<String> owners = notebook.getNotebookAuthorization().getOwners(note
          .getId());
      String projectGenericUser = hdfsUsersController.getProjectName(hdfsUserName)
          + Settings.PROJECT_GENERIC_USER_SUFFIX;
      owners.add(projectGenericUser);
      notebook.getNotebookAuthorization().setOwners(note.getId(), owners);
      
      note.persist(subject);
      noteInfo = new NoteInfo(note);
      zeppelinResource.persistToDB(this.project);
    } catch (IOException ex) {
      throw new ZeppelinException(RESTCodes.ZeppelinErrorCode.CREATE_NOTEBOOK_ERROR, Level.SEVERE, null,
        ex.getMessage(),ex);
    }
    notebookServer.broadcastNote(note);
    notebookServer.broadcastNoteList(subject, SecurityUtils.getRoles());
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }
}
