package io.hops.hopsworks.api.pythonDeps;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepJson;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.WebCommunication;
import java.util.ArrayList;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PythonDepsService {

  private final static Logger logger = Logger.getLogger(PythonDepsService.class.
          getName());

  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserManager userManager;
  @EJB
  private WebCommunication web;

  private Integer projectId;
  private Project project;

  public void setProject(Project project) {
    this.project = project;
  }

  public Project getProject() {
    return project;
  }

  public PythonDepsService() {

  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response index() throws AppException {

    Collection<PythonDep> pysparkDeps = new ArrayList<PythonDep>();
    pysparkDeps = project.getPythonDepCollection();
    GenericEntity<Collection<PythonDep>> pysparkDepsList
            = new GenericEntity<Collection<PythonDep>>(pysparkDeps) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            pysparkDepsList).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/remove")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response remove(PythonDepJson removeLibrary) throws AppException {

    Collection<PythonDep> pysparkDeps = new ArrayList<PythonDep>();
    pysparkDeps = project.getPythonDepCollection();
    GenericEntity<Collection<PythonDep>> pysparkDepsList
            = new GenericEntity<Collection<PythonDep>>(pysparkDeps) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            pysparkDepsList).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/install")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response install(PythonDepJson library) throws AppException {

    List<Host> hosts = pythonDepsFacade.installLibrary(project,
            library.getChannel(),
            library.getDependency(), library.getVersion());

    List<PythonDep> libs = new ArrayList<>();

    // 5. Send REST calls to all of the kagents using a thread pool - in a different thread
    for (Host h : hosts) {
//      web.installPythonDep(project.getName(), library)
    }

    GenericEntity<Collection<PythonDep>> pysparkDepsList
            = new GenericEntity<Collection<PythonDep>>(libs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            pysparkDepsList).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/update")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response update(PythonDepJson library) throws AppException {

    List<Host> hosts = pythonDepsFacade.updateLibrary(project,
            library.getChannel(),
            library.getDependency(), library.getVersion());

    List<PythonDep> libs = new ArrayList<>();

    // 5. Send REST calls to all of the kagents using a thread pool - in a different thread
    for (Host h : hosts) {
//      web.installPythonDep(project.getName(), library)
    }

    GenericEntity<Collection<PythonDep>> pysparkDepsList
            = new GenericEntity<Collection<PythonDep>>(libs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            pysparkDepsList).build();
  }

  @GET
  @Path("/clone/{projectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response doClone(@PathParam("projectName") String projectName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {


    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

}
