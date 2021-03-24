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

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.python.PythonEnvironment;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private LibraryFacade libraryFacade;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  private static final Logger LOGGER = Logger.getLogger(EnvironmentController.class.getName());

  private void checkCondaSyncFinished(Project project) throws PythonException {
    List<CondaStatus> statuses = new ArrayList<>();
    statuses.add(CondaStatus.NEW);
    statuses.add(CondaStatus.ONGOING);
    statuses.add(CondaStatus.FAILED);
    List<CondaCommands> syncCommands =
        condaCommandFacade.findByStatusAndCondaOpAndProject(statuses, CondaOp.SYNC_BASE_ENV, project);

    if(!syncCommands.isEmpty()) {
      Optional<CondaCommands> ongoingSyncCommand = syncCommands.stream()
          .filter(d -> d.getStatus().name().equals(CondaStatus.NEW.name()) ||
              d.getStatus().name().equals(CondaStatus.ONGOING.name())).findFirst();
      if(ongoingSyncCommand.isPresent()) {
        throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_INITIALIZING, Level.FINE);
      }
      Optional<CondaCommands> failedSyncCommand = syncCommands.stream()
          .filter(d -> d.getStatus().name().equals(CondaStatus.FAILED.name())).findFirst();
      if(failedSyncCommand.isPresent()) {
        throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_FAILED_INITIALIZATION, Level.FINE);
      }
    }
  }

  public void checkCondaEnabled(Project project, String pythonVersion,
                                boolean syncMustBeFinished) throws PythonException {
    if (project.getPythonEnvironment() == null ||
        !pythonVersion.equals(project.getPythonEnvironment().getPythonVersion())) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }
    if(syncMustBeFinished) {
      checkCondaSyncFinished(project);
    }
  }

  public void checkCondaEnvExists(Project project, Users user) throws PythonException {
    if (project.getPythonEnvironment() == null) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }
    if (Strings.isNullOrEmpty(project.getDockerImage()) ||
        project.getDockerImage().equals(settings.getBaseDockerImagePythonName())) {
      createProjectDockerImage(project, user);
    }
  }

  public Project updateInstalledDependencies(Project project) throws ServiceException, IOException {
    try {
      String condaListOutput = libraryController.condaList(projectUtils.getFullDockerImageName(project, false));
      Collection<PythonDep> projectDeps = libraryController.parseCondaList(condaListOutput);
      projectDeps = libraryController.persistAndMarkImmutable(projectDeps);
      project = libraryController.syncProjectPythonDepsWithEnv(project, projectDeps);
      project = libraryController.addOngoingOperations(project);
      return project;
    } catch (ServiceDiscoveryException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_DISCOVERY_ERROR, Level.SEVERE, null,
        e.getMessage(), e);
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void createProjectDockerImage(Project project, Users user) {
    // Check if there is no pending CREATE op for this project
    List<CondaStatus> statuses = new ArrayList<>();
    statuses.add(CondaStatus.NEW);
    statuses.add(CondaStatus.ONGOING);
    if(!condaCommandFacade.findByStatusAndCondaOpAndProject(statuses, CondaOp.CREATE, project).isEmpty()) {
      LOGGER.log(Level.INFO, "There is already a " + CondaOp.CREATE.name() + " operation for this project.");
      return;
    }
    condaEnvironmentOp(CondaOp.CREATE, project.getPythonEnvironment().getPythonVersion(), project, user,
        project.getPythonEnvironment().getPythonVersion(), null, false);
    if(project.getPythonEnvironment() == null) {
      PythonEnvironment pythonEnvironment = new PythonEnvironment();
      pythonEnvironment.setPythonVersion(settings.getDockerBaseImagePythonVersion());
      pythonEnvironment.setProjectId(project.getId());
      project.setPythonEnvironment(pythonEnvironment);
    }
    project.setDockerImage(settings.getBaseDockerImagePythonName());
    projectFacade.update(project);
    projectFacade.flushEm();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public String createProjectDockerImageFromImport(String importPath, boolean installJupyter, Users user,
                                                   Project project) throws PythonException, ServiceException {
    if (project.getPythonEnvironment() != null) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_ALREADY_INITIALIZED, Level.FINE);
    }
    String username = hdfsUsersController.getHdfsUserName(project, user);
    String importContent = validateImportFile(new Path(importPath), username);

    if(!importPath.endsWith(".yml") && !importPath.endsWith("/requirements.txt")) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_FILE_INVALID, Level.FINE);
    }

    String pythonVersion = findPythonVersion(importContent);
    if(Strings.isNullOrEmpty(pythonVersion)) {
      pythonVersion = settings.getDockerBaseImagePythonVersion();
    }

    condaEnvironmentOp(CondaOp.IMPORT, pythonVersion, project, user, pythonVersion, importPath,
        installJupyter);
    PythonEnvironment pythonEnvironment = new PythonEnvironment();
    pythonEnvironment.setPythonVersion(pythonVersion);
    pythonEnvironment.setProjectId(project.getId());
    project.setPythonEnvironment(pythonEnvironment);
    projectFacade.update(project);
    return pythonVersion;
  }

  public void removeEnvironment(Project project) {
    commandsController.deleteCommandsForProject(project);
    project.setPythonDepCollection(new ArrayList<>());
    condaEnvironmentRemove(project, project.getOwner());
    project.setPythonEnvironment(null);
    project.setDockerImage(settings.getBaseNonPythonDockerImage());
    projectFacade.update(project);
    projectFacade.flushEm();
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
      String arg, String environmentFile, Boolean installJupyter) {
    if (projectUtils.isReservedProjectName(proj.getName())) {
      throw new IllegalStateException("Tried to execute a conda env op on a reserved project name");
    }
    CondaCommands cc = new CondaCommands(settings.getAnacondaUser(),
        user, op, CondaStatus.NEW, CondaInstallType.ENVIRONMENT, proj, pythonVersion, "", "defaults",
        new Date(), arg, environmentFile, installJupyter);
    condaCommandFacade.save(cc);
  }

  public void condaEnvironmentRemove(Project project, Users user) {
    // Do not remove conda env if project is using the base
    if (Strings.isNullOrEmpty(project.getDockerImage()) ||
        projectUtils.dockerImageIsPreinstalled(project.getDockerImage())) {
      LOGGER.log(Level.INFO, "Will not remove conda env " + project.getDockerImage()
          + " for project: " + project.getName());
      return;
    }
    List<CondaStatus> statuses = new ArrayList<>();
    statuses.add(CondaStatus.NEW);
    statuses.add(CondaStatus.ONGOING);
    if(!condaCommandFacade.findByStatusAndCondaOpAndProject(statuses, CondaOp.REMOVE, project).isEmpty()) {
      LOGGER.log(Level.INFO, "There is already a " + CondaOp.REMOVE.name() + " operation for this project.");
      return;
    }
    condaEnvironmentOp(CondaOp.REMOVE, "", project, user,
        project.getDockerImage(), null, false);
  }

  public String findPythonVersion(String ymlFile) throws PythonException {
    String foundVersion = null;
    Pattern urlPattern = Pattern.compile("(- python=(\\d+.\\d+))");
    Matcher urlMatcher = urlPattern.matcher(ymlFile);
    if (urlMatcher.find()) {
      foundVersion = urlMatcher.group(2);
    } else {
      return null;
    }
    return foundVersion;
  }

  public String[] exportEnv(Project project, Users user, String projectRelativeExportPath)
      throws PythonException {
    if (project.getPythonEnvironment() == null) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }

    Date date = new Date();

    long exportTime = date.getTime();
    String ymlPath = projectRelativeExportPath + "/" + "environment_" + exportTime + ".yml";
    condaEnvironmentOp(CondaOp.EXPORT, project.getPythonEnvironment().getPythonVersion(), project, user,
        ymlPath,  null, false);
    return new String[]{ymlPath};
  }

  public Project createEnv(Project project, Users user) throws PythonException {

    List<CondaStatus> statuses = new ArrayList<>();
    statuses.add(CondaStatus.NEW);
    statuses.add(CondaStatus.ONGOING);
    if(!condaCommandFacade.findByStatusAndCondaOpAndProject(statuses,
        CondaOp.CREATE, project).isEmpty()) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_INITIALIZING, Level.INFO);
    }
    if (project.getPythonEnvironment() != null) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_ALREADY_INITIALIZED, Level.FINE);
    }
    PythonEnvironment pythonEnvironment = new PythonEnvironment();
    pythonEnvironment.setPythonVersion(settings.getDockerBaseImagePythonVersion());
    pythonEnvironment.setProjectId(project.getId());
    project.setPythonEnvironment(pythonEnvironment);
    project.setDockerImage(settings.getBaseDockerImagePythonName());

    CondaCommands cc = new CondaCommands(settings.getAnacondaUser(), user, CondaOp.SYNC_BASE_ENV, CondaStatus.NEW,
        CondaInstallType.ENVIRONMENT, project, settings.getDockerBaseImagePythonVersion(),
        null, null, new Date(), null, null, false);

    condaCommandFacade.save(cc);
    return projectFacade.update(project);
  }

  private String validateImportFile(Path fullPath, String username) throws ServiceException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      long fileSize = udfso.getFileStatus(fullPath).getLen();

      if (fileSize < settings.getMaxEnvYmlByteSize()) {
        byte[] ymlFileInBytes = new byte[(int) fileSize];
        try (DataInputStream dis = new DataInputStream(udfso.open(fullPath))) {
          dis.readFully(ymlFileInBytes, 0, (int) fileSize);
          String ymlFileContents = new String(ymlFileInBytes);

          /*
            Exclude libraries from being installed which are not publically available on pypi but in our base
          */
          ymlFileContents = Arrays.stream(ymlFileContents.split(System.lineSeparator()))
              .filter(line -> !line.contains("jupyterlab-git"))
              .collect(Collectors.joining(System.lineSeparator()));

          udfso.rm(fullPath, false);
          udfso.create(fullPath, ymlFileContents);

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

  public String getPipConflicts(Project project) throws ServiceDiscoveryException, IOException, PythonException {

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("check")
        .addCommand(projectUtils.getFullDockerImageName(project, false))
        .redirectErrorStream(true)
        .setWaitTimeout(300L, TimeUnit.SECONDS)
        .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
//From https://github.com/pypa/pip/blob/27d8687144bf38cdaeeb1d81aa72c892b1d0ab88/src/pip/_internal/commands/check.py#L35
    if(processResult.getExitCode() == 0) {
      return null;
    } else {

      if(processResult.getStdout() != null &&
        (processResult.getStdout().contains("which is not installed")
        || processResult.getStdout().contains("has requirement"))) {
        return processResult.getStdout();
      } else {
        throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_PIP_CHECK_FAILED,
            Level.SEVERE, "Failed to run pip check: "
            + (Strings.isNullOrEmpty(processResult.getStdout()) ? "" : processResult.getStdout()));
      }
    }
  }
}
