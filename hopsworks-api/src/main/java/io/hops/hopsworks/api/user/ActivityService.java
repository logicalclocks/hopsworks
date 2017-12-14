package io.hops.hopsworks.api.user;

import io.hops.hopsworks.api.filter.NoCacheResponse;
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
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.swagger.annotations.Api;

@Path("/activity")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@Api(value = "Activity", description = "User activity service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ActivityService {

  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response findAllByUser(@Context SecurityContext sc,
          @Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    List<Activity> activityDetails = activityFacade.getAllActivityByUser(user);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {};

    Response r = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(
                    projectActivities).build();
    return r;
  }

  @GET
  @Path("/inode")
  @Produces(MediaType.APPLICATION_JSON)
  public Response findByInode(@QueryParam("inodeId") int inodeId,
          @QueryParam("from") int from,
          @QueryParam("to") int to,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    List<Activity> activityDetails = activityFacade.getAllActivityByUser(user);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {};

    Response r = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(
                    projectActivities).build();
    return r;
  }

  @GET
  @Path("/query")
  @Produces(MediaType.APPLICATION_JSON)
  public Response findPaginatedByUser(@QueryParam("from") int from,
          @QueryParam("to") int to,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    List<Activity> activityDetails = activityFacade.
            getPaginatedActivityByUser(from, to, user);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findAllByProject(@PathParam("id") Integer id,
          @Context SecurityContext sc, @Context HttpServletRequest req) {
    Project project = projectFacade.find(id);
    List<Activity> activityDetails = activityFacade.
            getAllActivityOnProject(project);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }

  @GET
  @Path("{id}/query")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findPaginatedByProject(@PathParam("id") Integer id,
          @QueryParam("from") int from,
          @QueryParam("to") int to,
          @Context SecurityContext sc, @Context HttpServletRequest req) {
    Project project = projectFacade.find(id);
    List<Activity> activityDetails = activityFacade.
            getPaginatedActivityForProject(from, to, project);
    GenericEntity<List<Activity>> projectActivities
            = new GenericEntity<List<Activity>>(activityDetails) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectActivities).build();
  }
}
