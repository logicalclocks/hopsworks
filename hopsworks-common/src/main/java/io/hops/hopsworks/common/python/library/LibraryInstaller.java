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
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.naming.InitialContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LibraryInstaller {

  private static final Logger LOG = Logger.getLogger(LibraryInstaller.class.getName());

  private static final Comparator<CondaCommands> ASC_COMPARATOR = new CommandsComparator<>();

  private final AtomicInteger registryGCCycles = new AtomicInteger();
  private String prog;
  private String anaconda_dir;
  private String anaconda_project_dir;

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
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private Settings settings;
  @Inject
  private DockerRegistryMngr registry;
  private ManagedExecutorService executorService;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private SecretsController secretsController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  
  @PostConstruct
  public void init() {
    prog =  settings.getSudoersDir() + "/dockerImage.sh";
    anaconda_dir = settings.getAnacondaDir();
    anaconda_project_dir = anaconda_dir + "/envs/" + settings.getCurrentCondaEnvironment();
    // Set all ONGOING to FAILED with an error message
    List<CondaCommands> allOngoing = condaCommandFacade.findByStatus(CondaStatus.ONGOING);
    allOngoing.forEach(cc -> {
      cc.setStatus(CondaStatus.FAILED);
      cc.setErrorMsg("Could not run conda command due to internal server error. Please try again.");
      condaCommandFacade.update(cc);
    });
    try {
      executorService = InitialContext.doLookup("concurrent/condaExecutorService");
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error looking up for the condaExecutorService", e);
      // Nothing else we can do here
    }

    schedule();
  }

  private void schedule() {
    timerService.createSingleActionTimer(1000L, new TimerConfig("python library installer", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void isAlive() {
    try {
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
          registry.gc();
          LOG.log(Level.FINE, "registryGC-stop: " + System.currentTimeMillis());
        } catch (Exception ex) {
          LOG.log(Level.WARNING, "Could not run conda remove commands", ex);
        }
      } else {
        // Group new commands by project and run in parallel
        Map<Project, List<CondaCommands>> allCondaCommandsNewByProject =
          getCondaCommandsByProject(condaCommandFacade.findByStatus(CondaStatus.NEW));
        Map<Project, List<CondaCommands>> allCondaCommandsOngoingByProject =
          getCondaCommandsByProject(condaCommandFacade.findByStatus(CondaStatus.ONGOING));
        
        LOG.log(Level.FINE, "allCondaCommandsOngoingByProject:" + allCondaCommandsOngoingByProject);
        for (Project project : allCondaCommandsNewByProject.keySet()) {
          if (!allCondaCommandsOngoingByProject.containsKey(project)) {
            try {
              executorService.submit(() -> condaCommandHandler(allCondaCommandsNewByProject.get(project)));
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

  private void condaCommandHandler(List<CondaCommands> projectCondaCommands) {
    projectCondaCommands.sort(ASC_COMPARATOR);
    for (final CondaCommands cc : projectCondaCommands) {
      // Remove operations are handled differently, as we it needs to take an exclusive lock on all operations
      if (cc.getOp() != CondaOp.REMOVE) {
        try {
          try {
            commandsController.updateCondaCommandStatus(
                cc.getId(), CondaStatus.ONGOING, cc.getArg(), cc.getOp());
            switch (cc.getOp()) {
              case CREATE:
              case IMPORT:
                createNewImage(cc);
                break;
              case INSTALL:
                installLibrary(cc);
                break;
              case UNINSTALL:
                uninstallLibrary(cc);
                break;
              case EXPORT:
                exportLibraries(cc);
                break;
              case SYNC_BASE_ENV:
                syncBaseLibraries(cc);
                break;
              default:
                throw new UnsupportedOperationException("conda command unknown: " + cc.getOp());
            }
          } catch (Throwable ex) {
            LOG.log(Level.WARNING, "Could not execute command with ID: " + cc.getId(), ex);
            commandsController.updateCondaCommandStatus(
                cc.getId(), CondaStatus.FAILED, cc.getArg(), cc.getOp(), ex.toString());
            continue;
          }
          commandsController.updateCondaCommandStatus(
              cc.getId(), CondaStatus.SUCCESS, cc.getArg(), cc.getOp());
        } catch (ServiceException | ProjectException ex) {
          LOG.log(Level.WARNING, "Could not update command with ID: " + cc.getId(), ex);
        }
      }
    }
  }
  private void createNewImage(CondaCommands cc) throws IOException, ServiceDiscoveryException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    try {
      File home = new File(System.getProperty("user.home"));
      File condarc = new File(home, ".condarc");
      File pip = new File(home, ".pip");
      FileUtils.copyFileToDirectory(condarc, baseDir);
      FileUtils.copyDirectoryToDirectory(pip, baseDir);
      File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        String baseImage = projectUtils.getFullBaseImageName();
        //If a user creates an environment from a yml file and jupyter install is not selected build on the base
        //image that does not contain a python environment
        if(!Strings.isNullOrEmpty(cc.getEnvironmentFile()) && !cc.getInstallJupyter()) {
          baseImage = baseImage.replace(settings.getBaseDockerImagePythonName(),
              settings.getBaseNonPythonDockerImage());
        }
        writer.write("# syntax=docker/dockerfile:experimental");
        writer.newLine();
        writer.write("FROM " + baseImage);
        writer.newLine();
        // If new image is created from an IMPORT (.yml or requirements.txt) file
        // copy it in the image, create the env and delete it
        if (!Strings.isNullOrEmpty(cc.getEnvironmentFile())) {
          writer.write("RUN rm -f /root/.condarc");
          writer.newLine();
          // Materialize IMPORT FILE
          String environmentFilePath = cc.getEnvironmentFile();
          String environmentFile = FilenameUtils.getName(environmentFilePath);

          writer.write("COPY .condarc .pip " + environmentFile + " /root/");
          writer.newLine();
          copyCondaArtifactToLocal(environmentFilePath, baseDir + File.separator + environmentFile);

          if(environmentFilePath.endsWith(".yml")) {
            if(cc.getInstallJupyter()) {
              writer.write("RUN conda env update -f /root/" + environmentFile + " -n "
                  + settings.getCurrentCondaEnvironment());
            } else {
              writer.write("RUN conda env create -f /root/" + environmentFile + " -p "
                  + anaconda_project_dir);
            }
          } else if(environmentFilePath.endsWith("/requirements.txt")) {
            if(cc.getInstallJupyter()) {
              writer.write("RUN pip install -r /root/" + environmentFile);
            } else {
              writer.write("RUN conda create -y -p " + anaconda_project_dir
                  + " python=" + settings.getDockerBaseImagePythonVersion()
                  + " && pip install -r /root/" + environmentFile);
            }
          }
          writer.write(" && " + getCleanupCommand()  + " && " + anaconda_dir + "/bin/conda list -n "
              + settings.getCurrentCondaEnvironment());
        }
      }
  
      Project project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
        RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
      String initialDockerImage = projectUtils.getInitialDockerImageName(project);
      LOG.log(Level.FINEST, "project-initialDockerImage:" + initialDockerImage);
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand("create")
          .addCommand(dockerFile.getAbsolutePath())
          .addCommand(projectUtils.getRegistryURL() + "/" + initialDockerImage)
          .redirectErrorStream(true)
          .setCurrentWorkingDirectory(baseDir)
          .setWaitTimeout(1, TimeUnit.HOURS)
          .build();

      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new IOException(errorMsg);
      } else {
        project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
            RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
        project.setDockerImage(initialDockerImage);
        project = projectFacade.update(project);
        projectFacade.flushEm();
        setPipConflicts(project);
        environmentController.updateInstalledDependencies(project);
      }
    } catch (ServiceException | ProjectException | PythonException e) {
      LOG.log(Level.SEVERE, "Failed to persist python deps", e);
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }

  private String getCleanupCommand() {
    return anaconda_dir + "/bin/conda clean -afy && rm -rf ~/.cache && rm -rf /usr/local/share/.cache";
  }

  private void installLibrary(CondaCommands cc)
      throws IOException, ServiceException, ServiceDiscoveryException, ProjectException, UserException,
      PythonException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    Project project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
      RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
    try {
      File home = new File(System.getProperty("user.home"));
      File condarc = new File(home, ".condarc");
      File pip = new File(home, ".pip");
      FileUtils.copyFileToDirectory(condarc, baseDir);
      FileUtils.copyDirectoryToDirectory(pip, baseDir);
      File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
      String apiToken = null;
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        writer.write("# syntax=docker/dockerfile:experimental");
        writer.newLine();
        writer.write("FROM " + projectUtils.getFullDockerImageName(project, false));
        writer.newLine();
        writer.write(
            "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
                + " --mount=type=bind,source=.pip,target=/root/.pip ");
        switch (cc.getInstallType()) {
          case CONDA:
            String condaLib = cc.getVersion().equals(Settings.UNKNOWN_LIBRARY_VERSION) ?
              cc.getLib() : cc.getLib() + "=" + cc.getVersion();
            writer.write(anaconda_dir + "/bin/conda install -y -n " + settings.getCurrentCondaEnvironment()
              + " -c " + cc.getChannelUrl() + " " + condaLib);
            break;
          case PIP:
            String pipLib = cc.getVersion().equals(Settings.UNKNOWN_LIBRARY_VERSION) ?
              cc.getLib() : cc.getLib() + "==" + cc.getVersion();
            writer.write(anaconda_project_dir + "/bin/pip install --upgrade " + pipLib);
            break;
          case EGG:
            String eggName = cc.getLib();
            String localEggPath = baseDir + File.separator + eggName;
            copyCondaArtifactToLocal(cc.getArg(), localEggPath);
            writer.write("--mount=type=bind,source="+eggName+",target=/root/" + eggName + " ");
            writer.write(anaconda_project_dir + "/bin/easy_install --upgrade /root/" + eggName);
            break;
          case WHEEL:
            String wheelName = cc.getLib();
            String localWheelPath = baseDir + File.separator + wheelName;
            copyCondaArtifactToLocal(cc.getArg(), localWheelPath);
            writer.write("--mount=type=bind,source="+wheelName+",target=/root/" + wheelName + " ");
            writer.write(anaconda_project_dir + "/bin/pip install --upgrade /root/" + wheelName);
            break;
          case REQUIREMENTS_TXT:
            String requirementsName = cc.getLib();
            String localRequirementsName = baseDir + File.separator + requirementsName;
            copyCondaArtifactToLocal(cc.getArg(), localRequirementsName);
            writer.write("--mount=type=bind,source="+requirementsName+",target=/root/" + requirementsName + " ");
            writer.write(anaconda_project_dir + "/bin/pip install -r /root/" + requirementsName);
            break;
          case ENVIRONMENT_YAML:
            String environmentsName = cc.getLib();
            String localEnvironmentsName = baseDir + File.separator + environmentsName;
            copyCondaArtifactToLocal(cc.getArg(), localEnvironmentsName);
            writer.write("--mount=type=bind,source="+environmentsName+",target=/root/" + environmentsName + " ");
            writer.write(anaconda_dir + "/bin/conda env update -f /root/" + environmentsName + " -n "
                + settings.getCurrentCondaEnvironment());
            break;
          case GIT:
            if(cc.getGitBackend() != null && cc.getGitApiKeyName() != null) {
              apiToken = this.secretsController.get
                  (cc.getUserId(), cc.getGitApiKeyName()).getPlaintext();
              URL repoUrl = new URL(cc.getArg());
              if(cc.getGitBackend().equals(GitBackend.GITHUB)) {
                writer.write(anaconda_project_dir + "/bin/pip install --upgrade 'git+https://"
                    + apiToken + ":x-oauth-basic@" + repoUrl.getHost() + repoUrl.getPath() + "'");
              } else if(cc.getGitBackend().equals(GitBackend.GITLAB)) {
                writer.write(anaconda_project_dir + "/bin/pip install --upgrade 'git+https://oauth2:"
                    + apiToken  + "@" + repoUrl.getHost() + repoUrl.getPath() + "'");
              }
            } else {
              writer.write(anaconda_project_dir + "/bin/pip install --upgrade 'git+" + cc.getArg() + "'");
            }
            break;
          case ENVIRONMENT:
          default:
            throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
        }
        //Installing faulty libraries like broken .egg files can cause the list operation to fail
        //As we find library names and versions using that command we need to make sure it does not break
        writer.write(" && " + getCleanupCommand() + " && " + anaconda_dir + "/bin/conda list -n "
            + settings.getCurrentCondaEnvironment());
      }
      
      String nextDockerImageName = getNextDockerImageName(project);
      LOG.log(Level.FINEST, "project-nextDockerImageName:" + nextDockerImageName);
  
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand("create")
          .addCommand(dockerFile.getAbsolutePath())
          .addCommand(projectUtils.getRegistryURL() + "/" + nextDockerImageName)
          .redirectErrorStream(true)
          .setCurrentWorkingDirectory(baseDir)
          .setWaitTimeout(1, TimeUnit.HOURS)
          .build();

      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        //Avoid leeking the apitoken in the error logs by replacing it with the name
        if(cc.getInstallType().equals(CondaInstallType.GIT) && !Strings.isNullOrEmpty(apiToken)) {
          String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode();
          String stdout = processResult.getStdout();
          if(stdout != null) {
            errorMsg = errorMsg + " out: " + stdout.replaceAll(apiToken, cc.getGitApiKeyName() + "_token");
          }
          String stderr = processResult.getStderr();
          if(stderr != null) {
            errorMsg = errorMsg + "\n err: " + stderr.replaceAll(apiToken, cc.getGitApiKeyName() + "_token");
          }
          errorMsg = errorMsg + "||\n";
          throw new IOException(errorMsg);
        } else {
          String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
              + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
          throw new IOException(errorMsg);
        }
      } else {
        project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
            RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
        project.setDockerImage(nextDockerImageName);
        project = projectFacade.update(project);
        projectFacade.flushEm();
        setPipConflicts(project);
        environmentController.updateInstalledDependencies(project);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }
  
  private void uninstallLibrary(CondaCommands cc)
      throws IOException, ServiceDiscoveryException, ProjectException, ServiceException, PythonException {

    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    try {
      File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
      File home = new File(System.getProperty("user.home"));
      FileUtils.copyFileToDirectory(new File(home, ".condarc"), baseDir);
      FileUtils.copyDirectoryToDirectory(new File(home, ".pip"), baseDir);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        writer.write("# syntax=docker/dockerfile:experimental");
        writer.newLine();
        writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getProjectId(), false) + "\n");
        writer.newLine();
        writer.write(
            "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
                + " --mount=type=bind,source=.pip,target=/root/.pip ");
        switch (cc.getInstallType()) {
          case CONDA:
            writer.write(anaconda_dir + "/bin/conda remove -y -n " +
                settings.getCurrentCondaEnvironment() + " " + cc.getLib() + " || true\n");
            break;
          case PIP:
            writer.write(anaconda_project_dir + "/bin/pip uninstall -y " + cc.getLib() + " || true\n");
            break;
          case ENVIRONMENT:
          default:
            throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
        }
      }
  
      Project project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
        RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
      String nextDockerImageName = getNextDockerImageName(project);

      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand("create")
          .addCommand(dockerFile.getAbsolutePath())
          .addCommand(projectUtils.getRegistryURL() + "/" + nextDockerImageName)
          .redirectErrorStream(true)
          .setCurrentWorkingDirectory(baseDir)
          .setWaitTimeout(30, TimeUnit.MINUTES)
          .build();

      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new IOException(errorMsg);
      } else {
        project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
          RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
        project.setDockerImage(nextDockerImageName);
        project = projectFacade.update(project);
        projectFacade.flushEm();
        setPipConflicts(project);
        environmentController.updateInstalledDependencies(project);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }

  private void copyCondaArtifactToLocal(String source, String destPath) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      dfso.copyToLocal(source, destPath);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  private void setPipConflicts(Project project) throws IOException, ServiceDiscoveryException, ServiceException,
      PythonException {

    String conflictStr = environmentController.getPipConflicts(project);

    if(Strings.isNullOrEmpty(conflictStr)) {
      project.getPythonEnvironment().setJupyterConflicts(false);
      project.getPythonEnvironment().setConflicts(null);
      return;
    }

    ArrayList<String> conflicts = new ArrayList<>();
    String[] lines = conflictStr.split("\n");
    for(String conflictLine: lines) {
      conflicts.add(conflictLine.split("\\s+")[0].trim());
    }
    List<String> intersect = settings.getJupyterDependencies().stream()
        .filter(conflicts::contains)
        .collect(Collectors.toList());

    if(conflicts.isEmpty()) {
      project.getPythonEnvironment().setJupyterConflicts(false);
      project.getPythonEnvironment().setConflicts(null);
    } else {
      project.getPythonEnvironment().setConflicts(
          conflictStr.substring(0, Math.min(conflictStr.length(), 12000)));
      if(intersect != null && intersect.size() > 0) {
        project.getPythonEnvironment().setJupyterConflicts(true);
      } else {
        project.getPythonEnvironment().setJupyterConflicts(false);
      }
    }
  }

  public void exportLibraries(CondaCommands cc)
    throws IOException, ServiceException, ServiceDiscoveryException, ProjectException {
  
    Project project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
      RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("export")
        .addCommand(projectUtils.getFullDockerImageName(project, false))
        .redirectErrorStream(true)
        .setWaitTimeout(30, TimeUnit.MINUTES)
        .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
      throw new IOException(errorMsg);
    } else {
      environmentController.uploadYmlInProject(project, cc.getUserId(), processResult.getStdout(),
          cc.getArg());
    }
  }

  public void syncBaseLibraries(CondaCommands cc)
    throws ServiceException, ServiceDiscoveryException, ProjectException, IOException {
    Project project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
      RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));

    String condaListOutput = libraryController.condaList(projectUtils.getFullDockerImageName(project, true));
    Collection<PythonDep> projectDeps = libraryController.parseCondaList(condaListOutput);
    projectDeps = libraryController.persistAndMarkImmutable(projectDeps);

    project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
      RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));

    project.setPythonDepCollection(projectDeps);
    projectFacade.update(project);
  }

  private String getNextDockerImageName(Project project) {
    String dockerImage = ProjectUtils.getDockerImageName(project, settings, false);
    int indexOfLastDigit = dockerImage.lastIndexOf(".");
    int currentVersion = Integer.parseInt(dockerImage.substring(indexOfLastDigit + 1));
    int nextVersion = currentVersion + 1;
    return dockerImage.substring(0, indexOfLastDigit) + "." + nextVersion;
  }
  
  private Map<Project, List<CondaCommands>> getCondaCommandsByProject(List<CondaCommands> condaCommands) {
    Map<Project, List<CondaCommands>> condaCommandsByProject = new HashMap<>();
    for (CondaCommands condacommand : condaCommands) {
      // If it is a command of a project that has been deleted, delete the command (do not delete an environment
      //"REMOVE" command as it needs to be processed even after the project has been deleted
      if (condacommand.getOp() != CondaOp.REMOVE &&
        (condacommand.getProjectId() == null || condacommand.getProjectId().getPythonEnvironment() == null)) {
        LOG.log(Level.FINEST, "Removing condacommand: " + condacommand);
        condaCommandFacade.remove(condacommand);
      } else {
        Project project = condacommand.getProjectId();
        if (!condaCommandsByProject.containsKey(project)) {
          condaCommandsByProject.put(project, new ArrayList<>());
        }
        condaCommandsByProject.get(project).add(condacommand);
      }
    }
    return condaCommandsByProject;
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