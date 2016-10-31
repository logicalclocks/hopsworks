package se.kth.hopsworks.zeppelin.rest;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
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
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;
import se.kth.hopsworks.zeppelin.util.ZeppelinResource;

@Path("/notebook")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
public class NotebookService {
  Logger logger = LoggerFactory.getLogger(NotebookService.class);

  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @Inject
  private NotebookRestApi notebookRestApi;

  @Path("/")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public NotebookRestApi interpreter(@Context HttpServletRequest httpReq)
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
    String userRole = projectTeamBean.findCurrentRole(project, user);
    if (userRole == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "You curently have no role in this project!");
    }
    
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName(), user.getEmail());
    if (zeppelinConf == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not connect to web socket.");
    }
    notebookRestApi.setParms(project, userRole, zeppelinConf);
    return notebookRestApi;
  }

}
