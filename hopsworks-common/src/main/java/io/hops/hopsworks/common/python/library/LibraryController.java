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
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.provenance.core.opensearch.BasicOpenSearchHit;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHelper;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHits;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.search.PyPiLibraryOpenSearchIndexer;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.search.builder.SearchSourceBuilder;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibraryController {

  private static final Logger LOGGER = Logger.getLogger(LibraryController.class.getName());
  @EJB
  private CommandsController commandsController;
  @EJB
  private Settings settings;
  @EJB
  private LibraryFacade libraryFacade;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private OpenSearchClientController provOpenSearchController;
  @EJB
  private PyPiLibraryOpenSearchIndexer pypiIndexer;

  public PythonDep getPythonDep(String dependency, Project project) throws PythonException {
    return libraryFacade.findByDependencyAndProject(dependency, project)
        .orElseThrow(() -> new PythonException(RESTCodes.PythonErrorCode.PYTHON_LIBRARY_NOT_FOUND, Level.FINE));
  }

  public void uninstallLibrary(Project project, Users user, String libName) throws GenericException, ServiceException {
    for (PythonDep dep : project.getPythonDepCollection()) {
      if (dep.getDependency().equals(libName)) {
        uninstallLibrary(project, user, dep.getInstallType(), dep.getRepoUrl(), libName, dep.getVersion());
        break;
      }
    }
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
      Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS);

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
   * @param <R> parsed opensearch item
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
   * @param <R> parsed opensearch item
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
        OpenSearchHelper.baseSearchRequest(
          index,
          settings.getOpenSearchDefaultScrollPageSize())
          .andThen(OpenSearchHelper.withPagination(offset, limit, settings.getOpenSearchDefaultScrollPageSize()));
      SearchRequest request = srF.get();
      SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
      sourceBuilder.query(OpenSearchHelper.fullTextSearch("library", query));
      request.source(sourceBuilder);
      searchResult = provOpenSearchController.search(request, handlerFactory.getHandler());
    } catch(OpenSearchException | ProvenanceException pe) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.FINE,
        "Unable to list python libraries", pe.getMessage(), pe);
    }
    return handlerFactory.checkedResult(searchResult);
  }

  public interface HandlerFactory<R, S1, S2> {
    OpenSearchHits.Handler<R, S1> getHandler();
    S2 checkedResult(Pair<Long, Try<S1>> result) throws ServiceException;

    class BaseList implements LibraryController.HandlerFactory<String[], List<String[]>, String[]> {
      public OpenSearchHits.Handler<String[], List<String[]>> getHandler() {
        OpenSearchHits.Parser<String[]> parser =
          hit -> LibraryController.tryInstance(BasicOpenSearchHit.instance(hit));
        return OpenSearchHits.handlerAddToList(parser);
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

  public static Try<String[]> tryInstance(BasicOpenSearchHit hit) {
    return Try.apply(() -> new String[] {hit.getSource().get("library").toString()});
  }
  
  public Collection<PythonDep> parseCondaList(String condaListStr) {
    Collection<PythonDep> deps = new ArrayList<>();
    String[] lines = condaListStr.split(System.getProperty("line.separator"));
    
    for (int i = 3; i < lines.length; i++) {
      String[] split = lines[i].split(" +");
      
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

      PythonDep pyDep = getOrCreateDep(channel, installType, libraryName, version,
          settings.getImmutablePythonLibraryNames().contains(libraryName));
      deps.add(pyDep);
    }
    return deps;
  }

  public Collection<PythonDep> addOngoingOperations(Collection<CondaCommands> projectCondaCommands,
                                                    Collection<PythonDep> projectDeps) {
    for(CondaCommands condaCommand: projectCondaCommands) {
      if(condaCommand.getInstallType().equals(CondaInstallType.ENVIRONMENT)
          || condaCommand.getInstallType().equals(CondaInstallType.CUSTOM_COMMANDS))
          continue;

      PythonDep pythonDep = getOrCreateDep(condaCommand.getChannelUrl(),
          condaCommand.getInstallType(),
          condaCommand.getLib(),
          condaCommand.getVersion(),
          false
      );

      if(!projectDeps.contains(pythonDep)) {
        projectDeps.add(pythonDep);
      }
    }

    return projectDeps;
  }

  public boolean isPyPiIndexed() {
    return this.pypiIndexer.isIndexed();
  }

  // Here we rely on "optimistic" concurrency control. In most cases, in most of our deployments, this method will be
  // called by a single user at the time. However, if multiple users are creating projects in parallel on a new
  // deployment, or installing the same library, or installing libraries with the same dependency(ies)
  // we might receive a ConstraintViolationException from the database. in that case, it means that the
  // row has been inserted by another thread (or Hopsworks instance) and so we need to re-fetch it from the database
  public PythonDep getOrCreateDep(String repo, CondaInstallType installType, String dependency,
                                  String version, boolean preinstalled) {
    Optional<PythonDep> depOptional = libraryFacade.get(repo, installType, dependency, version);
    if (depOptional.isPresent()) {
      return depOptional.get();
    }

    // dependency not present, try to insert it
    depOptional = libraryFacade.create(repo, installType, dependency, version, preinstalled);
    if (depOptional.isPresent()) {
      return depOptional.get();
    }

    // the library failed to insert, constraint violation, get it from the database
    return libraryFacade.get(repo, installType, dependency, version)
        .orElseGet(null);
  }
}
