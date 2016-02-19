package se.kth.hopsworks.zeppelin.rest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.Paragraph;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.zeppelin.rest.message.CronRequest;
import se.kth.hopsworks.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import se.kth.hopsworks.zeppelin.rest.message.NewNotebookRequest;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import se.kth.hopsworks.zeppelin.rest.message.NewParagraphRequest;
import se.kth.hopsworks.zeppelin.rest.message.RunParagraphWithParametersRequest;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;
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
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;

  public NotebookRestApi() {
  }

  /**
   * bind a setting to note
   * <p/>
   * @param noteId
   * @param req
   * @param httpReq
   * @return
   * @throws IOException
   */
  @PUT
  @Path("interpreter/bind/{noteId}")
  public Response bind(@PathParam("noteId") String noteId, String req,
          @Context HttpServletRequest httpReq) throws IOException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    List<String> settingIdList = gson.fromJson(req,
            new TypeToken<List<String>>() {
    }.getType());
    zeppelinConf.getNotebook().bindInterpretersToNote(noteId, settingIdList);
    return new JsonResponse(Status.OK).build();
  }

  /**
   * list binded setting
   * <p/>
   * @param noteId
   * @param httpReq
   * @return
   */
  @GET
  @Path("interpreter/bind/{noteId}")
  public Response bind(@PathParam("noteId") String noteId,
          @Context HttpServletRequest httpReq) {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    List<InterpreterSettingListForNoteBind> settingList;
    settingList = new LinkedList<>();

    List<InterpreterSetting> selectedSettings = zeppelinConf.getNotebook().
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

    List<InterpreterSetting> availableSettings = zeppelinConf.getNotebook().
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
  public Response getNotebookList(@Context HttpServletRequest httpReq) throws
          IOException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    List<Map<String, String>> notesInfo = zeppelinConf.getNotebookServer().
            generateNotebooksInfo(false);
    return new JsonResponse(Status.OK, "", notesInfo).build();
  }

  @GET
  @Path("{notebookId}")
  public Response getNotebook(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq) throws
          IOException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, "", note).build();
  }

  /**
   * Create new note REST API
   *
   * @param message - JSON with new note name
   * @param httpReq
   * @return JSON with new note ID
   * @throws IOException
   */
  @POST
  public Response createNote(String message, @Context HttpServletRequest httpReq)
          throws IOException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    logger.info("Create new notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    Note note = zeppelinConf.getNotebook().createNote();
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
    zeppelinConf.getNotebookServer().broadcastNote(note);
    zeppelinConf.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.CREATED, "", note.getId()).build();
  }

  /**
   * Delete note REST API
   * <p>
   * @param notebookId
   * @param httpReq@return JSON with status.OK
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}")
  public Response deleteNote(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq) throws IOException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    logger.info("Delete notebook {} ", notebookId);
    if (!(notebookId.isEmpty())) {
      Note note = zeppelinConf.getNotebook().getNote(notebookId);
      if (note != null) {
        zeppelinConf.getNotebook().removeNote(notebookId);
      }
    }
    zeppelinConf.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.OK, "").build();
  }

  /**
   * Clone note REST API@return JSON with status.CREATED
   * <p>
   * @param notebookId
   * @param message
   * @param httpReq
   * @return
   * @throws IOException
   * @throws java.lang.CloneNotSupportedException
   */
  @POST
  @Path("{notebookId}")
  public Response cloneNote(@PathParam("notebookId") String notebookId,
          String message, @Context HttpServletRequest httpReq) throws
          IOException, CloneNotSupportedException, IllegalArgumentException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    logger.info("clone notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    String newNoteName = request.getName();
    Note newNote = zeppelinConf.getNotebook().cloneNote(notebookId, newNoteName);
    zeppelinConf.getNotebookServer().broadcastNote(newNote);
    zeppelinConf.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.CREATED, "", newNote.getId()).build();
  }

  /**
   * Insert paragraph REST API
   *
   * @param notebookId
   * @param message - JSON containing paragraph's information
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{notebookId}/paragraph")
  public Response insertParagraph(@PathParam("notebookId") String notebookId,
          String message, @Context HttpServletRequest httpReq)
          throws IOException {
    logger.info("insert paragraph {} {}", notebookId, message);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
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

    note.persist();
    zeppelinConf.getNotebookServer().broadcastNote(note);
    return new JsonResponse(Status.CREATED, "", p.getId()).build();
  }

  /**
   * Get paragraph REST API
   *
   * @param notebookId
   * @param paragraphId
   * @param httpReq
   * @return JSON with information of the paragraph
   * @throws IOException
   */
  @GET
  @Path("{notebookId}/paragraph/{paragraphId}")
  public Response getParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          @Context HttpServletRequest httpReq) throws IOException {
    logger.info("get paragraph {} {}", notebookId, paragraphId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
   * @param paragraphId
   * @param newIndex - new index to move
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException
   */
  @POST
  @Path("{notebookId}/paragraph/{paragraphId}/move/{newIndex}")
  public Response moveParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          @PathParam("newIndex") String newIndex,
          @Context HttpServletRequest httpReq) throws IOException {
    logger.info("move paragraph {} {} {}", notebookId, paragraphId, newIndex);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
      zeppelinConf.getNotebookServer().broadcastNote(note);
      return new JsonResponse(Status.OK, "").build();
    } catch (IndexOutOfBoundsException e) {
      return new JsonResponse(Status.BAD_REQUEST,
              "paragraph's new index is out of bound").build();
    }
  }

  /**
   * Delete paragraph REST API@return JSON with status.OK
   *
   * @param notebookId
   * @param paragraphId
   * @param httpReq
   * @return
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}/paragraph/{paragraphId}")
  public Response deleteParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          @Context HttpServletRequest httpReq) throws IOException {
    logger.info("delete paragraph {} {}", notebookId, paragraphId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
    if (note == null) {
      return new JsonResponse(Status.NOT_FOUND, "note not found.").build();
    }

    Paragraph p = note.getParagraph(paragraphId);
    if (p == null) {
      return new JsonResponse(Status.NOT_FOUND, "paragraph not found.").build();
    }

    note.removeParagraph(paragraphId);
    note.persist();
    zeppelinConf.getNotebookServer().broadcastNote(note);

    return new JsonResponse(Status.OK, "").build();
  }

  /**
   * Run notebook jobs REST API
   *
   * @param notebookId
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{notebookId}")
  public Response runNoteJobs(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq) throws
          IOException, IllegalArgumentException {
    logger.info("run notebook jobs {} ", notebookId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{notebookId}")
  public Response stopNoteJobs(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq) throws IOException,
          IllegalArgumentException {
    logger.info("stop notebook jobs {} ", notebookId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("job/{notebookId}")
  public Response getNoteJobStatus(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq) throws IOException,
          IllegalArgumentException {
    logger.info("get notebook job status.");
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
   * @param httpReq
   *
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("job/{notebookId}/{paragraphId}")
  public Response runParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          String message, @Context HttpServletRequest httpReq) throws
          IOException, IllegalArgumentException {
    logger.info("run paragraph job {} {} {}", notebookId, paragraphId, message);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
        note.persist();
      }
    }

    note.run(paragraph.getId());
    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Stop(delete) paragraph job REST API
   *
   * @param notebookId
   * @param paragraphId
   * @param httpReq@return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("job/{notebookId}/{paragraphId}")
  public Response stopParagraph(@PathParam("notebookId") String notebookId,
          @PathParam("paragraphId") String paragraphId,
          @Context HttpServletRequest httpReq) throws
          IOException, IllegalArgumentException {
    logger.info("stop paragraph job {} ", notebookId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @POST
  @Path("cron/{notebookId}")
  public Response registerCronJob(@PathParam("notebookId") String notebookId,
          String message, @Context HttpServletRequest httpReq) throws
          IOException, IllegalArgumentException {
    logger.info("Register cron job note={} request cron msg={}", notebookId,
            message);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    CronRequest request = gson.fromJson(message,
            CronRequest.class);

    Note note = zeppelinConf.getNotebook().getNote(notebookId);
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
    zeppelinConf.getNotebook().refreshCron(note.id());

    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Remove cron job REST API
   *
   * @param notebookId
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @DELETE
  @Path("cron/{notebookId}")
  public Response removeCronJob(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq)
          throws IOException, IllegalArgumentException {
    logger.info("Remove cron job note {}", notebookId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    Map<String, Object> config = note.getConfig();
    config.put("cron", null);
    note.setConfig(config);
    zeppelinConf.getNotebook().refreshCron(note.id());

    return new JsonResponse<>(Status.OK).build();
  }

  /**
   * Get cron job REST API
   *
   * @param notebookId
   * @param httpReq
   * @return JSON with status.OK
   * @throws IOException, IllegalArgumentException
   */
  @GET
  @Path("cron/{notebookId}")
  public Response getCronJob(@PathParam("notebookId") String notebookId,
          @Context HttpServletRequest httpReq) throws
          IOException, IllegalArgumentException {
    logger.info("Get cron job note {}", notebookId);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Note note = zeppelinConf.getNotebook().getNote(notebookId);
    if (note == null) {
      return new JsonResponse<>(Status.NOT_FOUND, "note not found.").build();
    }

    return new JsonResponse<>(Status.OK, note.getConfig().get("cron")).build();
  }

  /**
   * Search for a Notes
   *
   * @param queryTerm
   * @param httpReq
   * @return
   */
  @GET
  @Path("search")
  public Response search(@QueryParam("q") String queryTerm,
          @Context HttpServletRequest httpReq) {
    logger.info("Searching notebooks for: {}", queryTerm);
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    List<Map<String, String>> notebooksFound = zeppelinConf.getNotebookIndex().
            query(queryTerm);
    logger.info("{} notbooks found", notebooksFound.size());
    return new JsonResponse<>(Status.OK, notebooksFound).build();
  }

}
