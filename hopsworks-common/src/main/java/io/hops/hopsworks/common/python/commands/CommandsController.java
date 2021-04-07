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
package io.hops.hopsworks.common.python.commands;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.AnacondaRepo;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommandsController {
  
  private static final Logger LOGGER = Logger.getLogger(CommandsController.class.getName());
  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private LibraryFacade libraryFacade;
  
  public void deleteCommands(Project project, String library) {
    //Failed installation commands should remove
    List<CondaCommands> commands = condaCommandFacade.getFailedCommandsForProjectAndLib(project, library);
    Optional<CondaCommands> command = commands.stream()
        .filter(d -> d.getStatus().equals(CondaStatus.FAILED))
        .findFirst();
    if(command.isPresent()) {
      Collection<PythonDep> projectLibs = project.getPythonDepCollection();
      projectLibs.removeIf(lib -> lib.getDependency().equals(library));
      projectFacade.update(project);
    }
    condaCommandFacade.deleteCommandsForLibrary(project, library);
  }
  
  public void deleteCommands(Project project) {
    condaCommandFacade.deleteCommandsForEnvironment(project);
  }
  
  public void retryFailedCondaEnvOps(Project project) {
    List<CondaCommands> commands = condaCommandFacade.getFailedEnvCommandsForProject(project);
    for (CondaCommands cc : commands) {
      cc.setStatus(CondaStatus.NEW);
      condaCommandFacade.update(cc);
    }
  }
    
  public void retryFailedCondaLibraryOps(Project project, String library) {
    List<CondaCommands> commands = condaCommandFacade.getFailedCommandsForProjectAndLib(project, library);
    for (CondaCommands cc : commands) {
      cc.setStatus(CondaStatus.NEW);
      condaCommandFacade.update(cc);
    }
  }
  
  public void deleteCommandsForProject(Project proj) {
    List<CondaCommands> commands = condaCommandFacade.getCommandsForProject(proj);
    if (commands == null) {
      return;
    }
    // First, remove any old commands for the project in conda_commands. Except for REMOVE commands as they are
    //processed asynchronously
    commands.stream().filter(cc -> cc.getOp() != CondaOp.REMOVE).forEach(condaCommandFacade::remove);
  }

  public PythonDep condaOp(CondaOp op, Users user, CondaInstallType installType, Project proj, String channelUrl,
                           String lib, String version, String arg, GitBackend gitBackend, String apiKeyName)
    throws GenericException, ServiceException {

    if(Strings.isNullOrEmpty(version) && CondaOp.isLibraryOp(op)) {
      version = Settings.UNKNOWN_LIBRARY_VERSION;
    }

    //If there is an already ongoing command to uninstall the same library, allow the queuing of a new install op
    List<CondaStatus> statuses = new ArrayList<>();
    statuses.add(CondaStatus.NEW);
    statuses.add(CondaStatus.ONGOING);
    List<CondaCommands> uninstallCommands =
      condaCommandFacade.findByStatusAndCondaOpAndProject(statuses, CondaOp.UNINSTALL, proj);
    //Get current uninstall operations for this project
    List<CondaCommands> ongoingUninstall = uninstallCommands.stream()
      .filter(c -> c.getLib().equalsIgnoreCase(lib) && c.getInstallType().equals(installType))
      .collect(Collectors.toList());
    //Get currently installed library with the same name and package manager if it exists
    Optional<PythonDep> installedDep = proj.getPythonDepCollection().stream()
      .filter(d -> d.getDependency().equalsIgnoreCase(lib) && d.getInstallType().equals(installType))
      .findFirst();

    if (op == CondaOp.INSTALL && installedDep.isPresent() && ongoingUninstall.isEmpty()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_DEP_INSTALL_FORBIDDEN, Level.FINE,
        "dep: " + lib);
    }
    
    PythonDep dep;
    try {
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = libraryFacade.getRepo(channelUrl, true);

      // 2. Test if pythonDep exists. If not, add it.
      dep = libraryFacade.getOrCreateDep(repo, installType, lib, version, true, false);

      Collection<PythonDep> depsInProj = proj.getPythonDepCollection();
      // 3. Add the python library to the join table for the project
      if (op == CondaOp.INSTALL) {
        depsInProj.remove(dep);// if upgrade
        depsInProj.add(dep);
      }
      proj.setPythonDepCollection(depsInProj);
      projectFacade.update(proj);

      CondaCommands cc = new CondaCommands(settings.getAnacondaUser(), user, op,
          CondaStatus.NEW, installType, proj, lib, version, channelUrl,
          new Date(), arg, null, false, gitBackend, apiKeyName);
      condaCommandFacade.save(cc);
    } catch (Exception ex) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, "condaOp failed",
          ex.getMessage(), ex);
    }
    return dep;
  }
  
  public void updateCondaCommandStatus(int commandId, CondaStatus condaStatus, String arg,
                                       CondaOp opType) throws ServiceException, ProjectException {
    updateCondaCommandStatus(commandId, condaStatus, arg, opType, null);
  }
  
  public void updateCondaCommandStatus(int commandId, CondaStatus condaStatus, String arg, CondaOp opType,
                                       String errorMessage) throws ServiceException, ProjectException {
    CondaCommands cc = condaCommandFacade.findCondaCommand(commandId);
    if (cc != null) {
      if (condaStatus == CondaStatus.SUCCESS) {
        // remove completed commands
        condaCommandFacade.remove(cc);
        // Check if this is the last operation for this project. If yes, set
        // the PythonDep to be installed or
        // the CondaEnv operation is finished implicitly (no condaOperations are
        // returned => CondaEnv operation is finished).
        if (CondaOp.isLibraryOp(opType)) {
          Project project = projectFacade.findById(cc.getProjectId().getId()).orElseThrow(() -> new ProjectException(
            RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + cc.getProjectId().getId()));
          PythonDep dep = libraryFacade.getOrCreateDep(libraryFacade.getRepo(cc.getChannelUrl(), false),
              cc.getInstallType(), cc.getLib(), cc.getVersion(), true, false);
          Collection<PythonDep> deps = project.getPythonDepCollection();

          if(isPlaceholderDep(cc, opType) || opType.equals(CondaOp.UNINSTALL)) {
            deps.remove(dep);
          }
          
          project.setPythonDepCollection(deps);
          projectFacade.update(project);
          projectFacade.flushEm();
        }
      } else if(condaStatus == CondaStatus.FAILED) {
        cc.setStatus(condaStatus);
        cc.setArg(arg);
        cc.setErrorMsg(errorMessage);
        condaCommandFacade.update(cc);
      } else if (condaStatus == CondaStatus.ONGOING) {
        cc.setStatus(condaStatus);
        condaCommandFacade.update(cc);
      }
    } else {
      LOGGER.log(Level.FINE, "Could not remove CondaCommand with id: {0}", commandId);
    }
  }

  /**
   * A placeholder is a library which represents a dependency in the project which is not known before installation
   * For example installing from a git url, we do not know the name of the library, and when installing a library
   * but not specifying a version it's not known which PythonDep it will resolve to in the end.
   * We need placeholders to keep track of what libraries are being installed in the environment and to monitor the
   * installation progress.
   *
   * @param command
   * @param condaOp
   * @return
   */
  private boolean isPlaceholderDep(CondaCommands command, CondaOp condaOp) {
    if (command.getInstallType().equals(CondaInstallType.GIT) ||
      command.getInstallType().equals(CondaInstallType.EGG) ||
      command.getInstallType().equals(CondaInstallType.WHEEL) ||
      command.getInstallType().equals(CondaInstallType.REQUIREMENTS_TXT) ||
      command.getInstallType().equals(CondaInstallType.ENVIRONMENT_YAML)) {
      return true;
    }
    if(condaOp.equals(CondaOp.INSTALL) &&
      (command.getInstallType().equals(CondaInstallType.PIP)
        || command.getInstallType().equals(CondaInstallType.CONDA))
      && command.getVersion().equals(Settings.UNKNOWN_LIBRARY_VERSION)) {
      return true;
    }
    return false;
  }
}

