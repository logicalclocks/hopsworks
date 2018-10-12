/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.api.pythonDeps;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.EnvironmentYmlJson;
import io.hops.hopsworks.common.dao.pythonDeps.LibVersions;
import io.hops.hopsworks.common.dao.pythonDeps.OpStatus;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepJson;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.Version;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PythonDepsService {

  private static final Logger logger = Logger.getLogger(PythonDepsService.class.
      getName());

  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private InodeFacade inodes;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private JWTHelper jWTHelper;

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  public Project getProject() {
    return project;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response index() {

    Collection<PythonDep> pythonDeps = project.getPythonDepCollection();

    List<PythonDepJson> jsonDeps = new ArrayList<>();
    for (PythonDep pd : pythonDeps) {
      jsonDeps.add(new PythonDepJson(pd));
    }

    GenericEntity<Collection<PythonDepJson>> deps = new GenericEntity<Collection<PythonDepJson>>(jsonDeps) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        deps).build();
  }

  @GET
  @Path("/destroyAnaconda")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeAnacondaEnv() throws ServiceException {
    pythonDepsFacade.removeProject(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/enable/{version}/{pythonKernelEnable}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response enable(@PathParam("version") String version,
      @PathParam("pythonKernelEnable") String pythonKernelEnable) throws ServiceException {
    Boolean enablePythonKernel = Boolean.parseBoolean(pythonKernelEnable);
    if (!enablePythonKernel) {
      // 'X' indicates that the python kernel should not be enabled in Conda
      version = version + "X";
    }
    pythonDepsFacade.createProjectInDb(project, version, PythonDepsFacade.MachineType.ALL, null);

    project.setPythonVersion(version);
    projectFacade.update(project);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("/enableYml")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response enableYml(EnvironmentYmlJson environmentYmlJson, @Context HttpServletRequest req)
    throws ServiceException, DatasetException, ProjectException {

    Users user = jWTHelper.getUserPrincipal(req);
    String username = hdfsUsersController.getHdfsUserName(project, user);

    String version = "0.0";
    Boolean enablePythonKernel = Boolean.parseBoolean(environmentYmlJson.getPythonKernelEnable());
    if (!enablePythonKernel) {
      // 'X' indicates that the python kernel should not be enabled in Conda
      version = version + "X";
    }
    String allYmlPath = environmentYmlJson.getAllYmlPath();
    String cpuYmlPath = environmentYmlJson.getCpuYmlPath();
    String gpuYmlPath = environmentYmlJson.getGpuYmlPath();

    if (allYmlPath != null && !allYmlPath.isEmpty()) {
      if (!allYmlPath.substring(allYmlPath.length() - 4, allYmlPath.length()).equals(".yml")) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_YML, Level.FINE, "wrong allYmlPath length");
      }
      String allYml = getYmlFromPath(allYmlPath, username);
      pythonDepsFacade.createProjectInDb(project, version, PythonDepsFacade.MachineType.ALL, allYml);
    } else if (cpuYmlPath != null || gpuYmlPath != null || !cpuYmlPath.isEmpty() || !gpuYmlPath.isEmpty()) {

      if (!cpuYmlPath.substring(cpuYmlPath.length() - 4, cpuYmlPath.length()).equals(".yml") || !gpuYmlPath.substring(
          gpuYmlPath.length() - 4, gpuYmlPath.length()).equals(".yml")) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_YML, Level.FINE, "misconfigured cpu/gpu " +
          "information");
      }

      String cpuYml = getYmlFromPath(cpuYmlPath, username);
      pythonDepsFacade.createProjectInDb(project, version,PythonDepsFacade.MachineType.CPU, cpuYml);

      String gpuYml = getYmlFromPath(gpuYmlPath, username);
      pythonDepsFacade.createProjectInDb(project, version, PythonDepsFacade.MachineType.GPU, gpuYml);

    } else {
      throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_YML, Level.FINE);
    }

    project.setPythonVersion(version);
    projectFacade.update(project);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/installed")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response installed() {
    String defaultRepo = settings.getCondaDefaultRepo();
    if (settings.isAnacondaEnabled()) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(defaultRepo).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(
        Response.Status.SERVICE_UNAVAILABLE).build();
  }

  @GET
  @Path("/environmentTypes")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response environmentTypes() throws ServiceException {

    JsonObjectBuilder response = Json.createObjectBuilder();

    String cpuHost = hostsFacade.findCPUHost();

    if (cpuHost != null) {
      response.add("CPU", true);
    } else {
      response.add("CPU", false);
    }

    String gpuHost = hostsFacade.findGPUHost();
    if (gpuHost != null) {
      response.add("GPU", true);
    } else {
      response.add("GPU", false);
    }

    if (cpuHost == null && gpuHost == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_FOUND, Level.WARNING,
        "Could not find any CPU or GPU host");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response.build()).build();
  }

  @GET
  @Path("/enabled")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response enabled() {
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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response remove(PythonDepJson library) throws ServiceException, GenericException {
  
    if (settings.getPreinstalledPythonLibraryNames().contains(library.getLib())) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_DEP_REMOVE_FORBIDDEN, Level.INFO,
        "library: " + library.getLib());
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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response clearCondaOps(PythonDepJson library) {

    pythonDepsFacade.clearCondaOps(project, library.getLib());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/install")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Response install(PythonDepJson library) throws ServiceException, GenericException {

    if(settings.getPreinstalledPythonLibraryNames().contains(library.getLib())) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_DEP_INSTALL_FORBIDDEN, Level.INFO,
        "library: " + library.getLib());

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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Response installOneHost(
      @PathParam("hostId") String hostId,
      PythonDepJson library) throws ServiceException {
    pythonDepsFacade.blockingCondaOp(Integer.parseInt(hostId),
        PythonDepsFacade.CondaOp.INSTALL, PythonDepsFacade.CondaInstallType.valueOf(library.getInstallType()),
        PythonDepsFacade.MachineType.valueOf(library.getMachineType()), project,
        library.getChannelUrl(), library.getLib(), library.getVersion());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/createenv")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response createEnv() throws ServiceException, ProjectException {
    pythonDepsFacade.getPreInstalledLibs();
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/removeenv")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeEnv() throws ServiceException {
    pythonDepsFacade.removeProject(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/export")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response export(@Context HttpServletRequest req) throws ServiceException {
    Users user = jWTHelper.getUserPrincipal(req);
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);

    String cpuHost = hostsFacade.findCPUHost();
    if (cpuHost != null) {
      exportEnvironment(cpuHost, "environment_cpu.yml", hdfsUser);
    }

    String gpuHost = hostsFacade.findGPUHost();
    if (gpuHost != null) {
      exportEnvironment(gpuHost, "environment_gpu.yml", hdfsUser);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/status")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response status() {
    List<OpStatus> response = pythonDepsFacade.opStatus(project);
    GenericEntity<Collection<OpStatus>> opsFound = new GenericEntity<Collection<OpStatus>>(response) {     };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(opsFound).build();

  }

  @GET
  @Path("/failedCondaOps")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getFailedCondaOps() {
    List<OpStatus> failedOps = pythonDepsFacade.getFailedCondaOpsProject(project);
    GenericEntity<Collection<OpStatus>> opsFound = new GenericEntity<Collection<OpStatus>>(failedOps) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(opsFound).build();
  }

  @GET
  @Path("/retryFailedCondaOps")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response retryFailedCondaOps() {
    pythonDepsFacade.retryFailedCondaOpsProject(project);
    List<OpStatus> response = pythonDepsFacade.opStatus(project);
    GenericEntity<Collection<OpStatus>> opsFound = new GenericEntity<Collection<OpStatus>>(response) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(opsFound).build();
  }

  /**
   *
   * @param lib
   * @return 204 if no results found, results if successful, 500 if an error
   * occurs.
   */
  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response search(PythonDepJson lib) throws ServiceException {

    Collection<LibVersions> response = null;
    List<PythonDep> installedDeps = null;

    if (lib.getInstallType().equals(PythonDepsFacade.CondaInstallType.PIP.name())) {
      response = findPipLib(lib);
      installedDeps = pythonDepsFacade.listProject(project);
    } else if (lib.getInstallType().equals(PythonDepsFacade.CondaInstallType.CONDA.name())) {
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

  private Collection<LibVersions> findCondaLib(PythonDepJson lib) throws ServiceException {
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
          throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_FORMAT_ERROR, Level.WARNING);
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
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
          "errCode: " + errCode);
      } else if (errCode == 1) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.SEVERE,
          "errCode: " + errCode);
      }
      return all;

    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
        "lib: " + lib.getLib(),
        ex.getMessage(), ex);
    }
  }

  private Collection<LibVersions> findPipLib(PythonDepJson lib) throws ServiceException {
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

        if (line.length() == 0 || lineSplit.length < 2) {
          continue;
        }

        LibVersions lv = new LibVersions();

        // remove all paranthesis
        line = line.replaceAll("[()]", "");

        // split on multiple spaces
        lineSplit = line.split(" +");

        if (lineSplit.length >= 2) {

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
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
          "errCode: " + errCode);
      } else if (errCode == 1) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.SEVERE,
          "errCode: " + 1);
      }
      return all;

    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
        "lib: " + lib.getLib(), ex.getMessage(), ex);
    }
  }

  private String getYmlFromPath(String path, String username)
    throws DatasetException, ProjectException, ServiceException {

    DsPath ymlPath = pathValidator.validatePath(this.project, path);
    ymlPath.validatePathExists(inodes, false);
    org.apache.hadoop.fs.Path fullPath = ymlPath.getFullPath();
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      //tests if the user have permission to access this path

      long fileSize = udfso.getFileStatus(fullPath).getLen();
      byte[] ymlFileInBytes = new byte[(int) fileSize];

      if (fileSize < 10000) {
        try (DataInputStream dis = new DataInputStream(udfso.open(fullPath))) {
          dis.readFully(ymlFileInBytes, 0, (int) fileSize);
          String ymlFileContents = new String(ymlFileInBytes);

          ymlFileContents = Arrays.stream(ymlFileContents.split(System.lineSeparator()))
              .filter(line -> !line.contains("mmlspark=="))
              .collect(Collectors.joining(System.lineSeparator()));

          return ymlFileContents;
        }
      } else {
        throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_YML_SIZE, Level.WARNING);
      }

    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_FROM_YML_ERROR, Level.SEVERE, "path: " + path,
        ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  private void exportEnvironment(String host, String environmentFile, String hdfsUser) throws ServiceException {

    String secretDir = DigestUtils.sha256Hex(project.getName() + hdfsUser);
    String exportPath = settings.getStagingDir() + Settings.PRIVATE_DIRS + secretDir;
    File exportDir = new File(exportPath);
    exportDir.mkdirs();

    String prog = settings.getHopsworksDomainDir() + "/bin/condaexport.sh";
    ProcessBuilder pb = new ProcessBuilder("/usr/bin/sudo", prog,
        exportPath, project.getName(), host, environmentFile, hdfsUser);

    try {
      Process process = pb.start();
      process.waitFor(180l, TimeUnit.SECONDS);
      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new IOException("A problem occurred when exporting the environment. ");
      }
    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_EXPORT_ERROR, Level.SEVERE, "host: " + host + "," +
        " environmentFile: " + environmentFile + ", " + "hdfsUser: " + hdfsUser, ex.getMessage(), ex);
    }
  }
}
