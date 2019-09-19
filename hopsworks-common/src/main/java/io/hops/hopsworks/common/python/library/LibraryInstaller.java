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
import io.hops.hopsworks.persistence.entity.python.AnacondaRepo;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.MachineType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
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
                cc.getId(), CondaStatus.FAILED, cc.getInstallType(), cc.getMachineType(),
                cc.getArg(), cc.getProjectId(), cc.getUserId(), cc.getOp(), cc.getLib(), cc.getChannelUrl(),
                cc.getArg());
            continue;
          }
          commandsController.updateCondaCommandStatus(
              cc.getId(), CondaStatus.SUCCESS, cc.getInstallType(), cc.getMachineType(),
              cc.getArg(), cc.getProjectId(), cc.getUserId(), cc.getOp(), cc.getLib(), cc.getChannelUrl(),
              cc.getArg());
        } catch (ServiceException ex) {
          LOG.log(Level.WARNING, "Could not update command with ID: " + cc.getId());
        }
      }
    } finally {
      schedule();
    }
  }

  private void createNewImage(CondaCommands cc) throws IOException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdir();
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile));
    writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getDockerImage()));
    writer.close();

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("create")
        .addCommand(dockerFile.getAbsolutePath())
        .addCommand(settings.getRegistry() + "/" + cc.getProjectId().getName())
        .redirectErrorStream(true)
        .setCurrentWorkingDirectory(baseDir)
        .setWaitTimeout(60L, TimeUnit.SECONDS)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " Error: " + processResult.getStdout();
        LOG.log(Level.SEVERE, errorMsg);
        throw new IOException(errorMsg);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }

  private void deleteImage(CondaCommands cc) throws IOException {
    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("delete")
        .addCommand(settings.getRegistry() + "/" + cc.getProjectId().getName())
        .redirectErrorStream(true)
        .setWaitTimeout(60L, TimeUnit.SECONDS)
        .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String errorMsg = "Could not delete the docker image. Exit code: " + processResult.getExitCode()
          + " Error: " + processResult.getStdout();
      LOG.log(Level.SEVERE, errorMsg);
      throw new IOException(errorMsg);
    }
  }

  private void installLibrary(CondaCommands cc) throws IOException, ServiceException {
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
    writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getDockerImage()));
    writer.newLine();
    writer.write(
        "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
            + " --mount=type=bind,source=.pip,target=/root/.pip ");
    switch (cc.getInstallType()) {
      case CONDA:
        writer.write("/srv/hops/anaconda/bin/conda install -y -n theenv -c " + cc.getChannelUrl() + " " + cc.
            getLib() + "=" + cc.getVersion());
        break;
      case PIP:
        writer.write("/srv/hops/anaconda/envs/theenv/bin/pip install --upgrade " + cc.getLib() + "==" + cc.
            getVersion());
        break;
      case ENVIRONMENT:
      default:
        throw new UnsupportedOperationException("instal type unknown: " + cc.getInstallType());
    }

    writer.close();

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("create")
        .addCommand(dockerFile.getAbsolutePath())
        .addCommand(settings.getRegistry() + "/" + cc.getProjectId().getName())
        .redirectErrorStream(true)
        .setCurrentWorkingDirectory(baseDir)
        .setWaitTimeout(60L, TimeUnit.SECONDS)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " Error: " + processResult.getStdout();
        LOG.log(Level.SEVERE, errorMsg);
        throw new IOException(errorMsg);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
    
    Collection<PythonDep> envDeps = listLibraries(projectUtils.getFullDockerImageName(cc.getProjectId()));
    libraryController.addPythonDepsForProject(cc.getProjectId(), envDeps);
  }

  private void uninstallLibrary(CondaCommands cc) throws IOException {
    File baseDir = new File("/tmp/docker/" + cc.getProjectId().getName());
    baseDir.mkdir();
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile));
    writer.write("FROM " + projectUtils.getFullDockerImageName(cc.getDockerImage()) + "\n");
    switch (cc.getInstallType()) {
      case CONDA:
        writer.write("RUN /srv/hops/anaconda/bin/conda remove -y -n theenv " + cc.getLib() + "\n");
        break;
      case PIP:
        writer.write("RUN /srv/hops/anaconda/envs/theenv/bin/pip uninstall -y " + cc.getLib() + "\n");
        break;
      case ENVIRONMENT:
      default:
        throw new UnsupportedOperationException("instal type unknown: " + cc.getInstallType());
    }

    writer.close();

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("create")
        .addCommand(dockerFile.getAbsolutePath())
        .addCommand(settings.getRegistry() + "/" + cc.getProjectId().getName())
        .redirectErrorStream(true)
        .setCurrentWorkingDirectory(baseDir)
        .setWaitTimeout(60L, TimeUnit.SECONDS)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " Error: " + processResult.getStdout();
        LOG.log(Level.SEVERE, errorMsg);
        throw new IOException(errorMsg);
      }
    } finally {
      FileUtils.deleteDirectory(baseDir);
    }
  }

  public Collection<PythonDep> listLibraries(String imageName) throws IOException, ServiceException {

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("list")
        .addCommand(imageName)
        .redirectErrorStream(true)
        .setWaitTimeout(60L, TimeUnit.SECONDS)
        .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " Error: " + processResult.getStdout();
      LOG.log(Level.SEVERE, errorMsg);
      throw new IOException(errorMsg);
    } else {
      return depStringToCollec(processResult.getStdout());
    }
  }

  public void exportLibraries(CondaCommands cc) throws IOException, ServiceException {

    String prog = settings.getSudoersDir() + "/dockerImage.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("export")
        .addCommand(projectUtils.getFullDockerImageName(cc.getProjectId()))
        .redirectErrorStream(true)
        .setWaitTimeout(60L, TimeUnit.SECONDS)
        .build();

    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " Error: " + processResult.getStdout();
      LOG.log(Level.SEVERE, errorMsg);
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
      String channel = "defaults";
      if (split.length > 3) {
        channel = split[3].trim().isEmpty() ? channel : split[3];
      }

      CondaInstallType instalType = CondaInstallType.PIP;
      if (!(channel.equals("pypi") || channel.equals("defaults"))) {
        instalType = CondaInstallType.CONDA;
      }
      AnacondaRepo repo = libraryFacade.getRepo(channel, true);
      boolean cannotBeRemoved = channel.equals("default") ? true : false;
      PythonDep pyDep = libraryFacade.getOrCreateDep(repo, MachineType.ALL,
          instalType, libraryName, version, false, cannotBeRemoved);
      deps.add(pyDep);
    }
    return deps;
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
      if (t.getId() > t1.getId()) {
        return 1;
      } else if (t.getId() < t1.getId()) {
        return -1;
      } else {
        return 0;
      }
    }
  }
}
