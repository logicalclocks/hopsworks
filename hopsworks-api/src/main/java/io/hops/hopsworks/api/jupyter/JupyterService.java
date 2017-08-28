package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.util.LivyService;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterConfigFactory;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.GenericEntity;
import org.apache.commons.codec.digest.DigestUtils;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterService {

  private final static Logger LOGGER = Logger.getLogger(JupyterService.class.
          getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserManager userManager;
  @EJB
  private UserFacade userFacade;
  @EJB
  private JupyterConfigFactory jupyterConfigFactory;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private Settings settings;
  @EJB
  private LivyService livyService;

  private Integer projectId;
  private Project project;

  public JupyterService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  /**
   * Launches a Jupyter notebook server for this project-specific user
   *
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response getAllNotebookServersInProject(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    Collection<JupyterProject> servers = project.getJupyterProjectCollection();

    if (servers == null) {
      throw new AppException(
              Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find any Jupyter notebook servers for this project.");
    }

    List<JupyterProject> listServers = new ArrayList<>();
    listServers.addAll(servers);

    GenericEntity<List<JupyterProject>> notebookServers
            = new GenericEntity<List<JupyterProject>>(listServers) {   };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            notebookServers).build();
  }

  @GET
  @Path("/settings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response settings(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    String loggedinemail = sc.getUserPrincipal().getName();
    JupyterSettings js = jupyterSettingsFacade.findByProjectUser(projectId,
            loggedinemail);

    if (settings.isPythonKernelEnabled()) {
      js.setPrivateDir(settings.getStagingDir() + Settings.PRIVATE_DIRS + js.
              getSecret());
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            js).build();
  }

  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response isRunning(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(
              Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find any Jupyter notebook server for this project.");
    }
    // Check to make sure the jupyter notebook server is running
    boolean running = jupyterConfigFactory.pingServerJupyterUser(jp.getPid());
    // if the notebook is not running but we have a database entry for it,
    // we should remove the DB entry (and restart the notebook server).
    if (!running) {
      jupyterFacade.removeNotebookServer(hdfsUser);
      throw new AppException(
              Response.Status.NOT_FOUND.getStatusCode(),
              "Found Jupyter notebook server for you, but it wasn't running.");
    }
    String externalIp = Ip.getHost(req.getRequestURL().toString());
    settings.setHopsworksExternalIp(externalIp);
    Integer port = req.getLocalPort();
    String endpoint = externalIp + ":" + port;
    if (endpoint.compareToIgnoreCase(jp.getHostIp()) != 0) {
      // update the host_ip to whatever the client saw as the remote host:port
      jp.setHostIp(endpoint);
      jupyterFacade.update(jp);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jp).build();
  }

  @POST
  @Path("/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response startNotebookServer(JupyterSettings jupyterSettings,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not find your username. Report a bug.");
    }

    boolean enabled = project.getConda();
    if (!enabled) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "First enable Anaconda. Click on 'Settings -> Python'");
    }

    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);

    if (jp == null) {
      HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);

      String configSecret = DigestUtils.sha256Hex(Integer.toString(
              ThreadLocalRandom.current().nextInt()));
      JupyterDTO dto;
      try {

        jupyterSettingsFacade.update(jupyterSettings);

        dto = jupyterConfigFactory.startServerAsJupyterUser(project,
                configSecret, hdfsUser, jupyterSettings);
      } catch (InterruptedException | IOException ex) {
        Logger.getLogger(JupyterService.class.getName()).log(Level.SEVERE, null,
                ex);
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Problem starting a Jupyter notebook server.");
      }

      if (dto == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Incomplete request!");
      }

      String externalIp = Ip.getHost(req.getRequestURL().toString());

      jp = jupyterFacade.saveServer(externalIp, project, configSecret,
              dto.getPort(), user.getId(), dto.getToken(), dto.getPid());

      if (jp == null) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Could not save Jupyter Settings.");
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jp).build();
  }

  @GET
  @Path("/stopAll")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({"HOPS_ADMIN"})
  public Response stopAll(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    jupyterConfigFactory.stopProject(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stopDataOwner")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response stopDataOwner(@PathParam("hdfsUsername") String hdfsUsername,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    stop(hdfsUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response stopNotebookServer(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String hdfsUsername = getHdfsUser(sc);
    stop(hdfsUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void stop(String hdfsUser) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not find Jupyter entry for user: " + hdfsUser);
    }
    livyService.deleteAllJupyterLivySessions(hdfsUser);
    String projectPath = jupyterConfigFactory.getJupyterHome(hdfsUser, jp);

    // stop the server, remove the user in this project's local dirs
    jupyterConfigFactory.killServerJupyterUser(projectPath, jp.getPid(), jp.
            getPort());
    // remove the reference to th e server in the DB.
    jupyterFacade.removeNotebookServer(hdfsUser);
  }

  private String getHdfsUser(SecurityContext sc) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              "You are not authorized for this invocation.");
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    return hdfsUsername;
  }

}
