package se.kth.hopsworks.zeppelin.rest;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.zeppelin.interpreter.InterpreterSetting;
import se.kth.hopsworks.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.ejb.EJB;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.quartz.SchedulerException;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;

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
  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;
  private final Notebook notebook;
  private NotebookRepo notebookRepo;

  public NotebookRestApi() {
    this.notebook = new Notebook();
  }

  public NotebookRestApi(Notebook notebook) {
    this.notebook = notebook;
  }

  /**
   * bind a setting to note
   * <p>
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
   * <p>
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

  /**
   * List all Tutorial notes.
   * <p>
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  public Response getTutorialNotes() throws AppException {
    List<NoteInfo> noteInfo;
    try {
      notebookRepo = setupNotebookRepo(null);
      noteInfo = notebookRepo.list();
    } catch (IOException ex) {
      noteInfo = null;
      System.out.println("NNNNNNote : " + ex.getMessage());
    }
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }

  /**
   * List all notes in a project
   * <p>
   * @param id
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("{id}")
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
      notebookRepo = setupNotebookRepo(project);
      noteInfos = notebookRepo.list();
    } catch (IOException ex) {
      noteInfos = null;
    }
    return new JsonResponse(Status.OK, "", noteInfos).build();
  }

  /**
   * Create new note in a project
   * <p>
   * @param id
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("{id}/new")
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createNew(@PathParam("id") Integer id) throws
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
      notebookRepo = setupNotebookRepo(project);
      newNotebook = new Notebook(notebookRepo);
      note = newNotebook.createNote();
      note.addParagraph(); // it's an empty note. so add one paragraph
      note.persist();
      noteInfo = new NoteInfo(note);
    } catch (IOException | SchedulerException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create notebook" + ex.getMessage());
    }
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }

  private NotebookRepo setupNotebookRepo(Project project) throws AppException {
    ZeppelinConfiguration conf = zeppelin.getConf();
    Class<?> notebookStorageClass;
    NotebookRepo repo;
    try {
      notebookStorageClass = Class.forName(conf.getString(
              ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE));
      Constructor<?> constructor = notebookStorageClass.getConstructor(
              ZeppelinConfiguration.class, Project.class);
      repo = (NotebookRepo) constructor.newInstance(conf, project);

    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
            InstantiationException | IllegalAccessException |
            IllegalArgumentException | InvocationTargetException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not instantiate notebook" + ex.getMessage());
    }

    return repo;
  }
}
