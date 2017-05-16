package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import io.hops.hopsworks.api.zeppelin.util.ZeppelinResource;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;
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

@Path("/zeppelin/{projectID}/interpreter")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Zeppelin interpreter",
        description = "Zeppelin interpreter")
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
  public InterpreterRestApi interpreter(@PathParam("projectID") String projectID,
          @Context HttpServletRequest httpReq)
          throws AppException {
    Project project = zeppelinResource.getProject(projectID);
    if (project == null) {
      logger.error("Could not find project in cookies.");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find project. Make sure cookies are enabled.");
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    if (user == null) {
      logger.error("Could not find remote user in request.");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Could not find remote user.");
    }
    String userRole = projectTeamBean.findCurrentRole(project, user);
    if (userRole == null) {
      logger.error("User with no role in this project.");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "You curently have no role in this project!");
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName(), user.getEmail());
    if (zeppelinConf == null) {
      logger.error("Could not connect to web socket.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not connect to web socket.");
    }
    interpreterRestApi.setParms(project, user, userRole, zeppelinConf);
    return interpreterRestApi;
  }
  
  @GET
  @Path("/check")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public Response interpreterCheck(@PathParam("projectID") String projectID,
      @Context HttpServletRequest httpReq) throws AppException {
    Project project = zeppelinResource.getProject(projectID);
    if (project == null) {
      logger.error("Could not find project in cookies.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    if (user == null) {
      logger.error("Could not find remote user in request.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    String userRole = projectTeamBean.findCurrentRole(project, user);
    if (userRole == null) {
      logger.error("User with no role in this project.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
        getName(), user.getEmail());
    if (zeppelinConf == null) {
      logger.error("Zeppelin  not connect to web socket.");
      return new JsonResponse(Response.Status.NOT_FOUND, "").build();
    }
    return new JsonResponse(Response.Status.OK, "").build();
  }

  @GET
  @Path("/livy/sessions")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response getSessions(@PathParam("projectID") String projectID) {
    LivyMsg sessions = zeppelinResource.getLivySessions();
    return new JsonResponse(Response.Status.OK, "", sessions).build();
  }

  @GET
  @Path("/livy/sessions/{sessionId}")
  @Produces("application/json")
  @RolesAllowed({"HOPS_ADMIN"})
  public Response getSession(@PathParam("projectID") String projectID,
          @PathParam("sessionId") int sessionId) {
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
  public Response deleteSession(@PathParam("projectID") String projectID,
          @PathParam("sessionId") int sessionId) {
    int res = zeppelinResource.deleteLivySession(sessionId);
    if (res == Response.Status.NOT_FOUND.getStatusCode()) {
      return new JsonResponse(Response.Status.NOT_FOUND, "Session '" + sessionId
              + "' not found.").build();
    }
    return new JsonResponse(Response.Status.OK, "").build();
  }

}
