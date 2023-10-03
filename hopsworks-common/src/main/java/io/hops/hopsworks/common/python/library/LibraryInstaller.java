/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.DockerFileController;
import io.hops.hopsworks.common.python.environment.DockerImageController;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.python.environment.EnvironmentHistoryController;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hadoop.fs.Path;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.naming.InitialContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LibraryInstaller {

  private static final Logger LOG = Logger.getLogger(LibraryInstaller.class.getName());

  private static final Comparator<CondaCommands> ASC_COMPARATOR = new CommandsComparator<>();

  private final AtomicInteger registryGCCycles = new AtomicInteger();

  // Docker base image info cache
  private Collection<PythonDep> baseImageDeps = null;
  private String baseImageConflictsStr = null;
  private String baseImageEnvYaml = null;

  @Resource
  private TimerService timerService;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CommandsController commandsController;
  @EJB
  private LibraryController libraryController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private Settings settings;
  @Inject
  private DockerRegistryMngr registry;
  private ManagedExecutorService executorService;
  @EJB
  private DockerImageController dockerImageController;
  @EJB
  private DockerFileController dockerFileController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @EJB
  private EnvironmentHistoryController environmentHistoryController;

  private Timer timer;

  @PostConstruct
  public void init() {
    try {
      commandsController.failAllOngoing();
      executorService = InitialContext.doLookup("concurrent/condaExecutorService");
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error looking up for the condaExecutorService", e);
      // Nothing else we can do here
    }
    schedule();
  }
  
  @PreDestroy
  private void destroyTimer() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  private void schedule() {
    timer = timerService.createSingleActionTimer(1000L, new TimerConfig("python library installer", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void isAlive() {
    try {
      if (!payaraClusterManager.amIThePrimary()) {
        return;
      }
      LOG.log(Level.FINE, "isAlive-start: " + System.currentTimeMillis());
      
      registryGCCycles.incrementAndGet();
      final List<CondaCommands> allCondaCommandsOngoing = condaCommandFacade.findByStatus(CondaStatus.ONGOING);
      // Run Registry GC every 10 seconds if there are no pending operations call registry GC, else proceed with
      // other conda ops
      if (registryGCCycles.get() % 10 == 0 && allCondaCommandsOngoing.isEmpty()) {
        registryGCCycles.set(0);

        //Run registry GC
        try {
          LOG.log(Level.FINE, "registryGC-start: " + System.currentTimeMillis());
          gc();
          LOG.log(Level.FINE, "registryGC-stop: " + System.currentTimeMillis());
        } catch (Exception ex) {
          LOG.log(Level.WARNING, "Could not run conda remove commands", ex);
        }
      } else {
        // Group new commands by project and run in parallel
        Map<Project, List<CondaCommands>> allCondaCommandsNewByProject =
          getCondaLibraryCommandsByProject(condaCommandFacade.findByStatus(CondaStatus.NEW));
        Map<Project, List<CondaCommands>> allCondaCommandsOngoingByProject =
          getCondaLibraryCommandsByProject(condaCommandFacade.findByStatus(CondaStatus.ONGOING));
        
        LOG.log(Level.FINE, "allCondaCommandsOngoingByProject:" + allCondaCommandsOngoingByProject);
        for (Project project : allCondaCommandsNewByProject.keySet()) {
          if (!allCondaCommandsOngoingByProject.containsKey(project)) {
            try {
              allCondaCommandsNewByProject.get(project).sort(ASC_COMPARATOR);
              CondaCommands commandToExecute = allCondaCommandsNewByProject.get(project).get(0);
              // check if it was not deleted...
              if (condaCommandFacade.findCondaCommand(commandToExecute.getId()) == null) {
                LOG.log(Level.FINE, "Command with ID " + commandToExecute.getId() + " not found, skipping...");
              } else {
                commandsController.updateCondaCommandStatus(
                    commandToExecute.getId(), CondaStatus.ONGOING, commandToExecute.getArg(), commandToExecute.getOp());
                executorService.submit(() -> condaCommandHandler(commandToExecute));
              }
            } catch (Exception ex) {
              LOG.log(Level.WARNING, "Could not run conda commands for project: " + project, ex);
            }
          } else {
            LOG.log(Level.FINE, "Project " + project.getName() + " is already processing a conda command, skipping...");
          }
        }
      }
    } finally {
      LOG.log(Level.FINE, "isAlive-stop: " + System.currentTimeMillis());
      schedule();
    }
  }

  private void condaCommandHandler(CondaCommands commandToExecute) {
    // Remove operations are handled differently, as we it needs to take an exclusive lock on all operations
    if (commandToExecute.getOp() != CondaOp.REMOVE) {
      try {
        try {
          switch (commandToExecute.getOp()) {
            case CREATE:
            case IMPORT:
              createNewImage(commandToExecute);
              break;
            case INSTALL:
              installLibrary(commandToExecute);
              break;
            case UNINSTALL:
              uninstallLibrary(commandToExecute);
              break;
            case EXPORT:
              exportEnvironment(commandToExecute);
              break;
            case SYNC_BASE_ENV:
              syncBaseLibraries(commandToExecute);
              break;
            case CUSTOM_COMMANDS:
              buildWithCustomCommands(commandToExecute);
              break;
            default:
              throw new UnsupportedOperationException("conda command unknown: " + commandToExecute.getOp());
          }
        } catch (Throwable ex) {
          LOG.log(Level.WARNING, "Could not execute command with ID: " + commandToExecute.getId(), ex);
          commandsController.updateCondaCommandStatus(commandToExecute.getId(), CondaStatus.FAILED,
            commandToExecute.getArg(), commandToExecute.getOp(), errorMsg(ex));
          return;
        }
        commandsController.updateCondaCommandStatus(
          commandToExecute.getId(), CondaStatus.SUCCESS, commandToExecute.getArg(), commandToExecute.getOp());
      } catch (ProjectException ex) {
        LOG.log(Level.WARNING, "Could not update command with ID: " + commandToExecute.getId(), ex);
      }
    }
  }

  
  private String errorMsg(Throwable ex) {
    return errorMsg(ex, "");
  }
  
  private String errorMsg(Throwable ex, String prefix) {
    String errorMsg = ex.getMessage();
    if(errorMsg == null) {
      errorMsg = ExceptionUtils.getStackTrace(ex);
    }
    if(Strings.isNullOrEmpty(errorMsg)) {
      errorMsg = "Command failed due to exception:" + ex.getClass().getSimpleName();
    }
    errorMsg = prefix + errorMsg;
    if(errorMsg.length() > 10000) {
      errorMsg = errorMsg.substring(0, 10000);
    }

    return errorMsg;
  }
  
  private void createNewImage(CondaCommands cc)
    throws ServiceDiscoveryException, ProjectException, PythonException, ServiceException {
    Project project = getProject(cc);
    File cwd = dockerFileController.createTmpDir(cc.getProjectId());
    try {
      String dockerFileName = "dockerFile_" + cc.getProjectId().getName();
      String baseImage = projectUtils.getFullBaseImageName();
      //If a user creates an environment from a yml file and jupyter install is not selected build on the base
      //image that does not contain a python environment
      if(!Strings.isNullOrEmpty(cc.getEnvironmentFile()) && !cc.getInstallJupyter()) {
        baseImage = baseImage.replace(settings.getBaseDockerImagePythonName(),
          settings.getBaseNonPythonDockerImage());
      }
      File dockerFile = dockerFileController.createNewImage(cwd, dockerFileName, baseImage, cc);

      String initialDockerImage = projectUtils.getInitialDockerImageName(project);
      dockerImageController.buildImage(initialDockerImage, dockerFile.getAbsolutePath(), cwd);
      updateProjectDockerImage(cc, initialDockerImage);
    } finally {
      try {
        FileUtils.deleteDirectory(cwd);
      } catch (IOException e) {
        String errorMsg = "Failed removing docker file";
        throw new ServiceException(RESTCodes.ServiceErrorCode.LOCAL_FILESYSTEM_ERROR, Level.INFO,
          errorMsg, errorMsg, e);
      }
    }
  }

  private void updateProjectDockerImage(CondaCommands cc, String dockerImage)
    throws PythonException, ServiceException, ServiceDiscoveryException, ProjectException {
    Project project = getProject(cc);
    project.setDockerImage(dockerImage);
    project = projectFacade.update(project);
    projectFacade.flushEm();
    updateInstalledDependencies(project);
    exportEnvironment(project, cc.getUserId());
  }
  
  private Project updateInstalledDependencies(Project project) throws ServiceException, PythonException {
    String dockerImage = null;
    try {
      dockerImage = projectUtils.getFullDockerImageName(project, false);
    } catch (ServiceDiscoveryException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_DISCOVERY_ERROR, Level.SEVERE, null,
        e.getMessage(), e);
    }
    
    String condaListOutput = dockerImageController.listLibraries(dockerImage);
    // Update installed dependencies
    Collection<PythonDep> projectDeps = libraryController.parseCondaList(condaListOutput);
    projectDeps = libraryController.addOngoingOperations(project.getCondaCommandsCollection(), projectDeps);
    project.setPythonDepCollection(projectDeps);
    
    // Update PIP conflicts
    String pipConflictStr = dockerImageController.checkImage(dockerImage);
    setPipConflicts(project, pipConflictStr);
    
    projectFacade.update(project);
    projectFacade.flushEm();
    return project;
  }

  private void buildWithCustomCommands(CondaCommands cc) throws ProjectException, ServiceDiscoveryException,
      ServiceException, UserException, PythonException {
    Project project = getProject(cc);
    File cwd = dockerFileController.createTmpDir(cc.getProjectId());
    boolean buildSuccess = false;
    String nextDockerImageName = getNextDockerImageName(project);
    try{
      String baseImage = projectUtils.getFullDockerImageName(project, false);
      String dockerFileName = "dockerFile_" + cc.getProjectId().getName();
      DockerFileController.BuildImageDetails customCommandsDockerFile
          = dockerFileController.installLibrary(cwd, dockerFileName, baseImage, cc);
      dockerImageController.buildImage(nextDockerImageName,
          customCommandsDockerFile.dockerFile.getAbsolutePath(),
          cwd,
          customCommandsDockerFile.dockerBuildOpts);
      updateProjectDockerImage(cc, nextDockerImageName);
      buildSuccess = true;
    } finally {
      try {
        DistributedFileSystemOps dfso = dfs.getDfsOps(cc.getProjectId(), cc.getUserId());
        try {
          String artifactDirPrefix = buildSuccess ? nextDockerImageName
              .substring(nextDockerImageName.lastIndexOf(":") + 1): String.valueOf(cc.getId());
          Path artifactPath = new Path(Utils.getProjectPath(project.getName()) +
              Settings.PROJECT_PYTHON_ENVIRONMENT_FILE_DIR,
              artifactDirPrefix + Settings.DOCKER_CUSTOM_COMMANDS_POST_BUILD_ARTIFACT_DIR_SUFFIX);
          dfso.mkdirs(artifactPath, dfso.getParentPermission(artifactPath));
          File commandsFile = new File(cc.getCustomCommandsFile());
          // If the build was success we should have a common name for the custom commands file so that
          // we can retrieve it later. The conda command is deleted
          String finalCommandsFileName =
              buildSuccess ? Settings.DOCKER_CUSTOM_COMMANDS_FILE_NAME : cc.getCustomCommandsFile();
          dfso.copyFromLocal(false, new Path(cwd.toString(), commandsFile.getName()),
              new Path(artifactPath, finalCommandsFileName));
        } catch (Exception e){
          LOG.log(Level.WARNING, "Failed copy commands file for conda build: " + cc.getId(), e.getMessage());
        } finally {
          dfs.closeDfsClient(dfso);
        }
        FileUtils.deleteDirectory(cwd);
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Failed removing docker file - " + cc.getId(), e.getMessage());
      }
    }
  }

  private void installLibrary(CondaCommands cc)
    throws ServiceException, ServiceDiscoveryException, ProjectException, UserException, PythonException {
    Project project = getProject(cc);
    File cwd = dockerFileController.createTmpDir(cc.getProjectId());
    try{
      String baseImage = projectUtils.getFullDockerImageName(project, false);
      String dockerFileName = "dockerFile_" + cc.getProjectId().getName();
      DockerFileController.BuildImageDetails installLibraryResult
        = dockerFileController.installLibrary(cwd, dockerFileName, baseImage, cc);
      String nextDockerImageName = getNextDockerImageName(project);
      dockerImageController.buildImage(nextDockerImageName,
        installLibraryResult.dockerFile.getAbsolutePath(),
        cwd,
        installLibraryResult.dockerBuildOpts,
        installLibraryResult.gitApiKeyName,
        installLibraryResult.gitApiToken);
      updateProjectDockerImage(cc, nextDockerImageName);
    } finally {
      try {
        FileUtils.deleteDirectory(cwd);
      } catch (IOException e) {
        String errorMsg = "Failed removing docker file";
        throw new ServiceException(RESTCodes.ServiceErrorCode.LOCAL_FILESYSTEM_ERROR, Level.INFO,
          errorMsg, errorMsg, e);
      }
    }
  }
  
  private void uninstallLibrary(CondaCommands cc)
    throws ServiceDiscoveryException, ProjectException, ServiceException, PythonException {
    Project project = getProject(cc);
    File cwd = dockerFileController.createTmpDir(cc.getProjectId());
    try {
      String fullDockerImageName = projectUtils.getFullDockerImageName(project, false);
      File dockerFile = dockerFileController.uninstallLibrary(cwd, fullDockerImageName, cc);
      String nextDockerImageName = getNextDockerImageName(project);
      dockerImageController.buildImage(nextDockerImageName, dockerFile.getAbsolutePath(), cwd);
      updateProjectDockerImage(cc, nextDockerImageName);
    } finally {
      try {
        FileUtils.deleteDirectory(cwd);
      } catch (IOException e) {
        String errorMsg = "Failed removing docker file";
        throw new ServiceException(RESTCodes.ServiceErrorCode.LOCAL_FILESYSTEM_ERROR, Level.INFO,
          errorMsg, errorMsg, e);
      }
    }
  }
  
  private Project getProject(CondaCommands cc) throws ProjectException {
    return projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
      RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
  }

  private String exportEnvironment(Project project, Users user)
    throws ServiceException, ServiceDiscoveryException {
    String dockerImage = projectUtils.getFullDockerImageName(project, false);
    String env = dockerImageController.exportImage(dockerImage);
    environmentController.uploadYmlInProject(project, user, env);
    environmentHistoryController.computeDelta(project, user);
    return env;
  }

  private void exportEnvironment(CondaCommands cc)
    throws ServiceException, ServiceDiscoveryException, ProjectException {

    Project project = getProject(cc);
    exportEnvironment(project, cc.getUserId());
  }

  private void syncBaseLibraries(CondaCommands cc)
      throws ServiceException, ServiceDiscoveryException, ProjectException, PythonException {
    Project project = getProject(cc);

    String dockerImage = projectUtils.getFullDockerImageName(project, true);

    Collection<PythonDep> projectDeps = getBaseImageDeps();
    if (projectDeps == null) {
      String condaListOutput = dockerImageController.listLibraries(dockerImage);
      projectDeps = libraryController.parseCondaList(condaListOutput);
      setBaseImageDeps(projectDeps);
    }
    project.setPythonDepCollection(projectDeps);

    String pipConflictsStr = getBaseImageConflictsStr();
    if (pipConflictsStr == null) {
      pipConflictsStr = getPipConflicts(dockerImage);
      setBaseImageConflictsStr(pipConflictsStr);
    }
    setPipConflicts(project, pipConflictsStr);
    projectFacade.update(project);


    String baseImageEnvYaml = getBaseImageEnvYaml();
    if (baseImageEnvYaml == null) {
      baseImageEnvYaml = exportEnvironment(project, cc.getUserId());
      setBaseImageEnvYaml(baseImageEnvYaml);
    } else {
      environmentController.uploadYmlInProject(project, cc.getUserId(), baseImageEnvYaml);
      environmentHistoryController.computeDelta(project, cc.getUserId());
    }
  }
  
  private void setPipConflicts(Project project, String conflictStr) {
    if (Strings.isNullOrEmpty(conflictStr)) {
      project.getPythonEnvironment().setJupyterConflicts(false);
      project.getPythonEnvironment().setConflicts(null);
      return;
    }
    
    ArrayList<String> conflicts = new ArrayList<>();
    String[] lines = conflictStr.split("\n");
    for (String conflictLine : lines) {
      conflicts.add(conflictLine.split("\\s+")[0].trim());
    }
    
    if (conflicts.isEmpty()) {
      project.getPythonEnvironment().setJupyterConflicts(false);
      project.getPythonEnvironment().setConflicts(null);
    } else {
      project.getPythonEnvironment().setConflicts(conflictStr.substring(0, Math.min(conflictStr.length(), 12000)));
      project.getPythonEnvironment().setJupyterConflicts(
        Settings.JUPYTER_DEPENDENCIES.stream().anyMatch(conflicts::contains));
    }
  }
  
  public String getPipConflicts(String dockerImage) throws PythonException, ServiceException {
    return dockerImageController.checkImage(dockerImage);
  }

  private String getNextDockerImageName(Project project) {
    String dockerImage = projectUtils.getDockerImageName(project, settings, false);
    int indexOfLastDigit = dockerImage.lastIndexOf(".");
    int currentVersion = Integer.parseInt(dockerImage.substring(indexOfLastDigit + 1));
    int nextVersion = currentVersion + 1;
    return dockerImage.substring(0, indexOfLastDigit) + "." + nextVersion;
  }
  
  private Map<Project, List<CondaCommands>> getCondaLibraryCommandsByProject(List<CondaCommands> condaCommands) {
    Map<Project, List<CondaCommands>> condaCommandsByProject = new HashMap<>();
    for (CondaCommands condacommand : condaCommands) {
      // If it is a command of a project that has been deleted, delete the command (do not delete an environment
      //"REMOVE" command as it needs to be processed even after the project has been deleted
      if (condacommand.getOp() != CondaOp.REMOVE &&
        (condacommand.getProjectId() == null || condacommand.getProjectId().getPythonEnvironment() == null)) {
        LOG.log(Level.FINEST, "Removing condacommand: " + condacommand);
        condaCommandFacade.remove(condacommand);
      } else if (condacommand.getOp() != CondaOp.REMOVE) {
        Project project = condacommand.getProjectId();
        if (!condaCommandsByProject.containsKey(project)) {
          condaCommandsByProject.put(project, new ArrayList<>());
        }
        condaCommandsByProject.get(project).add(condacommand);
      }
    }
    return condaCommandsByProject;
  }
  
  public void gc() throws ServiceException, ProjectException {
    // 1. Get all conda commands of type REMOVE. Should be only 1 REMOVE per project
    final List<CondaCommands> condaCommandsRemove = condaCommandFacade.findByStatusAndCondaOp(CondaStatus.NEW,
      CondaOp.REMOVE);
    LOG.log(Level.FINE, "condaCommandsRemove: " + condaCommandsRemove);
    try {
      for (CondaCommands cc : condaCommandsRemove) {
        // We do not want to remove the base image! Get arguments from command as project may have already been deleted.
        String projectDockerImage = cc.getArg();
        if (!projectUtils.dockerImageIsPreinstalled(projectDockerImage)) {
          try {
            registry.deleteProjectDockerImage(projectDockerImage);
          } catch (Exception ex) {
            LOG.log(Level.WARNING,
              "Could not complete docker registry cleanup for: " + cc, ex);
            try {
              commandsController.updateCondaCommandStatus(cc.getId(), CondaStatus.FAILED,
                cc.getArg(), cc.getOp(), errorMsg(ex, "Could not complete docker registry cleanup: "));
            } catch (ProjectException e) {
              LOG.log(Level.WARNING,
                "Could not change conda command status to NEW.", e);
            }
          }
        }
        commandsController.updateCondaCommandStatus(cc.getId(), CondaStatus.SUCCESS, cc.getArg(), cc.getOp());
      }
    } finally {
      // Run docker gc in cli
      if (!condaCommandsRemove.isEmpty()) {
        registry.runRegistryGC();
      }
    }
  }


  // Cache methods for the base environment
  public synchronized Collection<PythonDep> getBaseImageDeps() {
    return baseImageDeps;
  }

  public synchronized void setBaseImageDeps(Collection<PythonDep> baseImageDeps) {
    this.baseImageDeps = baseImageDeps;
  }

  public synchronized String getBaseImageConflictsStr() {
    return baseImageConflictsStr;
  }

  public synchronized void setBaseImageConflictsStr(String baseImageConflictsStr) {
    this.baseImageConflictsStr = baseImageConflictsStr;
  }

  public synchronized String getBaseImageEnvYaml() {
    return baseImageEnvYaml;
  }

  public synchronized void setBaseImageEnvYaml(String baseImageEnvYaml) {
    this.baseImageEnvYaml = baseImageEnvYaml;
  }
  
  private static class CommandsComparator<T> implements Comparator<T> {

    @Override
    public int compare(T t, T t1) {

      if ((t instanceof CondaCommands) && (t1 instanceof CondaCommands)) {
        return condaCommandCompare((CondaCommands) t, (CondaCommands) t1);
      } else if ((t instanceof SystemCommand) && (t1 instanceof SystemCommand)) {
        return systemCommandCompare((SystemCommand) t, (SystemCommand) t1);
      } else {
        return 0;
      }
    }

    private int condaCommandCompare(final CondaCommands t, final CondaCommands t1) {
      return t.getId().compareTo(t1.getId());
    }

    private int systemCommandCompare(final SystemCommand t, final SystemCommand t1) {
      return t.getId().compareTo(t1.getId());
    }
  }

}