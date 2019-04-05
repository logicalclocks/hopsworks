/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.python.library;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.dao.python.PythonDep;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibraryController {
  
  private static final Logger LOGGER = Logger.getLogger(LibraryController.class.getName());
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CommandsController commandsController;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private Settings settings;
  @EJB
  private LibraryFacade libraryFacade;
  
  public PythonDep getPythonDep(String dependency, Project project) {
    return libraryFacade.findByDependencyAndProject(dependency, project);
  }
  
  public void uninstallLibrary(Project project, String libName) throws ServiceException, GenericException {
    for (PythonDep dep : project.getPythonDepCollection()) {
      if (dep.getDependency().equals(libName)) {
        uninstallLibrary(project, dep.getInstallType(), dep.getMachineType(), dep.getRepoUrl().getUrl(), libName,
          dep.getVersion());
        break;
      }
    }
  }
  
  public List<PythonDep> listProject(Project proj) {
    List<PythonDep> libs = new ArrayList<>();
    Collection<PythonDep> objs = proj.getPythonDepCollection();
    if (objs != null) {
      libs.addAll(objs);
    }
    return libs;
  }
  
  public void addPythonDepsForProject(Project proj, Collection<PythonDep> pythonDeps) {
    // proj.setPythonDepCollection(pythonDeps); will overwrite any dep already in proj.
    for (PythonDep dep : pythonDeps) {
      proj.getPythonDepCollection().remove(dep);
      proj.getPythonDepCollection().add(dep);
    }
    projectFacade.update(proj);
  }
  
  public PythonDep addLibrary(Project proj, CondaCommandFacade.CondaInstallType installType,
    LibraryFacade.MachineType machineType, String channelUrl, String dependency, String version) throws ServiceException
    , GenericException {
    PythonDep dep = commandsController.condaOp(CondaCommandFacade.CondaOp.INSTALL, installType, machineType, proj,
      channelUrl, dependency, version);
    return dep;
  }
  
  public void uninstallLibrary(Project proj, CondaCommandFacade.CondaInstallType installType,
    LibraryFacade.MachineType machineType, String channelUrl, String dependency, String version)
    throws ServiceException, GenericException {
    commandsController.condaOp(CondaCommandFacade.CondaOp.UNINSTALL, installType, machineType, proj, channelUrl,
      dependency, version);
  }
  
  public HashMap<String, Set<LibraryVersionDTO>> condaSearch(String library, String url) throws ServiceException {
    
    HashMap<String, Set<LibraryVersionDTO>> libVersions = new HashMap<>();
    
    String prog = settings.getHopsworksDomainDir() + "/bin/condasearch.sh";
    ProcessBuilder pb = new ProcessBuilder(prog, url, library);
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
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
        // Skip No, match line
        } else if (key.equalsIgnoreCase("no") && value.equalsIgnoreCase("match")) {
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
        
        if (!libVersions.containsKey(foundLib)) {
          Set<LibraryVersionDTO> versions = new HashSet<>();
          versions.add(new LibraryVersionDTO(foundVersion));
          libVersions.put(foundLib, versions);
        } else {
          libVersions.get(foundLib).add(new LibraryVersionDTO(foundVersion));
        }
      }
      int errCode = process.waitFor();
      if (errCode == 2) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
          "errCode: " + errCode);
      } else if (errCode == 1) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.SEVERE,
          "errCode: " + errCode);
      }
      
      // if empty
      if (libVersions.isEmpty()) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.FINE);
      }
      
      return libVersions;
      
    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE, "lib: " + library,
        ex.getMessage(), ex);
    }
  }
  
  private void findPipLibPyPi(String libName, HashMap<String, Set<LibraryVersionDTO>> versions)
  {
    Response resp = null;
    try {
      resp = ClientBuilder.newClient()
        .target(settings.getPyPiRESTEndpoint().replaceFirst("\\{package}", libName))
        .request()
        .header("Content-Type", "application/json").get();
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "PyPi REST endpoint connection failed" + settings.getPyPiRESTEndpoint().replaceFirst(
        "\\{package}", libName), e);
      return;
    }
    
    if (resp.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
      return;
    }
    
    JSONObject jsonObject = new JSONObject(resp.readEntity(String.class));
    
    if (jsonObject.has("releases")) {
      JSONObject releases = jsonObject.getJSONObject("releases");
      versions.put(libName, getVersions(releases));
    }
  }
  
  private Set<LibraryVersionDTO> getVersions(JSONObject releases) {
    Set<LibraryVersionDTO> versions = new HashSet<>();
    Iterator<String> keys = releases.keys();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    while (keys.hasNext()) {
      String key = keys.next();
      if (releases.get(key) instanceof JSONArray) {
        JSONObject versionMeta =
          ((JSONArray) releases.get(key)).length() > 0 ? (JSONObject) ((JSONArray) releases.get(key)).get(0) : null;
        if (versionMeta != null && versionMeta.has("upload_time")) {
          Date releaseDate = null;
          String strDate = versionMeta.getString("upload_time");
          try {
            releaseDate = sdf.parse(strDate);
          } catch (ParseException e) {
            LOGGER.log(Level.FINE, "Failed to parse release date: {0}", strDate);
          }
          versions.add(new LibraryVersionDTO(key, releaseDate));
        }
      }
    }
    return versions;
  }
  
  public HashMap<String, Set<LibraryVersionDTO>> pipSearch(String library, Project project) throws ServiceException {
    
    String env = projectUtils.getCurrentCondaEnvironment(project);
    HashMap<String, Set<LibraryVersionDTO>> versions = new HashMap<>();
    
    String prog = settings.getHopsworksDomainDir() + "/bin/pipsearch.sh";
    ProcessBuilder pb = new ProcessBuilder(prog, library, env);
    try {
      Process process = pb.start();
      
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
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
        
        String libName = lineSplit[0];
        
        if (!libName.toLowerCase().startsWith(library)) {
          continue;
        }
        
        findPipLibPyPi(libName, versions);
        if (!versions.containsKey(libName)) {
          //This may happen when the version is (), i.e pip search does not return a version
          String version = lineSplit[1];
          if(version.equals("()")) {
            Set<LibraryVersionDTO> versionList = new HashSet<>();
            versionList.add(new LibraryVersionDTO(""));
            versions.put(libName, versionList);
          } else {
            version = version.replaceAll("[()]", "").trim();
            Set<LibraryVersionDTO> versionList = new HashSet<>();
            versionList.add(new LibraryVersionDTO(version));
            versions.put(libName, versionList);
          }
        }
      }
      
      boolean exited = process.waitFor(10L, TimeUnit.MINUTES);
      int errCode = process.exitValue();
      if (!exited) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
          "errCode: " + errCode);
      }
      if (errCode == 2) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
          "errCode: " + errCode);
      } else if (errCode == 1) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.SEVERE,
          "errCode: " + 1);
      }
      
      return versions;
      
    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
        "lib: " + library, ex.getMessage(), ex);
    }
  }

}
