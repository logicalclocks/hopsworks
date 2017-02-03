package io.hops.hopsworks.api.pythonDeps;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.LibVersions;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepJson;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.Version;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.WebCommunication;
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
  private UserManager userManager;
  @EJB
  private WebCommunication web;
  @EJB
  private Settings settings;

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

    Collection<PythonDep> pythonDeps = project.getPythonDepCollection();

    List<PythonDepJson> jsonDeps = new ArrayList<>();
    for (PythonDep pd : pythonDeps) {
      jsonDeps.add(new PythonDepJson(pd));
    }

    GenericEntity<Collection<PythonDepJson>> deps
            = new GenericEntity<Collection<PythonDepJson>>(jsonDeps) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            deps).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/remove")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response remove(PythonDepJson library) throws AppException {

    pythonDepsFacade.removeLibrary(project,
            library.getChannelUrl(),
            library.getLib(), library.getVersion());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/install")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response install(PythonDepJson library) throws AppException {

    Collection<LibVersions> response = findCondaLib(library);
    if (response !=null && response.size() == 1) {
      LibVersions lv = response.iterator().next();
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(),
              "Go to 'Manage Installed Libraries' tab. This python library is "
                      + "already installed with state: " + lv.getStatus());
    }

    pythonDepsFacade.addLibrary(project,
            library.getChannelUrl(),
            library.getLib(), library.getVersion());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/upgrade")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response upgrade(PythonDepJson library) throws AppException {

    pythonDepsFacade.upgradeLibrary(project,
            library.getChannelUrl(),
            library.getLib(), library.getVersion());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/clone/{projectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response doClone(
          @PathParam("projectName") String srcProject,
          @PathParam("projectName") String destProject,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    pythonDepsFacade.cloneProject(srcProject, destProject);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/createenv/{projectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createEnv(@PathParam("projectName") String projectName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    pythonDepsFacade.createProject(projectName);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/removeenv/{projectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeEnv(@PathParam("projectName") String projectName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    pythonDepsFacade.removeProject(projectName);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  /**
   * 
   * @param sc
   * @param req
   * @param httpHeaders
   * @param lib
   * @return 204 if no results found, results if successful, 500 if an error occurs.
   * @throws AppException 
   */
  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response search(@Context SecurityContext sc,
          @Context HttpServletRequest req,
          @Context HttpHeaders httpHeaders,
          PythonDepJson lib) throws AppException {

    Collection<LibVersions> response = findCondaLib(lib);
//    List<PythonDep> installedDeps = pythonDepsFacade.listProject(project);
    List<PythonDep> installedDeps = new ArrayList<PythonDep>();

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
              v.setStatus("Installed");
              l.setStatus("Installed");
              break;
            }
          }
        }
      }
    }

    GenericEntity<Collection<LibVersions>> libsFound
            = new GenericEntity<Collection<LibVersions>>(response) {};

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
        // pandas-datareader,0.2.0
        // 0.2.0,py34_0
        //....
        // pandasql,0.3.1
        // 0.3.1,np16py27_0
        //....
        // 0.4.2,np18py33_0
        // 
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
      }
      else if (errCode == 1) {
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
}
