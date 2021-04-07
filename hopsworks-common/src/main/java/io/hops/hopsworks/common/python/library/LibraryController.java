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

import com.lambdista.util.Try;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHits;
import io.hops.hopsworks.common.elastic.ElasticClientController;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.search.PyPiLibraryElasticIndexer;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.AnacondaRepo;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
  private Settings settings;
  @EJB
  private LibraryFacade libraryFacade;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private ElasticClientController provElasticController;
  @EJB
  private PyPiLibraryElasticIndexer pypiIndexer;

  public PythonDep getPythonDep(String dependency, Project project) {
    return libraryFacade.findByDependencyAndProject(dependency, project);
  }

  public void uninstallLibrary(Project project, Users user, String libName) throws GenericException, ServiceException {
    for (PythonDep dep : project.getPythonDepCollection()) {
      if (dep.getDependency().equals(libName)) {
        uninstallLibrary(project, user,  dep.getInstallType(), dep.getRepoUrl().getUrl(), libName,
          dep.getVersion());
        break;
      }
    }
  }

  public Project syncProjectPythonDepsWithEnv(Project proj, Collection<PythonDep> newDeps) {
    proj.setPythonDepCollection(newDeps);
    proj = projectFacade.update(proj);
    projectFacade.flushEm();
    return proj;
  }

  public PythonDep installLibrary(Project proj, Users user, CondaInstallType installType, String channelUrl,
                              String dependency, String version, String arg, GitBackend gitBackend, String apiKeyName)
      throws GenericException, ServiceException {
    return commandsController.condaOp(CondaOp.INSTALL, user, installType, proj,
      channelUrl, dependency, version, arg, gitBackend, apiKeyName);
  }

  public void uninstallLibrary(Project proj, Users user, CondaInstallType installType, String channelUrl,
                               String dependency, String version) throws GenericException, ServiceException {
    commandsController.condaOp(CondaOp.UNINSTALL, user, installType, proj, channelUrl,
      dependency, version, "", null, null);
  }

  public HashMap<String, List<LibraryVersionDTO>> condaSearch(String library, String url) throws ServiceException {
    HashMap<String, List<LibraryVersionDTO>> libVersions = new HashMap<>();
    String prog = settings.getHopsworksDomainDir() + "/bin/condasearch.sh";
    String[] lines = condaList(prog, library, url);
    String[] libVersion;
    String foundLib = "";
    String foundVersion;
    for (String line : lines) {
      libVersion = line.split(",");
      if (libVersion.length != 2) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_FORMAT_ERROR, Level.WARNING);
      }
      String key = libVersion[0];
      String value = libVersion[1];
      // Output searching for 'dropbox' looks like this:
      // Loading,channels:
      // #,Name
      // dropbox,1.5.1
      // dropbox,8.4.0
      // ...
      // or for '4ti2'
      // Loading,channels:
      // #,Name
      // 4ti2,1.6.9
      // or for something with no match
      // Loading,channels:
      // No,match
      // Skip the first line, Skip No, match line, First row is sometimes empty
      if (key.isEmpty() ||
        (key.equalsIgnoreCase("Loading") && value.equalsIgnoreCase("channels:")) ||
        (key.equalsIgnoreCase("#") && value.equalsIgnoreCase("Name")) ||
        (key.equalsIgnoreCase("no") && value.equalsIgnoreCase("match"))) {
        continue;
      }

      foundLib = key;
      foundVersion = value;
      if (!libVersions.containsKey(foundLib)) {
        List<LibraryVersionDTO> versions = new LinkedList<>();
        versions.add(new LibraryVersionDTO(foundVersion));
        libVersions.put(foundLib, versions);
      } else {
        LibraryVersionDTO libraryVersionDTO = new LibraryVersionDTO(foundVersion);
        if (!libVersions.get(foundLib).contains(libraryVersionDTO)) {
          libVersions.get(foundLib).add(0, libraryVersionDTO);
        }
      }
    }
    // if empty
    if (libVersions.isEmpty()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.FINE);
    }
    return libVersions;
  }

  private void findPipLibPyPi(String libName, HashMap<String, List<LibraryVersionDTO>> versions) {
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

  private List<LibraryVersionDTO> getVersions(JSONObject releases) {
    List<LibraryVersionDTO> versions = new ArrayList<>();
    Iterator<String> keys = releases.keys();
    Date releaseDate = new Date(0);
    while (keys.hasNext()) {
      String key = keys.next();
      if (releases.get(key) instanceof JSONArray) {
        JSONObject versionMeta =
          ((JSONArray) releases.get(key)).length() > 0 ? (JSONObject) ((JSONArray) releases.get(key)).get(0) : null;
        if (versionMeta != null && versionMeta.has("upload_time")) {
          String strDate = versionMeta.getString("upload_time");
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
          try {
            releaseDate = sdf.parse(strDate);
          } catch (ParseException e) {
            LOGGER.log(Level.FINE, "Failed to parse release date: {0}", strDate);
          }
        }
      }
      versions.add(new LibraryVersionDTO(key, releaseDate));
    }
    return versions;
  }

  public HashMap<String, List<LibraryVersionDTO>> pipSearch(String query)
    throws ServiceException {

    HashMap<String, List<LibraryVersionDTO>> versions = new HashMap<>();

    String[] lines = pipList(query, new HandlerFactory.BaseList(),
      Settings.ELASTIC_PYPI_LIBRARIES_ALIAS);

    for (String library : lines) {
      findPipLibPyPi(library, versions);
    }

    // if empty
    if (versions.isEmpty()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.FINE);
    }
    return versions;
  }

  private String[] condaList(String program, String library, String url)
    throws ServiceException {
    ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder();
    pdBuilder.addCommand(program);
    if (url != null && !url.isEmpty()) {
      pdBuilder.addCommand(url);
      pdBuilder.addCommand(library);
    } else if(library != null && !library.isEmpty()) {
      pdBuilder.addCommand(library);
    }
    ProcessResult processResult;
    ProcessDescriptor processDescriptor = pdBuilder.redirectErrorStream(true)
      .setWaitTimeout(5L, TimeUnit.MINUTES)
      .build();
    try {
      processResult = osProcessExecutor.execute(processDescriptor);
      boolean exited = processResult.processExited();
      int errCode = processResult.getExitCode();
      if (!exited) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.WARNING,
          "errCode: " + errCode + ", " + processResult.getStderr());
      }
      if (errCode == 1 || errCode == 23) { // 1 for conda, 23 for pip
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_NOT_FOUND, Level.FINE,
          "errCode: " + errCode + ", " + processResult.getStderr());
      } else if (errCode != 0) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.WARNING,
          "errCode: " + errCode + ", " + processResult.getStderr());
      }
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.WARNING,
        "lib: " + library, ex.getMessage(), ex);
    }
    String result = processResult.getStdout();
    return (result != null && !result.isEmpty())? result.split("\n") : new String[0];
  }

  /**
   * @param <R> parsed elastic item
   * @param <S1> intermediate result wrapped in Try
   * @param <S2> final result
   * @return
   * @throws ProvenanceException
   */
  public <R, S1, S2> S2 pipList(String query, LibraryController.HandlerFactory<R, S1, S2> handlerFactory,
                                String index)
    throws ServiceException {
    return pipList(query, 0, 10, handlerFactory, index);
  }

  /**
   * @param <R> parsed elastic item
   * @param <S1> intermediate result wrapped in Try
   * @param <S2> final result
   * @return
   * @throws ProvenanceException
   */
  private <R, S1, S2> S2 pipList(String query, Integer offset, Integer limit,
                                 LibraryController.HandlerFactory<R, S1, S2> handlerFactory, String index)
    throws ServiceException {
    Pair<Long, Try<S1>> searchResult;
    try {
      CheckedSupplier<SearchRequest, ProvenanceException> srF =
        ElasticHelper.baseSearchRequest(
          index,
          settings.getElasticDefaultScrollPageSize())
          .andThen(ElasticHelper.withPagination(offset, limit, settings.getElasticDefaultScrollPageSize()));
      SearchRequest request = srF.get();
      SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
      sourceBuilder.query(ElasticHelper.fullTextSearch("library", query));
      request.source(sourceBuilder);
      searchResult = provElasticController.search(request, handlerFactory.getHandler());
    } catch(ElasticException | ProvenanceException pe) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.FINE,
        "Unable to list python libraries", pe.getMessage(), pe);
    }
    return handlerFactory.checkedResult(searchResult);
  }

  public interface HandlerFactory<R, S1, S2> {
    ElasticHits.Handler<R, S1> getHandler();
    S2 checkedResult(Pair<Long, Try<S1>> result) throws ServiceException;

    class BaseList implements LibraryController.HandlerFactory<String[], List<String[]>, String[]> {
      public ElasticHits.Handler<String[], List<String[]>> getHandler() {
        ElasticHits.Parser<String[]> parser =
          hit -> LibraryController.tryInstance(BasicElasticHit.instance(hit));
        return ElasticHits.handlerAddToList(parser);
      }

      public String[] checkedResult(Pair<Long, Try<List<String[]>>> result) throws ServiceException {
        try {
          ArrayList<String> hits = new ArrayList<>();
          for(String[] lib: result.getValue1().checkedGet()) {
            hits.add(lib[0]);
          }
          return hits.toArray(new String[0]);
        } catch (Throwable t) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.WARNING,
            "Failed to list libraries");
        }
      }
    }
  }

  public static Try<String[]> tryInstance(BasicElasticHit hit) {
    return Try.apply(() -> new String[] {hit.getSource().get("library").toString()});
  }
  
  public Collection<PythonDep> parseCondaList(String condaListStr) throws ServiceException {
    Collection<PythonDep> deps = new ArrayList<>();
    
    String[] lines = condaListStr.split(System.getProperty("line.separator"));
    
    for (int i = 3; i < lines.length; i++) {
      
      String line = lines[i];
      String[] split = line.split(" +");
      
      String libraryName = split[0];
      String version = split[1];
      String channel = "conda";
      if (split.length > 3) {
        channel = split[3].trim().isEmpty() ? channel : split[3];
      }
      
      CondaInstallType installType = CondaInstallType.PIP;
      if (!(channel.equals("pypi"))) {
        installType = CondaInstallType.CONDA;
      }
      AnacondaRepo repo = libraryFacade.getRepo(channel, true);
      boolean cannotBeRemoved = channel.equals("default");
      PythonDep pyDep = libraryFacade.getOrCreateDep(repo, installType, libraryName, version,
        false, cannotBeRemoved);
      deps.add(pyDep);
    }
    return deps;
  }

  public Project addOngoingOperations(Project project) throws ServiceException {
    Collection<CondaCommands> commands = project.getCondaCommandsCollection();
    for(CondaCommands condaCommand: commands) {
      if(condaCommand.getInstallType().equals(CondaInstallType.ENVIRONMENT))
          continue;

      PythonDep pythonDep = new PythonDep();
      pythonDep.setDependency(condaCommand.getLib());
      pythonDep.setInstallType(condaCommand.getInstallType());
      pythonDep.setPreinstalled(false);
      AnacondaRepo repo = libraryFacade.getRepo(condaCommand.getChannelUrl(), false);
      pythonDep.setRepoUrl(repo);
      pythonDep.setVersion(condaCommand.getVersion());
      pythonDep = libraryFacade.getOrCreateDep(pythonDep);

      if(!project.getPythonDepCollection().contains(pythonDep)) {
        project.getPythonDepCollection().add(pythonDep);
      }
    }
    return projectFacade.update(project);
  }

  /**
   * For each library in the conda environment figure out if it should be marked as unmutable.
   *
   * @param pyDepsInImage
   * @return
   */
  public Collection<PythonDep> persistAndMarkImmutable(Collection<PythonDep> pyDepsInImage) {
    Collection<PythonDep> deps = new ArrayList();
    for (PythonDep dep: pyDepsInImage) {
      String libraryName = dep.getDependency();
      if (settings.getImmutablePythonLibraryNames().contains(libraryName)) {
        PythonDep pyDep = libraryFacade.getOrCreateDep(dep.getRepoUrl(), dep.getInstallType(), libraryName,
            dep.getVersion(), true, true);
        deps.add(pyDep);
      } else {
        PythonDep pyDep = libraryFacade.getOrCreateDep(dep);
        deps.add(pyDep);
      }
    }
    return deps;
  }

  public String condaList(String dockerImage) throws IOException, ServiceException {

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(prog)
      .addCommand("list")
      .addCommand(dockerImage)
      .redirectErrorStream(true)
      .setWaitTimeout(30, TimeUnit.MINUTES)
      .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String devMsg = "Could list libraries in the docker image. Exit code: " + processResult.getExitCode()
        + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
      String errorMsg = "Failed to list libraries for the environment, if the issue persists" +
        " please retry the command or recreate the environment";
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_IMAGE_CREATION_ERROR,
        Level.SEVERE, errorMsg, devMsg);
    } else {
      return processResult.getStdout();
    }
  }

  public boolean isPyPiIndexed() {
    return this.pypiIndexer.isIndexed();
  }
}
