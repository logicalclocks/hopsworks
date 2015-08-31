package se.kth.hopsworks.zeppelin.rest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.hopsworks.zeppelin.notebook.Notebook;
import se.kth.hopsworks.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import se.kth.hopsworks.zeppelin.server.JsonResponse;

/**
 * Rest api endpoint for the noteBook.
 */
@Path("/notebook")
@Produces("application/json")
public class NotebookRestApi {

  Logger logger = LoggerFactory.getLogger(NotebookRestApi.class);
  Gson gson = new Gson();
  private final Notebook notebook;

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
}
