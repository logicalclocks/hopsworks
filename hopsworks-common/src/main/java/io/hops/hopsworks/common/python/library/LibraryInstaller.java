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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import org.apache.commons.io.FileUtils;

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
import java.util.Collection;
import java.util.Comparator;
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

  @PostConstruct
  public void init() {
    prog =  settings.getSudoersDir() + "/dockerImage.sh";
    anaconda_dir = settings.getAnacondaDir();
    anaconda_project_dir = anaconda_dir + "/envs/theenv";
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
        // Group new commands by project and run it parallel
        final List<CondaCommands> allCondaCommandsNew = condaCommandFacade.findByStatus(CondaStatus.NEW);
        final Map<Project, List<CondaCommands>> allCondaCommandsNewByProject = allCondaCommandsNew
          .stream()
          .collect(Collectors.groupingBy(CondaCommands::getProjectId));
        final Map<Project, List<CondaCommands>> allCondaCommandsOngoingByProject = allCondaCommandsOngoing
          .stream()
          .collect(Collectors.groupingBy(CondaCommands::getProjectId));
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
  
  private void condaCommandHandler(List<CondaCommands> projectCondaCommands){
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
        } catch (ServiceException ex) {
          LOG.log(Level.WARNING, "Could not update command with ID: " + cc.getId(), ex);
        }
      }
    }
  }
  private void createNewImage(CondaCommands cc) throws IOException, ServiceDiscoveryException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    try(BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
      writer.write("FROM " + projectUtils.getFullBaseImageName());
    }
    
    Project project = cc.getProjectId();
    String initialDockerImage = projectUtils.getInitialDockerImageName(project);

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("create")
        .addCommand(dockerFile.getAbsolutePath())
        .addCommand(projectUtils.getRegistryURL() + "/" + initialDockerImage)
        .redirectErrorStream(true)
        .setCurrentWorkingDirectory(baseDir)
        .setWaitTimeout(300L, TimeUnit.SECONDS)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new IOException(errorMsg);
      } else {
        project.setDockerImage(initialDockerImage);
        projectFacade.update(project);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }

  private void installLibrary(CondaCommands cc) throws IOException, ServiceException, ServiceDiscoveryException {
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
        writer.write("# syntax=docker/dockerfile:experimental");
        writer.newLine();
        writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getProjectId(), false));
        writer.newLine();
        writer.write(
          "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
            + " --mount=type=bind,source=.pip,target=/root/.pip ");
        switch (cc.getInstallType()) {
          case CONDA:
            writer.write(anaconda_dir + "/bin/conda install -y -n " +
              "theenv" + " -c " + cc.getChannelUrl() + " " + cc.
              getLib() + "=" + cc.getVersion());
            break;
          case PIP:
            writer.write(anaconda_project_dir + "/bin/pip install --upgrade " +
              cc.getLib() + "==" + cc.getVersion());
            break;
          case ENVIRONMENT:
          default:
            throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
        }
      }
      
      String nextDockerImageName = getNextDockerImageName(cc.getProjectId());
      
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("create")
        .addCommand(dockerFile.getAbsolutePath())
        .addCommand(projectUtils.getRegistryURL() + "/" + nextDockerImageName)
        .redirectErrorStream(true)
        .setCurrentWorkingDirectory(baseDir)
        .setWaitTimeout(300L, TimeUnit.SECONDS)
        .build();
      
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new IOException(errorMsg);
      } else {
        Project project = cc.getProjectId();
        project.setDockerImage(nextDockerImageName);
        projectFacade.update(project);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
    
    Collection<PythonDep> envDeps =
      libraryController.listLibraries(projectUtils.getFullDockerImageName(cc.getProjectId(), false));
    libraryController.addPythonDepsForProject(cc.getProjectId(), envDeps);
  }

  private void uninstallLibrary(CondaCommands cc) throws IOException, ServiceDiscoveryException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    try {
      File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getProjectId(), false) + "\n");
        switch (cc.getInstallType()) {
          case CONDA:
            writer.write("RUN " + anaconda_dir + "/bin/conda remove -y -n " +
              "theenv" + " " + cc.getLib() + "\n");
            break;
          case PIP:
            writer.write("RUN " + anaconda_project_dir + "/bin/pip uninstall -y " +
              cc.getLib() + "\n");
            break;
          case ENVIRONMENT:
          default:
            throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
        }
      }
      
      String nextDockerImageName = getNextDockerImageName(cc.getProjectId());
      
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("create")
        .addCommand(dockerFile.getAbsolutePath())
        .addCommand(projectUtils.getRegistryURL() + "/" + nextDockerImageName)
        .redirectErrorStream(true)
        .setCurrentWorkingDirectory(baseDir)
        .setWaitTimeout(300L, TimeUnit.SECONDS)
        .build();
      
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new IOException(errorMsg);
      } else {
        Project project = cc.getProjectId();
        project.setDockerImage(nextDockerImageName);
        projectFacade.update(project);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }
  
  

  public void exportLibraries(CondaCommands cc) throws IOException, ServiceException, ServiceDiscoveryException {
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(prog)
      .addCommand("export")
      .addCommand(projectUtils.getFullDockerImageName(cc.getProjectId(), false))
      .redirectErrorStream(true)
      .setWaitTimeout(300L, TimeUnit.SECONDS)
      .build();
    
    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
        + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
      throw new IOException(errorMsg);
    } else {
      environmentController.uploadYmlInProject(cc.getProjectId(), cc.getUserId(), processResult.getStdout(),
        cc.getArg());
    }
  }
  
  private String getNextDockerImageName(Project project) {
    String dockerImage = project.getDockerImage();
    int indexOfLastDigit = dockerImage.lastIndexOf(".");
    int currentVersion = Integer.parseInt(dockerImage.substring(indexOfLastDigit + 1));
    int nextVersion = currentVersion + 1;
    return dockerImage.substring(0, indexOfLastDigit) + "." + nextVersion;
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