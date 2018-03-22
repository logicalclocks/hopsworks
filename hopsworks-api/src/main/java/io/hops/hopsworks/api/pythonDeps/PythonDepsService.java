/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.api.pythonDeps;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.OpStatus;
import io.hops.hopsworks.common.dao.pythonDeps.LibVersions;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepJson;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.Version;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
  private Settings settings;
  @EJB
  private HostsFacade hostsFacade;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;
  @EJB
  private JupyterProcessMgr jupyterProcessFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;

  public void setProject(Project project) {
    this.project = project;
  }

  public Project getProject() {
    return project;
  }

  //User upgradable libraries we installed for them
  public static final ArrayList<String> providedLibraryNames = new ArrayList<String>() {
    {
      add("hops");
      add("tfspark");
      add("pandas");
      add("tensorflow-serving-api");
      add("hopsfacets");
      add("numpy");
    }
  };

  //Libraries we preinstalled users should not mess with
  public static final ArrayList<String> preInstalledLibraryNames = new ArrayList<String>() {
    {
      add("tensorflow-gpu");
      add("tensorflow");
      add("horovod");
      add("pydoop");
      add("pyspark");
      add("tensorboard");
    }
  };

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response index() throws AppException {

    Collection<PythonDep> pythonDeps = project.getPythonDepCollection();

    List<PythonDepJson> jsonDeps = new ArrayList<>();
    for (PythonDep pd : pythonDeps) {
      jsonDeps.add(new PythonDepJson(pd));
    }

    GenericEntity<Collection<PythonDepJson>> deps = new GenericEntity<Collection<PythonDepJson>>(jsonDeps) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        deps).build();
  }

  private String getHdfsUser(SecurityContext sc) throws AppException {
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "You are not authorized for this invocation.");
    }
    return hdfsUsersController.getHdfsUserName(project, user);
  }

  @GET
  @Path("/destroyAnaconda")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response removeAnacondaEnv(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {

    pythonDepsFacade.removeProject(project);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/enable/{version}/{pythonKernelEnable}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response enable(@PathParam("version") String version,
      @PathParam("pythonKernelEnable") String pythonKernelEnable,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Map<String, String> deps = pythonDepsFacade.getPreInstalledLibs(project);
    Boolean enablePythonKernel = Boolean.parseBoolean(pythonKernelEnable);
    if (!enablePythonKernel) {
      // 'X' indicates that the python kernel should not be enabled in Conda
      version = version + "X";
    }
    pythonDepsFacade.createProjectInDb(project, deps, version, enablePythonKernel);

    project.setPythonVersion(version);
    projectFacade.update(project);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private Users getUser(String email) throws ThirdPartyException {
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new ThirdPartyException(Response.Status.FORBIDDEN.getStatusCode(), "user not found",
          ThirdPartyException.Source.LOCAL, "exception");
    }
    return user;
  }

  @GET
  @Path("/installed")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @Produces(MediaType.TEXT_PLAIN)
  public Response installed() throws AppException {
    String defaultRepo = settings.getCondaDefaultRepo();
    if (settings.isAnacondaInstalled()) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(defaultRepo).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(
        Response.Status.SERVICE_UNAVAILABLE).build();
  }

  @GET
  @Path("/enabled")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @Produces(MediaType.TEXT_PLAIN)
  public Response enabled() throws AppException {
    boolean enabled = project.getConda();
    if (enabled) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(project.getPythonVersion()).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(
        Response.Status.SERVICE_UNAVAILABLE).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/remove")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response remove(PythonDepJson library) throws AppException {

    if(preInstalledLibraryNames.contains(library.getLib())) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not uninstall " + library.getLib() + ", it is a mandatory dependency");
    }

    pythonDepsFacade.uninstallLibrary(project, PythonDepsFacade.CondaInstallType.valueOf(library.getInstallType()),
        PythonDepsFacade.MachineType.valueOf(library.getMachineType()), library.getChannelUrl(),
            library.getLib(), library.getVersion());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/clearCondaOps")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response clearCondaOps(PythonDepJson library) throws AppException {

    pythonDepsFacade.clearCondaOps(project,
        library.getChannelUrl(),
        library.getLib(), library.getVersion());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/install")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Response install(PythonDepJson library) throws AppException {

    if(preInstalledLibraryNames.contains(library.getLib())) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not install " + library.getLib() + ", it is already pre-installed");

    }

    if (project.getName().startsWith("demo_tensorflow")) {
      List<OpStatus> opStatuses = pythonDepsFacade.opStatus(project);
      int counter = 0;
      while ((opStatuses != null && !opStatuses.isEmpty()) && counter < 10) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "Error enabled anaconda for demo project", ex);
        }
        opStatuses = pythonDepsFacade.opStatus(project);
        counter++;
      }
    }

    pythonDepsFacade.addLibrary(project, PythonDepsFacade.CondaInstallType.valueOf(library.getInstallType()),
        PythonDepsFacade.MachineType.valueOf(library.getMachineType()),
            library.getChannelUrl(), library.getLib(), library.getVersion());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/installOneHost/{hostId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Response installOneHost(
      @PathParam("hostId") String hostId,
      PythonDepJson library) throws AppException {
    pythonDepsFacade.blockingCondaOp(Integer.parseInt(hostId),
        PythonDepsFacade.CondaOp.INSTALL, PythonDepsFacade.CondaInstallType.valueOf(library.getInstallType()),
            PythonDepsFacade.MachineType.valueOf(library.getMachineType()), project,
            library.getChannelUrl(), library.getLib(), library.getVersion());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }


/* Disable UPGRADE for now

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/upgrade")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response upgrade(PythonDepJson library) throws AppException {

    if(preInstalledLibraryNames.contains(library.getLib())) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not upgrade " + library.getLib());
    }

    logger.log(Level.SEVERE,"INSTALL TYPE " + library.getInstallType());
    logger.log(Level.SEVERE,"MACHINE TYPE " + library.getMachineType());


    pythonDepsFacade.upgradeLibrary(project, PythonDepsFacade.CondaInstallType.valueOf(library.getInstallType()),
            PythonDepsFacade.MachineType.valueOf(library.getMachineType()),
            library.getChannelUrl(), library.getLib(), library.getVersion());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  */

  @GET
  @Path("/clone/{projectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response doClone(
      @PathParam("projectName") String srcProject,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

//    pythonDepsFacade.cloneProject(srcProject, project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/createenv")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response createEnv(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {

    pythonDepsFacade.getPreInstalledLibs(project);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/removeenv")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response removeEnv(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {

    pythonDepsFacade.removeProject(project);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/status")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response status() throws AppException {

    List<OpStatus> response = pythonDepsFacade.opStatus(project);

    GenericEntity<Collection<OpStatus>> opsFound = new GenericEntity<Collection<OpStatus>>(response) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(opsFound).build();

  }

  @GET
  @Path("/failedCondaOps")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getFailedCondaOps(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {

    List<OpStatus> failedOps = pythonDepsFacade.getFailedCondaOpsProject(project);
    GenericEntity<Collection<OpStatus>> opsFound = new GenericEntity<Collection<OpStatus>>(failedOps) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(opsFound).build();
  }

  @GET
  @Path("/retryFailedCondaOps")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response retryFailedCondaOps(@Context SecurityContext sc, @Context HttpServletRequest req)
      throws AppException {

    pythonDepsFacade.retryFailedCondaOpsProject(project);

    List<OpStatus> response = pythonDepsFacade.opStatus(project);
    GenericEntity<Collection<OpStatus>> opsFound = new GenericEntity<Collection<OpStatus>>(response) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(opsFound).build();
  }

  /**
   *
   * @param sc
   * @param req
   * @param httpHeaders
   * @param lib
   * @return 204 if no results found, results if successful, 500 if an error
   * occurs.
   * @throws AppException
   */
  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response search(@Context SecurityContext sc,
      @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders,
      PythonDepJson lib) throws AppException {

    Collection<LibVersions> response = null;
    List<PythonDep> installedDeps = null;

    if(lib.getInstallType().equals(PythonDepsFacade.CondaInstallType.PIP.name())) {
      response = findPipLib(lib);
      installedDeps = pythonDepsFacade.listProject(project);
    } else if(lib.getInstallType().equals(PythonDepsFacade.CondaInstallType.CONDA.name())) {
      response = findCondaLib(lib);
      installedDeps = pythonDepsFacade.listProject(project);
    }



    // 1. Reverse version numbers to have most recent first.
    // 2. Check installation status of each version 
    // Check which of these libraries found are already installed.
    // This code is O(N^2) in the number of hits and installed libs, so
    // it is not optimal
    for (LibVersions l : response) {
      l.reverseVersionList();
      for (PythonDep pd : installedDeps) {
        if (l.getLib().compareToIgnoreCase(pd.getDependency()) == 0) {
          List<Version> allVs = l.getVersions();
          for (Version v : allVs) {
            if (pd.getVersion().compareToIgnoreCase(v.getVersion()) == 0) {
              v.setStatus(pd.getStatus().toString().toLowerCase());
              l.setStatus(pd.getStatus().toString().toLowerCase());
              break;
            }
          }
        }
      }
    }

    GenericEntity<Collection<LibVersions>> libsFound
        = new GenericEntity<Collection<LibVersions>>(response) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        libsFound).build();
  }

  private Collection<LibVersions> findCondaLib(PythonDepJson lib) throws
      AppException {
    String url = lib.getChannelUrl();
    String library = lib.getLib();
    List<LibVersions> all = new ArrayList<>();

    String prog = settings.getHopsworksDomainDir() + "/bin/condasearch.sh";
    ProcessBuilder pb = new ProcessBuilder(prog, url, library);
    try {
      Process process = pb.start();
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.
          getInputStream()));
      String line;
      String foundLib = "";
      String foundVersion;

      while ((line = br.readLine()) != null) {
        // returns key,value  pairs
        String[] libVersion = line.split(",");
        if (libVersion.length != 2) {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Problem listing libraries. Did conda get upgraded and change "
              + "its output format?");
        }
        String key = libVersion[0];
        String value = libVersion[1];
        // if the key starts with a letter, it is a library name, otherwise it's a version number
        // Output searching for 'pandas' looks like this:
        // Loading,channels:
        // pandas-datareader,0.2.0
        // 0.2.0,py34_0
        //....
        // pandasql,0.3.1
        // 0.3.1,np16py27_0
        //....
        // 0.4.2,np18py33_0
        // 
        // Skip the first line
        if (key.compareToIgnoreCase("Loading") == 0 || value.compareToIgnoreCase("channels:") == 0) {
          continue;
        }

        // First row is sometimes empty
        if (key.isEmpty()) {
          continue;
        }
        if (key.contains("Name") || key.contains("#")) {
          continue;
        }
        char c = key.charAt(0);
        if (c >= 'a' && c <= 'z') {
          foundLib = key;
          foundVersion = value;
        } else {
          foundVersion = key;
        }
        LibVersions lv = null;
        for (LibVersions v : all) {
          if (v.getLib().compareTo(foundLib) == 0) {
            lv = v;
            break;
          }
        }
        if (lv == null) {
          lv = new LibVersions(url, foundLib);
          all.add(lv);
        }
        lv.addVersion(new Version(foundVersion));

      }
      int errCode = process.waitFor();
      if (errCode == 2) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(),
            "Problem listing libraries with conda - report a bug.");
      } else if (errCode == 1) {
        throw new AppException(Response.Status.NO_CONTENT.
            getStatusCode(),
            "No results found.");
      }
      return all;

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class
          .getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Problem listing libraries, conda interrupted on this webserver.");

    }
  }

  private Collection<LibVersions> findPipLib(PythonDepJson lib) throws
          AppException {
    String envName = project.getName();
    String library = lib.getLib();
    List<LibVersions> all = new ArrayList<>();

    String prog = settings.getHopsworksDomainDir() + "/bin/pipsearch.sh";
    ProcessBuilder pb = new ProcessBuilder(prog, library, envName);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(process.
              getInputStream()));
      String line;
      library = library.toLowerCase();

      // Sample pip search format
      // go-defer (1.0.1)      - Go's defer for Python
      String[] lineSplit = null;

      while ((line = br.readLine()) != null) {

        // line could be a continuation of a comment
        // currently it is indented
        lineSplit = line.split(" +");

        if(line.length() == 0 || lineSplit.length < 2) {
          continue;
        }

        LibVersions lv = new LibVersions();

        // remove all paranthesis
        line = line.replaceAll("[()]", "");

        // split on multiple spaces
        lineSplit = line.split(" +");

        if(lineSplit.length >= 2) {

          String libName = lineSplit[0];

          if (!libName.toLowerCase().startsWith(library)) {
            continue;
          }

          lv.setLib(libName);

          lv.addVersion(new Version(lineSplit[1]));

          all.add(lv);

        }
      }
      int errCode = process.waitFor();
      if (errCode == 2) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Problem listing libraries with pip - report a bug.");
      } else if (errCode == 1) {
        throw new AppException(Response.Status.NO_CONTENT.
                getStatusCode(),
                "No results found.");
      }
      return all;

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class
              .getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Problem listing libraries, pip interrupted on this webserver.");

    }
  }
}
