/*
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
 */
package io.hops.hopsworks.common.python.environment;

import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.python.library.LibraryInstaller;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.MachineType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentController {
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private Settings settings;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private AgentController agentController;
  @EJB
  private LibraryController libraryController;
  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private CommandsController commandsController;
  @EJB
  private ElasticController elasticController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private LibraryFacade libraryFacade;
  @EJB
  private LibraryInstaller libraryInstaller;

  private static final Logger LOGGER = Logger.getLogger(EnvironmentController.class.getName());
  
  private static final DateTimeFormatter ELASTIC_INDEX_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
  
  public void checkCondaEnabled(Project project, String pythonVersion) throws PythonException {
    if (!project.getConda() || !pythonVersion.equals(project.getPythonVersion())) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }
  }
  
  public void checkCondaEnvExists(Project project, Users user)
      throws ServiceException, ProjectException, PythonException,
      ElasticException {
    if (!project.getConda()) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }
    if (!project.getCondaEnv()) {
      copyOnWriteCondaEnv(project, user);
    }
  }

  public void synchronizeDependencies(Project project, boolean createBaseEnv) throws ServiceException {
    String envName = projectUtils.getDockerImageName(project);
    Collection<PythonDep> defaultEnvDeps = libraryFacade.getBaseEnvDeps(envName);
    if (defaultEnvDeps == null || defaultEnvDeps.isEmpty()) {
      defaultEnvDeps = libraryInstaller.listLibraries(projectUtils.getFullDockerImageName(project));
      if (createBaseEnv) {
        for (PythonDep dep : defaultEnvDeps) {
          dep.setBaseEnv(envName);
        }
      }
    }
    final Collection<PythonDep> pythonDeps = agentController.persistAndMarkUnmutable(defaultEnvDeps);
    // Insert all deps in current listing
    libraryController.addPythonDepsForProject(project, pythonDeps);
  }
  
  private Collection<PythonDep> createProjectInDb(Project project, Users user, String pythonVersion,
    MachineType machineType, String environmentYml, Boolean installJupyter) throws ServiceException {
    
    if (environmentYml == null && pythonVersion.compareToIgnoreCase("2.7") != 0 && pythonVersion.
      compareToIgnoreCase("3.5") != 0 && pythonVersion.
      compareToIgnoreCase("3.6") != 0 && !pythonVersion.contains("X")) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.PYTHON_INVALID_VERSION,
        Level.INFO, "pythonVersion: " + pythonVersion);
    }
    
    List<PythonDep> all = new ArrayList<>();
    enableConda(project);
    return all;
  }
  
  private void enableConda(Project project) {
    if (project != null) {
      project.setConda(true);
      projectFacade.update(project);
      projectFacade.flushEm();
    }
  }
  
  private void setCondaEnv(Project project, boolean condaEnv) {
    project.setCondaEnv(condaEnv);
    projectFacade.mergeProject(project);
  }


  private List<Hosts> validateCondaHosts(MachineType machineType) throws ServiceException {
    List<Hosts> hosts = hostsFacade.getCondaHosts(machineType);
    if (hosts.isEmpty()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_NODES_UNAVAILABLE, Level.WARNING);
    }
    return hosts;
  }
  
  public boolean condaEnabledHosts() {
    List<Hosts> hosts = hostsFacade.getCondaHosts(MachineType.ALL);
    return !hosts.isEmpty();
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void copyOnWriteCondaEnv(Project project, Users user) throws ServiceException {
    condaEnvironmentOp(CondaOp.CREATE, project.getPythonVersion(), project, user,
      project.getPythonVersion(), MachineType.ALL, null, false, false);
    setCondaEnv(project, true);
  }
  
  /**
   *
   * @param proj
   */
  public void removeEnvironment(Project proj, Users user)
      throws ServiceException, ElasticException {
    commandsController.deleteCommandsForProject(proj);
    if (proj.getCondaEnv()) {
      condaEnvironmentRemove(proj, user);
      setCondaEnv(proj, false);
    }
    deleteKibanaIndex(proj);
    removePythonForProject(proj);
  }

  /**
   * Asynchronous execution of conda operations
   *
   * @param op
   * @param proj
   * @param pythonVersion
   * @param arg
   */
  private void condaEnvironmentOp(CondaOp op, String pythonVersion, Project proj, Users user,
      String arg, MachineType machineType, String environmentYml,
      Boolean installJupyter, boolean singleHost)
      throws ServiceException {
    if (projectUtils.isReservedProjectName(proj.getName())) {
      throw new IllegalStateException("Tried to execute a conda env op on a reserved project name");
    }
    CondaCommands cc = new CondaCommands(settings.getAnacondaUser(),
        user, op, CondaStatus.NEW, CondaInstallType.ENVIRONMENT, machineType,
        proj, pythonVersion, "", "defaults", new Date(), arg, environmentYml, installJupyter,
        projectUtils.getDockerImageName(proj));
    condaCommandFacade.save(cc);
  }
  
  private void condaEnvironmentRemove(Project proj, Users user) throws ServiceException {
    condaEnvironmentOp(CondaOp.REMOVE, "", proj, user,
      "", MachineType.ALL, null, false, false);
  }
    
  public CondaCommands getOngoingEnvCreation(Project proj) {
    List<CondaCommands> commands = condaCommandFacade.getCommandsForProject(proj);
    for (CondaCommands command : commands) {
      if ((
          command.getOp().equals(CondaOp.CREATE)) && (command.getStatus().
        equals(CondaStatus.NEW) ||
        command.getStatus().equals(CondaStatus.ONGOING))) {
        return command;
      }
    }
    return null;
  }

  private void removePythonForProject(Project proj) {
    proj.setPythonDepCollection(new ArrayList<>());
    proj.setPythonVersion("");
    proj.setConda(false);
    projectFacade.update(proj);
  }
  
  public String findPythonVersion(String ymlFile) throws PythonException {
    String foundVersion = null;
    Pattern urlPattern = Pattern.compile("(- python=(\\d+.\\d+))");
    Matcher urlMatcher = urlPattern.matcher(ymlFile);
    if (urlMatcher.find()) {
      foundVersion = urlMatcher.group(2);
    } else {
      throw new PythonException(RESTCodes.PythonErrorCode.YML_FILE_MISSING_PYTHON_VERSION, Level.FINE);
    }
    return foundVersion;
  }
    
  public String exportEnv(Project project, Users user, String projectRelativeExportPath)
      throws PythonException, ServiceException {
    if (!project.getConda()) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }

    Date date = new Date();

    long exportTime = date.getTime();
    String ymlPath = projectRelativeExportPath + "/" + "environment_" + exportTime + ".yml";
    condaEnvironmentOp(CondaOp.EXPORT, project.getPythonVersion(), project, user,
        ymlPath, MachineType.CPU, null, false, true);
    return ymlPath;
  }
  
  public void createEnv(Project project, Users user, String version, boolean createBaseEnv) throws PythonException,
      ServiceException, ProjectException {
    if (project.getConda() || project.getCondaEnv()) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_ALREADY_INITIALIZED, Level.FINE);
    }
    createProjectInDb(project, user, version, MachineType.ALL, null, false);
    project.setPythonVersion(version);
    projectFacade.update(project);
    synchronizeDependencies(project, createBaseEnv);
  }
  
  private String getYmlFromPath(Path fullPath, String username) throws ServiceException {
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
          
          /* Exclude libraries from being installed.
            mmlspark because is not distributed on PyPi
            Jupyter, Sparkmagic and hdfscontents because if users want to use Jupyter they should
            check the "install jupyter" option
          */
          ymlFileContents = Arrays.stream(ymlFileContents.split(System.lineSeparator()))
            .filter(line -> !line.contains("jupyter"))
            .filter(line -> !line.contains("sparkmagic"))
            .filter(line -> !line.contains("hdfscontents"))
            .collect(Collectors.joining(System.lineSeparator()));
          return ymlFileContents;
        }
      } else {
        throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_YML_SIZE, Level.WARNING);
      }
      
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_FROM_YML_ERROR, Level.SEVERE, "path: " + fullPath,
        ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public void createKibanaIndex(Project project, Users user)
      throws ServiceException,
      ProjectException, ElasticException {
    String indexName = project.getName().toLowerCase() + Settings.ELASTIC_KAGENT_INDEX_PATTERN.replace("*",
        LocalDateTime.now().format(ELASTIC_INDEX_FORMATTER));
    if (!elasticController.indexExists(indexName)) {
      elasticController.createIndex(indexName);
    }
    // Kibana index pattern for conda commands logs
    elasticController.createIndexPattern(project, user,
            project.getName().toLowerCase() + Settings.ELASTIC_KAGENT_INDEX_PATTERN);
  }

  private void deleteKibanaIndex(Project project)
      throws ElasticException {
    String indexName = project.getName().toLowerCase() + Settings.ELASTIC_KAGENT_INDEX_PATTERN;
    elasticController.deleteIndex(indexName);
    elasticController.deleteIndexPattern(project, indexName);
  }
  
  public void uploadYmlInProject(Project project, Users user, String environmentYml, String relativePath)
      throws ServiceException {
    DistributedFileSystemOps udfso = null;
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    try {
      udfso = dfs.getDfsOps(hdfsUser);
      Path projectYmlPath = new Path(Utils.getProjectPath(project.getName()) + "/" + relativePath);
      udfso.create(projectYmlPath, environmentYml);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_EXPORT_ERROR,
          Level.SEVERE, "path: " + relativePath, ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
}
