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

import io.hops.hopsworks.common.dao.project.ProjectFacade;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.AnacondaRepo;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.restutils.RESTCodes;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Startup;
import org.apache.commons.io.FileUtils;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibraryInstaller {

  private static final Logger LOG = Logger.getLogger(LibraryInstaller.class.getName());

  private static final Comparator ASC_COMPARATOR = new CommandsComparator();

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
  private LibraryFacade libraryFacade;
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private LibraryController libraryController;
      
  @PostConstruct
  public void init() {
    schedule();
  }

  private void schedule() {
    timerService.createSingleActionTimer(1000L, new TimerConfig("python library installer", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void isAlive() {
    try {
      final List<CondaCommands> allCondaCommands = condaCommandFacade.findByStatus(CondaStatus.NEW);
      allCondaCommands.sort(ASC_COMPARATOR);
      for (final CondaCommands cc : allCondaCommands) {
        try {
          try {
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
              case REMOVE:
                deleteImage(cc);
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
          LOG.log(Level.WARNING, "Could not update command with ID: " + cc.getId());
        }
      }
    } finally {
      schedule();
    }
  }

  private void createNewImage(CondaCommands cc) throws IOException, ServiceDiscoveryException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile));
    writer.write("FROM " + projectUtils.getFullBaseImageName());
    writer.close();

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

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

  private void deleteImage(CondaCommands cc) throws IOException, ServiceDiscoveryException {
    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("delete")
        .addCommand(projectUtils.getFullDockerImageName(cc.getProjectId(), false))
        .redirectErrorStream(true)
        .setWaitTimeout(300L, TimeUnit.SECONDS)
        .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String errorMsg = "Could not delete the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
      throw new IOException(errorMsg);
    }
  }

  private void installLibrary(CondaCommands cc) throws IOException, ServiceException, ServiceDiscoveryException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    File home = new File(System.getProperty("user.home"));
    File condarc = new File(home, ".condarc");
    File pip = new File(home, ".pip");
    FileUtils.copyFileToDirectory(condarc, baseDir);
    FileUtils.copyDirectoryToDirectory(pip, baseDir);
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile));
    writer.write("# syntax=docker/dockerfile:experimental");
    writer.newLine();
    writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getProjectId(), false));
    writer.newLine();
    writer.write(
        "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
            + " --mount=type=bind,source=.pip,target=/root/.pip ");
    switch (cc.getInstallType()) {
      case CONDA:
        writer.write(settings.getAnacondaDir() + "/bin/conda install -y -n " +
            settings.getCurrentCondaEnvironment() + " -c " + cc.getChannelUrl() + " " + cc.
            getLib() + "=" + cc.getVersion());
        break;
      case PIP:
        writer.write(settings.getAnacondaProjectDir() + "/bin/pip install --upgrade " +
            cc.getLib() + "==" + cc.getVersion());
        break;
      case ENVIRONMENT:
      default:
        throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
    }

    writer.close();

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

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

    try {
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
    
    Collection<PythonDep> envDeps = listLibraries(projectUtils.getFullDockerImageName(cc.getProjectId(), false));
    libraryController.addPythonDepsForProject(cc.getProjectId(), envDeps);
  }

  private void uninstallLibrary(CondaCommands cc) throws IOException, ServiceDiscoveryException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdirs();
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile));
    writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getProjectId(), false) + "\n");
    switch (cc.getInstallType()) {
      case CONDA:
        writer.write("RUN " + settings.getAnacondaDir() + "/bin/conda remove -y -n " +
            settings.getCurrentCondaEnvironment() + " " + cc.getLib() + "\n");
        break;
      case PIP:
        writer.write("RUN " + settings.getAnacondaProjectDir() + "/bin/pip uninstall -y " +
            cc.getLib() + "\n");
        break;
      case ENVIRONMENT:
      default:
        throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
    }

    writer.close();

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

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

    try {
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

  public Collection<PythonDep> listLibraries(String imageName) throws ServiceException {

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("list")
        .addCommand(imageName)
        .redirectErrorStream(true)
        .setWaitTimeout(300L, TimeUnit.SECONDS)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        LOG.log(Level.SEVERE, errorMsg);
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_IMAGE_CREATION_ERROR, Level.SEVERE);
      } else {
        return depStringToCollec(processResult.getStdout());
      }
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_IMAGE_CREATION_ERROR, Level.SEVERE);
    }
  }

  public void exportLibraries(CondaCommands cc) throws IOException, ServiceException, ServiceDiscoveryException {

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

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
  
  public Collection<PythonDep> depStringToCollec(String condaListStr) throws ServiceException {
    Collection<PythonDep> deps = new ArrayList();

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
      boolean cannotBeRemoved = channel.equals("default") ? true : false;
      PythonDep pyDep = libraryFacade.getOrCreateDep(repo, installType, libraryName, version,
          false, cannotBeRemoved);
      deps.add(pyDep);
    }
    return deps;
  }

  private String getNextDockerImageName(Project project) {
    String dockerImage = project.getDockerImage();
    int indexOfLastDigit = dockerImage.lastIndexOf(".");
    int currentVersion = Integer.parseInt(dockerImage.substring(indexOfLastDigit + 1, dockerImage.length()));
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
      if (t.getId() > t1.getId()) {
        return 1;
      } else if (t.getId() < t1.getId()) {
        return -1;
      } else {
        return 0;
      }
    }

    private int systemCommandCompare(final SystemCommand t, final SystemCommand t1) {
      return t.getId().compareTo(t1.getId());
    }
  }
}
