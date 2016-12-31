package io.hops.hopsworks.api.zeppelin.rest;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hops.hopsworks.api.filter.AllowedRoles;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.hops.hopsworks.api.zeppelin.rest.message.CronRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import io.hops.hopsworks.api.zeppelin.rest.message.NewNotebookRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.NewParagraphRequest;
import io.hops.hopsworks.api.zeppelin.rest.message.RunParagraphWithParametersRequest;
import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.socket.NotebookServer;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.AppException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
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

  private static final Logger LOG = LoggerFactory.getLogger(
          NotebookRestApi.class);
  Gson gson = new Gson();
  private Notebook notebook;
  private NotebookServer notebookServer;
  private SearchService notebookIndex;
  private NotebookAuthorization notebookAuthorization;
  private Project project;
  private ZeppelinConfig zeppelinConf;
  private String roleInProject;
  private String hdfsUserName;

  public NotebookRestApi() {
  }

  public void setParms(Project project, String userRole, String hdfsUserName,
          ZeppelinConfig zeppelinConf) {
    this.project = project;
    this.zeppelinConf = zeppelinConf;
    this.hdfsUserName = hdfsUserName;
    this.roleInProject = userRole;
    this.notebook = zeppelinConf.getNotebook();
    this.notebookServer = zeppelinConf.getNotebookServer();
    this.notebookIndex = zeppelinConf.getNotebookIndex();
    this.notebookAuthorization = this.notebook.getNotebookAuthorization();
  }

  /**
   * get note authorization information
   *
   * @param noteId
   * @return
   */
  @GET
  @Path("{noteId}/permissions")
  public Response getNotePermissions(@PathParam("noteId") String noteId) {
    Note note = zeppelinConf.getNotebook().getNote(noteId);
    HashMap<String, Set<String>> permissionsMap = new HashMap();
    permissionsMap.put("owners", notebookAuthorization.getOwners(noteId));
    permissionsMap.put("readers", notebookAuthorization.getReaders(noteId));
    permissionsMap.put("writers", notebookAuthorization.getWriters(noteId));
    return new JsonResponse<>(Status.OK, "", permissionsMap).build();
  }

  String ownerPermissionError(Set<String> current,
          Set<String> allowed) throws IOException {
    LOG.info(
            "Cannot change permissions. Connection owners {}. Allowed owners {}",
            current.toString(), allowed.toString());
    return "Insufficient privileges to change permissions.\n\n"
            + "Allowed owners: " + allowed.toString() + "\n\n"
            + "User belongs to: " + current.toString();
  }

  /**
   * set note authorization information
   *
   * @param noteId
   * @param req
   * @return
   * @throws java.io.IOException
   */
  @PUT
  @Path("{noteId}/permissions")
  public Response putNotePermissions(@PathParam("noteId") String noteId,
          String req)
          throws IOException {
    HashMap<String, HashSet> permMap = gson.fromJson(req,
            new TypeToken<HashMap<String, HashSet>>() {}.getType());
    Note note = notebook.getNote(noteId);
    String principal = SecurityUtils.getPrincipal();
    HashSet<String> roles = SecurityUtils.getRoles();
    LOG.info("Set permissions {} {} {} {} {}",
            noteId,
            principal,
            permMap.get("owners"),
            permMap.get("readers"),
            permMap.get("writers")
    );

    HashSet<String> userAndRoles = new HashSet<>();
    userAndRoles.add(principal);
    userAndRoles.addAll(roles);
    if (!notebookAuthorization.isOwner(noteId, userAndRoles)) {
      return new JsonResponse<>(Status.FORBIDDEN, ownerPermissionError(
              userAndRoles,
              notebookAuthorization.getOwners(noteId))).build();
    }

    HashSet readers = permMap.get("readers");
    HashSet owners = permMap.get("owners");
    HashSet writers = permMap.get("writers");
    // Set readers, if writers and owners is empty -> set to user requesting the change
    if (readers != null && !readers.isEmpty()) {
      if (writers.isEmpty()) {
        writers = Sets.newHashSet(SecurityUtils.getPrincipal());
      }
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
    LOG.debug("After set permissions {} {} {}",
            notebookAuthorization.getOwners(noteId),
            notebookAuthorization.getReaders(noteId),
            notebookAuthorization.getWriters(noteId));
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    note.persist(subject);
    notebookServer.broadcastNote(note);
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
  public Response bind(@PathParam("noteId") String noteId, String req) throws
          IOException {
    List<String> settingIdList = gson.fromJson(req,
            new TypeToken<List<String>>() {}.getType());
    notebook.bindInterpretersToNote(noteId, settingIdList);
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
    List<InterpreterSettingListForNoteBind> settingList;
    settingList = new LinkedList<>();

    List<InterpreterSetting> selectedSettings = notebook.
            getBindedInterpreterSettings(noteId);
    for (InterpreterSetting setting : selectedSettings) {
      settingList.add(new InterpreterSettingListForNoteBind(
              setting.id(),
              setting.getName(),
              setting.getGroup(),
              setting.getInterpreterInfos(),
              true)
      );
    }

    List<InterpreterSetting> availableSettings = notebook.
            getInterpreterFactory().get();
    for (InterpreterSetting setting : availableSettings) {
      boolean selected = false;
      for (InterpreterSetting selectedSetting : selectedSettings) {
        if (selectedSetting.id().equals(setting.id())) {
          selected = true;
          break;
        }
      }

      if (!selected) {
        settingList.add(new InterpreterSettingListForNoteBind(
                setting.id(),
                setting.getName(),
                setting.getGroup(),
                setting.getInterpreterInfos(),
                false)
        );
      }
    }
    return new JsonResponse<>(Status.OK, "", settingList).build();
  }

  @GET
  public Response getNotebookList() throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    List<Map<String, String>> notesInfo = notebookServer.generateNotebooksInfo(
            false, subject);
    return new JsonResponse<>(Status.OK, "", notesInfo).build();
  }

  @GET
  @Path("/reloadedNotebookList")
  public Response getReloadedNotebookList() throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    List<Map<String, String>> notesInfo = notebookServer.generateNotebooksInfo(
            true, subject);
    return new JsonResponse<>(Status.OK, "", notesInfo).build();
  }

  @GET
  @Path("{notebookId}")
  public Response getNotebook(@PathParam("notebookId") String notebookId) throws
          IOException {
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, "", note).build();
  }

  /**
   * export note REST API
   *
   * @param noteId
   * @return note JSON with status.OK
   * @throws IOException
   */
  @GET
  @Path("export/{id}")
  public Response exportNoteBook(@PathParam("id") String noteId) throws
          IOException {
    String exportJson = notebook.exportNote(noteId);
    return new JsonResponse(Status.OK, "", exportJson).build();
  }

  /**
   * import new note REST API
   *
   * @param req - notebook Json
   * @return JSON with new note ID
   * @throws IOException
   */
  @POST
  @Path("import")
  public Response importNotebook(String req) throws IOException {
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    Note newNote = notebook.importNote(req, null, subject);
    return new JsonResponse<>(Status.CREATED, "", newNote.getId()).build();
  }

  /**
   * Create new note REST API
   * <p>
   * @param message - JSON with new note name
   * @return JSON with new note ID
   * @throws IOException
   */
  @POST
  public Response createNote(String message) throws IOException {
    LOG.info("Create new notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    Note note = notebook.createNote(subject);
    List<NewParagraphRequest> initialParagraphs = request.getParagraphs();
    if (initialParagraphs != null) {
      for (NewParagraphRequest paragraphRequest : initialParagraphs) {
        Paragraph p = note.addParagraph();
        p.setTitle(paragraphRequest.getTitle());
        p.setText(paragraphRequest.getText());
      }
    }
    note.addParagraph(); // add one paragraph to the last
    String noteName = request.getName();
    if (noteName.isEmpty()) {
      noteName = "Note " + note.getId();
    }

    note.setName(noteName);
    note.persist(subject);
    notebookServer.broadcastNote(note);
    notebookServer.broadcastNoteList(subject);
    return new JsonResponse<>(Status.CREATED, "", note.getId()).build();
  }

  /**
   * Delete note REST API
   * <p>
   * @param notebookId
   * @return
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}")
  public Response deleteNote(@PathParam("notebookId") String notebookId) throws
          IOException {
    LOG.info("Delete notebook {} ", notebookId);
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    if (!(notebookId.isEmpty())) {
      Note note = notebook.getNote(notebookId);
      if (note != null) {
        notebook.removeNote(notebookId, subject);
      }

      notebook.reloadAllNotes(subject);
      note = notebook.getNote(notebookId);
      if (note != null) {
        return new JsonResponse<>(Status.BAD_REQUEST,
                "Could not delete notebook, check your permission.").build();
      }
    }

    notebookServer.broadcastNoteList(subject);
    return new JsonResponse<>(Status.OK, "").build();
  }

  /**
   * Clone note REST API@return JSON with status.CREATED
   * <p>
   * @param notebookId
   * @param message
   * @return
   * @throws IOException
   * @throws java.lang.CloneNotSupportedException
   */
  @POST
  @Path("{notebookId}")
  public Response cloneNote(@PathParam("notebookId") String notebookId,
          String message) throws
          IOException, CloneNotSupportedException, IllegalArgumentException {
    LOG.info("clone notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    String newNoteName = request.getName();
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    Note newNote = notebook.cloneNote(notebookId, newNoteName, subject);
    notebookServer.broadcastNote(newNote);
    notebookServer.broadcastNoteList(subject);
    return new JsonResponse<>(Status.CREATED, "", newNote.getId()).build();
  }

  /**
   * Insert paragraph REST API
   *
   * @param notebookId
   * @param message - JSON containing paragraph's information
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{notebookId}/paragraph")
  public Response insertParagraph(@PathParam("notebookId") String notebookId,
          String message)
          throws IOException {
    LOG.info("insert paragraph {} {}", notebookId, message);

    Note note = zeppelinConf.getNotebook().getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    NewParagraphRequest request = gson.fromJson(message,
            NewParagraphRequest.class);

    Paragraph p;
    Double indexDouble = request.getIndex();
    if (indexDouble == null) {
      p = note.addParagraph();
    } else {
      p = note.insertParagraph(indexDouble.intValue());
    }
    p.setTitle(request.getTitle());
    p.setText(request.getText());

    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    note.persist(subject);
    notebookServer.broadcastNote(note);
    return new JsonResponse(Status.CREATED, "", p.getId()).build();
  }

  /**
   * Get paragraph REST API
   *
   * @param notebookId
   * @param paragraphId@return JSON with information of the paragraph
   * @throws IOException
   */
  @GET
  @Path("{notebookId}/paragraph/{paragraphId}")
  public Response getParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId) throws IOException {
    LOG.info("get paragraph {} {}", notebookId, paragraphId);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse(Status.NOT_FOUND, "paragraph not found.").build();
    }

    return new JsonResponse(Status.OK, "", p).build();
  }

  /**
   * Move paragraph REST API
   *
   * @param notebookId
   * @param newIndex - new index to move
   * @param paragraphId
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{notebookId}/paragraph/{paragraphId}/move/{newIndex}")
  public Response moveParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          @PathParam("newIndex") String newIndex) throws IOException {
    LOG.info("move paragraph {} {} {}", notebookId, paragraphId, newIndex);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse(Status.NOT_FOUND, "paragraph not found.").build();
    }

    try {
      note.moveParagraph(paragraphId, Integer.parseInt(newIndex), true);

      AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
      note.persist(subject);
      notebookServer.broadcastNote(note);
      return new JsonResponse(Status.OK, "").build();
    } catch (IndexOutOfBoundsException e) {
      LOG.error("Exception in NotebookRestApi while moveParagraph ", e);
      return new JsonResponse(Status.BAD_REQUEST,
              "paragraph's new index is out of bound").build();
    }
  }

  /**
   * Delete paragraph REST API
   *
   * @param notebookId
   * @param paragraphId@return JSON with status.OK
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}/paragraph/{paragraphId}")
  public Response deleteParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId) throws IOException {
    LOG.info("delete paragraph {} {}", notebookId, paragraphId);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse(Status.NOT_FOUND, "paragraph not found.").build();
    }

    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    note.removeParagraph(paragraphId);
    note.persist(subject);
    notebookServer.broadcastNote(note);

    return new JsonResponse(Status.OK, "").build();
  }

  /**
   * Run notebook jobs REST API
   *
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{notebookId}")
  public Response runNoteJobs(@PathParam("notebookId") String notebookId) throws
          IOException, IllegalArgumentException {
    LOG.info("run notebook jobs {} ", notebookId);
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    note.runAll();
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Stop(delete) notebook jobs REST API
   *
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{notebookId}")
  public Response stopNoteJobs(@PathParam("notebookId") String notebookId)
          throws
          IOException, IllegalArgumentException {
    LOG.info("stop notebook jobs {} ", notebookId);
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    for (Paragraph p : note.getParagraphs()) {
      if (!p.isTerminated()) {
        p.abort();
      }
    }
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Get notebook job status REST API
   *
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("job/{notebookId}")
  public Response getNoteJobStatus(@PathParam("notebookId") String notebookId)
          throws IOException, IllegalArgumentException {
    LOG.info("get notebook job status.");
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, null, note.generateParagraphsInfo()).
            build();
  }

  /**
   * Run paragraph job REST API
   *
   * @param notebookId
   * @param message - JSON with params if user wants to update dynamic form's
   * value
   * null, empty string, empty json if user doesn't want to update
   * @param paragraphId
   *
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{notebookId}/{paragraphId}")
  public Response runParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          String message) throws
          IOException, IllegalArgumentException {
    LOG.info("run paragraph job {} {} {}", notebookId, paragraphId, message);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph paragraph = note.getParagraph(paragraphId);
    if (paragraph == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "paragraph not found.").
              build();
    }

    // handle params if presented
    if (!StringUtils.isEmpty(message)) {
      RunParagraphWithParametersRequest request = gson.fromJson(message,
              RunParagraphWithParametersRequest.class);
      Map<String, Object> paramsForUpdating = request.getParams();
      if (paramsForUpdating != null) {
        paragraph.settings.getParams().putAll(paramsForUpdating);
        AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
        note.persist(subject);
      }
    }

    note.run(paragraph.getId());
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Stop(delete) paragraph job REST API
   *
   * @param notebookId
   * @param paragraphId@return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{notebookId}/{paragraphId}")
  public Response stopParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId) throws
          IOException, IllegalArgumentException {
    LOG.info("stop paragraph job {} ", notebookId);
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "paragraph not found.").
              build();
    }
    p.abort();
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Register cron job REST API
   *
   * @param notebookId
   * @param message - JSON with cron expressions.
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("cron/{notebookId}")
  public Response registerCronJob(@PathParam("notebookId") String notebookId,
          String message) throws
          IOException, IllegalArgumentException {
    LOG.info("Register cron job note={} request cron msg={}", notebookId,
            message);

    CronRequest request = gson.fromJson(message,
            CronRequest.class);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    if (!CronExpression.isValidExpression(request.getCronString())) {
      return new JsonResponse<>(Status.BAD_REQUEST, "wrong cron expressions.").
              build();
    }

    Map<String, Object> config = note.getConfig();
    config.put("cron", request.getCronString());
    note.setConfig(config);
    notebook.refreshCron(note.id());

    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Remove cron job REST API
   *
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("cron/{notebookId}")
  public Response removeCronJob(@PathParam("notebookId") String notebookId)
          throws
          IOException, IllegalArgumentException {
    LOG.info("Remove cron job note {}", notebookId);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    Map<String, Object> config = note.getConfig();
    config.put("cron", null);
    note.setConfig(config);
    notebook.refreshCron(note.id());

    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Get cron job REST API
   *
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("cron/{notebookId}")
  public Response getCronJob(@PathParam("notebookId") String notebookId) throws
          IOException, IllegalArgumentException {
    LOG.info("Get cron job note {}", notebookId);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, note.getConfig().get("cron")).build();
  }

  /**
   * Get notebook jobs for job manager
   *
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("jobmanager/")
  public Response getJobListforNotebook() throws IOException,
          IllegalArgumentException {
    LOG.info("Get notebook jobs for job manager");

    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    List<Map<String, Object>> notebookJobs = notebook.getJobListforNotebook(
            false, 0, subject);
    Map<String, Object> response = new HashMap<>();

    response.put("lastResponseUnixTime", System.currentTimeMillis());
    response.put("jobs", notebookJobs);

    return new JsonResponse<>(Status.OK, response).build();
  }

  /**
   * Get updated notebook jobs for job manager
   *
   * @param lastUpdateUnixTime
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("jobmanager/{lastUpdateUnixtime}/")
  public Response getUpdatedJobListforNotebook(
          @PathParam("lastUpdateUnixtime") long lastUpdateUnixTime) throws
          IOException, IllegalArgumentException {
    LOG.info("Get updated notebook jobs lastUpdateTime {}", lastUpdateUnixTime);

    List<Map<String, Object>> notebookJobs;
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    notebookJobs = notebook.getJobListforNotebook(false, lastUpdateUnixTime,
            subject);
    Map<String, Object> response = new HashMap<>();

    response.put("lastResponseUnixTime", System.currentTimeMillis());
    response.put("jobs", notebookJobs);

    return new JsonResponse<>(Status.OK, response).build();
  }

  /**
   * Search for a Notes with permissions
   *
   * @param queryTerm
   * @return
   */
  @GET
  @Path("search")
  public Response search(@QueryParam("q") String queryTerm) {
    LOG.info("Searching notebooks for: {}", queryTerm);
    String principal = SecurityUtils.getPrincipal();
    HashSet<String> roles = SecurityUtils.getRoles();
    HashSet<String> userAndRoles = new HashSet<>();
    userAndRoles.add(principal);
    userAndRoles.addAll(roles);
    List<Map<String, String>> notebooksFound = notebookIndex.query(queryTerm);
    for (int i = 0; i < notebooksFound.size(); i++) {
      String[] Id = notebooksFound.get(i).get("id").split("/", 2);
      String noteId = Id[0];
      if (!notebookAuthorization.isOwner(noteId, userAndRoles)
              && !notebookAuthorization.isReader(noteId, userAndRoles)
              && !notebookAuthorization.isWriter(noteId, userAndRoles)) {
        notebooksFound.remove(i);
        i--;
      }
    }
    LOG.info("{} notebooks found", notebooksFound.size());
    return new JsonResponse<>(Status.OK, notebooksFound).build();
  }

  /**
   * Create new note in a project
   * <p/>
   * @param newNote
   * @return note info if successful.
   * @throws AppException
   */
  @POST
  @Path("/new")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createNew(NewNotebookRequest newNote) throws
          AppException {
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    Note note;
    NoteInfo noteInfo;
    AuthenticationInfo subject = new AuthenticationInfo(this.hdfsUserName);
    try {
      note = notebook.createNote(subject);
      note.addParagraph(); // it's an empty note. so add one paragraph
      String noteName = newNote.getName();
      if (noteName == null || noteName.isEmpty()) {
        noteName = "Note " + note.getId();
      }
      note.setName(noteName);
      note.persist(subject);
      noteInfo = new NoteInfo(note);
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create notebook" + ex.getMessage());
    }
    notebookServer.broadcastNote(note);
    notebookServer.broadcastNoteList(subject);
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }
}
