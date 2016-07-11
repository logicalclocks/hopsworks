package se.kth.hopsworks.zeppelin.rest;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;
import se.kth.hopsworks.zeppelin.util.LivyMsg;
import se.kth.hopsworks.zeppelin.util.ZeppelinResource;

@Path("/interpreter")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
public class InterpreterService {

  Logger logger = LoggerFactory.getLogger(InterpreterService.class);

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @Inject
  private InterpreterRestApi interpreterRestApi;

  @Path("/")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public InterpreterRestApi interpreter(@Context HttpServletRequest httpReq)
          throws AppException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    if (project == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find project. Make sure cookies are enabled.");
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    if (user == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find remote user.");
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName(), user.getEmail());
    if (zeppelinConf == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not connect to web socket.");
    }

    String userRole = projectTeamBean.findCurrentRole(project, user);

    if (userRole == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "You curently have no role in this project!");
    }
    interpreterRestApi.setParms(project, user, userRole, zeppelinConf);
    return interpreterRestApi;
  }
  
  @GET
  @Path("/livy/sessions")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response getSessions() {
    LivyMsg sessions = zeppelinResource.getLivySessions();
    return new JsonResponse(Response.Status.OK, "", sessions).build();
  }

  @GET
  @Path("/livy/sessions/{sessionId}")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response getSession(@PathParam("sessionId") int sessionId) {
    LivyMsg.Session session = zeppelinResource.getLivySession(sessionId);
    if (session == null) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not found.").build();
    }
    return new JsonResponse(Response.Status.OK, "", session).build();
  }

  @DELETE
  @Path("/livy/sessions/delete/{sessionId}")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response deleteSession(@PathParam("sessionId") int sessionId) {
    int res = zeppelinResource.deleteLivySession(sessionId);
    if (res == Response.Status.NOT_FOUND.getStatusCode()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not found.").build();
    }
    return new JsonResponse(Response.Status.OK, "").build();
  }


}
