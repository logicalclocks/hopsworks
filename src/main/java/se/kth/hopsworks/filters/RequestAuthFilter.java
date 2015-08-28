package se.kth.hopsworks.filters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.rest.JsonResponse;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 *
 * Request filter that can be used to restrict users accesses to projects based
 * on the role they have for the project and the annotation on the method being
 * called.
 */
@Provider
public class RequestAuthFilter implements ContainerRequestFilter {

  @EJB
  private ProjectTeamFacade projectTeamBean;

  @EJB
  private ProjectFacade projectBean;

  @Context
  private ResourceInfo resourceInfo;

  private final static Logger log = Logger.getLogger(RequestAuthFilter.class.
          getName());

  @Override
  public void filter(ContainerRequestContext requestContext) {

    String path = requestContext.getUriInfo().getPath();

    Method method = resourceInfo.getResourceMethod();

    String[] pathParts = path.split("/");
    log.log(Level.INFO, "Filtering request path: {0}", path);
    log.log(Level.INFO, "Method called: {0}", method.getName());
    //intercepted method must be project operations on a specific project
    //with an id (/project/projectId/... or /activity/projectId/...). 
    if (pathParts.length > 1 && (pathParts[0].equalsIgnoreCase("project")
            || pathParts[0].equalsIgnoreCase("activity") 
            || pathParts[0].equalsIgnoreCase("notebook")
            || pathParts[0].equalsIgnoreCase("interpreter"))) {

      JsonResponse json = new JsonResponse();
      Integer projectId;
      String userRole;
      try{
        projectId = Integer.valueOf(pathParts[1]);
      }catch(NumberFormatException ne) {
        //if the second pathparam is not a project id return.
        log.log(Level.INFO, "No project id, leaving interceptor.");
        return;
      }
      
      Project project = projectBean.find(projectId);
      log.log(Level.INFO, "Filtering project request path: {0}", project.getName());
      

      if (!method.isAnnotationPresent(AllowedRoles.class)) {
        //Should throw exception if there is a method that is not annotated in this path.
        requestContext.abortWith(Response.
                status(Response.Status.SERVICE_UNAVAILABLE).build());
        return;
      }
      AllowedRoles rolesAnnotation = method.getAnnotation(AllowedRoles.class);
      Set<String> rolesSet;
      rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.roles()));

      //If the resource is allowed for all roles continue with the request. 
      if (rolesSet.contains(AllowedRoles.ALL)) {
        log.log(Level.INFO, "Accessing resource that is allowed for all");
        return;
      }

      if (requestContext.getSecurityContext().getUserPrincipal() == null) {
        requestContext.abortWith(Response.
                status(Response.Status.UNAUTHORIZED).build());
        return;
      }

      //if the resource is only allowed for some roles check if the user have the requierd role for the resource.
      String userEmail = requestContext.getSecurityContext().getUserPrincipal().
              getName();

      userRole = projectTeamBean.findCurrentRole(project, userEmail);

      if (userRole == null || userRole.isEmpty()) {
        log.log(Level.INFO,
                "Trying to access resource, but you dont have any role in this project");
        json.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
        json.setErrorMsg("You do not have access to this project.");
        requestContext.abortWith(Response
                .status(Response.Status.FORBIDDEN)
                .entity(json)
                .build());
      } else if (!rolesSet.contains(userRole)) {
        log.log(Level.INFO,
                "Trying to access resource that is only allowed for: {0}, But you are a: {1}",
                new Object[]{rolesSet, userRole});
        json.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
        json.setErrorMsg(
                "You do not have the required role to perform this action.");
        requestContext.abortWith(Response
                .status(Response.Status.FORBIDDEN)
                .entity(json)
                .build());
      }
    }
  }
}
