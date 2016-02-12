package se.kth.hopsworks.zeppelin.rest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.search.SearchService;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.rest.message.CronRequest;
import se.kth.hopsworks.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import se.kth.hopsworks.zeppelin.rest.message.NewNotebookRequest;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;
import se.kth.hopsworks.zeppelin.rest.message.NewParagraphRequest;
import se.kth.hopsworks.zeppelin.rest.message.RunParagraphWithParametersRequest;
import se.kth.hopsworks.zeppelin.util.ZeppelinResource;

/**
 * Rest api endpoint for the noteBook.
 */
@Path("/notebook")
@Produces("application/json")
public class NotebookRestApi {

  Logger logger = LoggerFactory.getLogger(NotebookRestApi.class);
  Gson gson = new Gson();
  @EJB
  private ProjectController projectController;
  @EJB
  private ZeppelinResource zeppelinResource;
  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;
  private final Notebook notebook;
  private NotebookRepo notebookRepo;
  private SearchService notebookIndex;

  public NotebookRestApi() {
    this.notebook = new Notebook();
    this.notebookIndex = zeppelin.getNotebookIndex();
  }

  public NotebookRestApi(Notebook notebook, SearchService search) {
    this.notebook = notebook;
    this.notebookIndex = search;
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
            new TypeToken<List<String>>() {
            }.getType());
    notebook.bindInterpretersToNote(noteId, settingIdList);
    return new JsonResponse(Status.OK).build();
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
              setting.getInterpreterGroup(),
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
                setting.getInterpreterGroup(),
                false)
        );
      }
    }
    return new JsonResponse(Status.OK, "", settingList).build();
  }

  @GET
  public Response getNotebookList() throws IOException {
    List<Map<String, String>> notesInfo = zeppelin.getNotebookServer().
            generateNotebooksInfo();
    return new JsonResponse(Status.OK, "", notesInfo).build();
  }

  @GET
  @Path("{notebookId}")
  public Response getNotebook(@PathParam("notebookId") String notebookId) throws IOException {
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, "", note).build();
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
    logger.info("Create new notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    Note note = notebook.createNote();
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
    note.persist();
    zeppelin.getNotebookServer().broadcastNote(note);
    zeppelin.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.CREATED, "", note.getId()).build();
  }

  /**
   * Delete note REST API
   * <p>
   * @param notebookId@return JSON with status.OK
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}")
  public Response deleteNote(@PathParam("notebookId") String notebookId) throws
          IOException {
    logger.info("Delete notebook {} ", notebookId);
    if (!(notebookId.isEmpty())) {
      Note note = notebook.getNote(notebookId);
      if (note != null) {
        notebook.removeNote(notebookId);
      }
    }
    zeppelin.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.OK, "").build();
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
    logger.info("clone notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    String newNoteName = request.getName();
    Note newNote = notebook.cloneNote(notebookId, newNoteName);
    zeppelin.getNotebookServer().broadcastNote(newNote);
    zeppelin.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.CREATED, "", newNote.getId()).build();
  }

  /**
   * Insert paragraph REST API
   * @param notebookId
   * @param message - JSON containing paragraph's information
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{notebookId}/paragraph")
  public Response insertParagraph(@PathParam("notebookId") String notebookId, String message)
      throws IOException {
    logger.info("insert paragraph {} {}", notebookId, message);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    NewParagraphRequest request = gson.fromJson(message, NewParagraphRequest.class);

    Paragraph p;
    Double indexDouble = request.getIndex();
    if (indexDouble == null) {
      p = note.addParagraph();
    } else {
      p = note.insertParagraph(indexDouble.intValue());
    }
    p.setTitle(request.getTitle());
    p.setText(request.getText());

    note.persist();
    zeppelin.getNotebookServer().broadcastNote(note);
    return new JsonResponse(Status.CREATED, "", p.getId()).build();
  }

  /**
   * Get paragraph REST API
   * @param notebookId
   * @param paragraphId
   * @return JSON with information of the paragraph
   * @throws IOException
   */
  @GET
  @Path("{notebookId}/paragraph/{paragraphId}")
  public Response getParagraph(@PathParam("notebookId") String notebookId,
                               @PathParam("paragraphId") String paragraphId) throws IOException {
    logger.info("get paragraph {} {}", notebookId, paragraphId);

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
   * @param notebookId
   * @param paragraphId
   * @param newIndex - new index to move
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{notebookId}/paragraph/{paragraphId}/move/{newIndex}")
  public Response moveParagraph(@PathParam("notebookId") String notebookId,
                                @PathParam("paragraphId") String paragraphId,
                                @PathParam("newIndex") String newIndex) throws IOException {
    logger.info("move paragraph {} {} {}", notebookId, paragraphId, newIndex);

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

      note.persist();
      zeppelin.getNotebookServer().broadcastNote(note);
      return new JsonResponse(Status.OK, "").build();
    } catch (IndexOutOfBoundsException e) {
      return new JsonResponse(Status.BAD_REQUEST, "paragraph's new index is out of bound").build();
    }
  }

  /**
   * Delete paragraph REST API@return JSON with status.OK
   * @param notebookId
   * @param paragraphId
   * @return 
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}/paragraph/{paragraphId}")
  public Response deleteParagraph(@PathParam("notebookId") String notebookId,
                                  @PathParam("paragraphId") String paragraphId) throws IOException {
    logger.info("delete paragraph {} {}", notebookId, paragraphId);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse(Status.NOT_FOUND, "paragraph not found.").build();
    }

    note.removeParagraph(paragraphId);
    note.persist();
    zeppelin.getNotebookServer().broadcastNote(note);

    return new JsonResponse(Status.OK, "").build();
  }

  /**
   * Run notebook jobs REST API
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{notebookId}")
  public Response runNoteJobs(@PathParam("notebookId") String notebookId) throws
      IOException, IllegalArgumentException {
    logger.info("run notebook jobs {} ", notebookId);
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }
    
    note.runAll();
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Stop(delete) notebook jobs REST API
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{notebookId}")
  public Response stopNoteJobs(@PathParam("notebookId") String notebookId) throws
      IOException, IllegalArgumentException {
    logger.info("stop notebook jobs {} ", notebookId);
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
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("job/{notebookId}")
  public Response getNoteJobStatus(@PathParam("notebookId") String notebookId) throws
      IOException, IllegalArgumentException {
    logger.info("get notebook job status.");
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, null, note.generateParagraphsInfo()).build();
  }
  
  /**
   * Run paragraph job REST API
   * 
   * @param notebookId
   * @param message - JSON with params if user wants to update dynamic form's value
   *                null, empty string, empty json if user doesn't want to update
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
    logger.info("run paragraph job {} {} {}", notebookId, paragraphId, message);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph paragraph = note.getParagraph(paragraphId);
    if (paragraph == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "paragraph not found.").build();
    }

    // handle params if presented
    if (!StringUtils.isEmpty(message)) {
      RunParagraphWithParametersRequest request = gson.fromJson(message,
          RunParagraphWithParametersRequest.class);
      Map<String, Object> paramsForUpdating = request.getParams();
      if (paramsForUpdating != null) {
        paragraph.settings.getParams().putAll(paramsForUpdating);
        note.persist();
      }
    }

    note.run(paragraph.getId());
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Stop(delete) paragraph job REST API
   * @param notebookId
   * @param paragraphId@return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{notebookId}/{paragraphId}")
  public Response stopParagraph(@PathParam("notebookId") String notebookId, 
                                @PathParam("paragraphId") String paragraphId) throws
                                IOException, IllegalArgumentException {
    logger.info("stop paragraph job {} ", notebookId);
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "paragraph not found.").build();
    }
    p.abort();
    return new JsonResponse<>(Status.OK).build();
  }
    
  /**
   * Register cron job REST API
   * @param notebookId
   * @param message - JSON with cron expressions.
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("cron/{notebookId}")
  public Response registerCronJob(@PathParam("notebookId") String notebookId, String message) throws
      IOException, IllegalArgumentException {
    logger.info("Register cron job note={} request cron msg={}", notebookId, message);

    CronRequest request = gson.fromJson(message,
                          CronRequest.class);
    
    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }
    
    if (!CronExpression.isValidExpression(request.getCronString())) {
      return new JsonResponse<>(Status.BAD_REQUEST, "wrong cron expressions.").build();
    }

    Map<String, Object> config = note.getConfig();
    config.put("cron", request.getCronString());
    note.setConfig(config);
    notebook.refreshCron(note.id());
    
    return new JsonResponse<>(Status.OK).build();
  }
  
  /**
   * Remove cron job REST API
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("cron/{notebookId}")
  public Response removeCronJob(@PathParam("notebookId") String notebookId) throws
      IOException, IllegalArgumentException {
    logger.info("Remove cron job note {}", notebookId);

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
   * @param notebookId
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("cron/{notebookId}")
  public Response getCronJob(@PathParam("notebookId") String notebookId) throws
      IOException, IllegalArgumentException {
    logger.info("Get cron job note {}", notebookId);

    Note note = notebook.getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }
    
    return new JsonResponse<>(Status.OK, note.getConfig().get("cron")).build();
  }  

  /**
   * Search for a Notes
   * @param queryTerm
   * @return 
   */
  @GET
  @Path("search")
  public Response search(@QueryParam("q") String queryTerm) {
    logger.info("Searching notebooks for: {}", queryTerm);
    List<Map<String, String>> notebooksFound = notebookIndex.query(queryTerm);
    logger.info("{} notbooks found", notebooksFound.size());
    return new JsonResponse<>(Status.OK, notebooksFound).build();
  }

  /**
   * List all Tutorial notes.
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("/tutorial")
  public Response getTutorialNotes() throws AppException {
    List<NoteInfo> noteInfo;
    try {
      notebookRepo = zeppelinResource.setupNotebookRepo(null);
      noteInfo = notebookRepo.list();
    } catch (IOException ex) {
      noteInfo = null;
    }
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }

  /**
   * List all notes in a project
   * @param id
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("{id}/notebooks")
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getAllNotesInProject(@PathParam("id") Integer id) throws
          AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<NoteInfo> noteInfos;
    try {
      notebookRepo = zeppelinResource.setupNotebookRepo(project);
      noteInfos = notebookRepo.list();
    } catch (IOException ex) {
      noteInfos = null;
    }
    return new JsonResponse(Status.OK, "", noteInfos).build();
  }

  /**
   * Create new note in a project
   * @param id
   * @param newNote
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Path("{id}/new")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createNew(@PathParam("id") Integer id,
          NewNotebookRequest newNote) throws
          AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    Notebook newNotebook;
    Note note;
    NoteInfo noteInfo;
    try {
      notebookRepo = zeppelinResource.setupNotebookRepo(project);
      newNotebook = new Notebook(notebookRepo, zeppelin.getNotebookIndex());
      note = newNotebook.createNote();
      note.addParagraph(); // it's an empty note. so add one paragraph
      String noteName = newNote.getName();
      
      if (noteName == null || noteName.isEmpty()) {
        noteName = "Note " + note.getId();
      }
      note.setName(noteName);
      note.persist();
      noteInfo = new NoteInfo(note);
    } catch (IOException | SchedulerException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create notebook" + ex.getMessage());
    }
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }
}
