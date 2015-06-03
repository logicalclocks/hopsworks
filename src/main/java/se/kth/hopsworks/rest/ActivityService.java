package se.kth.hopsworks.rest;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/activity")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ActivityService {

  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response findAllByUser(@Context SecurityContext sc,
          @Context HttpServletRequest req) {
    User user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    List<Activity> activityDetails = activityFacade.getAllActivityByUser(user);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }

  @GET
  @Path("/query")
  @Produces(MediaType.APPLICATION_JSON)
  public Response findPaginatedByUser(@QueryParam("from") int from,
          @QueryParam("to") int to,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {
    User user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    List<Activity> activityDetails = activityFacade.
            getPaginatedActivityByUser(from, to, user);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllByProject(@PathParam("id") Integer id,
          @Context SecurityContext sc, @Context HttpServletRequest req) {
    Project project = projectFacade.find(id);
    List<Activity> activityDetails = activityFacade.
            getAllActivityOnProject(project);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }

  @GET
  @Path("{id}/query")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findPaginatedByProject(@PathParam("id") Integer id,
          @QueryParam("from") int from,
          @QueryParam("to") int to,
          @Context SecurityContext sc, @Context HttpServletRequest req) {
    Project project = projectFacade.find(id);
    List<Activity> activityDetails = activityFacade.
            getPaginatedActivityForProject(from, to, project);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }
}
