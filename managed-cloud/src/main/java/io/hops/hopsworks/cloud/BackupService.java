/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.BackupCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandType;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CommandStatus;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BackupService {

  private static final Logger LOG
          = Logger.getLogger(BackupService.class.getName());

  @Inject
  private DockerRegistryMngr registry;
  @EJB
  private Settings settings;
  @EJB
  private CondaCommandFacade condaCommandFacade;

  private Map<String, Map<String, Future<ProcessResult>>> ongoingBackup = new HashMap();
  private Map<String, BackupCommand> ongoingBackupCommands = new HashMap();
  protected List<BackupCommand> waitingBackupCommands = new ArrayList();

  public BackupService() {
    
  }
  
  // for testing
  public BackupService(DockerRegistryMngr registry, Settings settings, CondaCommandFacade condaCommandFacade) {
    this.registry = registry;
    this.settings = settings;
    this.condaCommandFacade = condaCommandFacade;
  }

  public void handleBackup(List<BackupCommand> backupCommands, Map<String, CommandStatus> commandsStatus) throws
          InterruptedException, ExecutionException {
    for (BackupCommand command : backupCommands) {
      if (!commandsStatus.containsKey(command.getBackupId())) {
        try {
          Map<String, Future<ProcessResult>> processes = null;
          if (command.getType().equals(CloudCommandType.BACKUP)) {
            settings.setOngoingBackup(true);
            if (noOngoingLibraryInstall()) {
              processes = registry.backupImages(command.getBackupId());
            } else {
              waitingBackupCommands.add(command);
            }
          } else if (command.getType().equals(CloudCommandType.RESTORE)) {
            processes = registry.resotreImages(command.getBackupId());
          } else if (command.getType().equals(CloudCommandType.BACKUP_DONE)) {
            if (ongoingBackupCommands.isEmpty()) {
              //if there are several backups in parallel we should get out of
              //ongoing backup only when the last backup is done
              settings.setOngoingBackup(false);
            }
            commandsStatus.put(command.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.SUCCEED,
                    "Marked backup with id " + command.getBackupId() + " as done"));
          }else if (command.getType().equals(CloudCommandType.DELETE_BACKUP)) {
            registry.deleteBackup(command.getBackupId());
            commandsStatus.put(command.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.SUCCEED,
                    "Delete backup with id " + command.getBackupId()));
          } else {
            commandsStatus.put(command.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
                    "Backup with id " + command.getBackupId() + " has an unknown command type " + command.getType()));
            continue;
          }
          if (processes != null) {
            ongoingBackup.put(command.getId(), processes);
            ongoingBackupCommands.put(command.getId(), command);
          }
        } catch (IOException | ServiceDiscoveryException ex) {
          commandsStatus.put(command.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
                  "Backup with id " + command.getBackupId() + " failed to execute " + ex.getLocalizedMessage()));
        }
      }
    }

    if (!waitingBackupCommands.isEmpty() && noOngoingLibraryInstall()) {
      //some backups are waiting for library installation to be finished and the install are done
      for (BackupCommand command : waitingBackupCommands) {
        try {
          Map<String, Future<ProcessResult>> processes = null;
          processes = registry.backupImages(command.getBackupId());
          ongoingBackup.put(command.getId(), processes);
          ongoingBackupCommands.put(command.getId(), command);
        } catch (IOException | ServiceDiscoveryException ex) {
          commandsStatus.put(command.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
                  "Backup with id " + command.getBackupId() + " failed to execute " + ex.getLocalizedMessage()));
        }
      }
    }

    List<String> backupToRemove = new ArrayList();
    for (Map.Entry<String, Map<String, Future<ProcessResult>>> entry : ongoingBackup.entrySet()) {
      List<String> futureToRemove = new ArrayList();

      for (Map.Entry<String, Future<ProcessResult>> future : entry.getValue().entrySet()) {
        if (future.getValue().isDone()) {
          ProcessResult result = future.getValue().get();
          if (!result.processExited()) {
            commandsStatus.put(entry.getKey(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
                    "Backup with id " + ongoingBackupCommands.get(entry.getKey()).getBackupId()
                    + " took too long to execute "));
            backupToRemove.add(entry.getKey());
          } else if (result.getExitCode() != 0) {
            commandsStatus.put(entry.getKey(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
                    "Backup with id " + ongoingBackupCommands.get(entry.getKey()).getBackupId()
                    + " exited with a non null status " + result.getExitCode() + " stderr: " + result.getStderr()
                    + " stdout: " + result.getStdout()));
            backupToRemove.add(entry.getKey());
          } else {
            futureToRemove.add(future.getKey());
          }
        }
      }
      entry.getValue().keySet().removeAll(futureToRemove);
      if (entry.getValue().isEmpty()) {
        commandsStatus.put(entry.getKey(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.SUCCEED,
                "Backup with id " + ongoingBackupCommands.get(entry.getKey()).getBackupId()
                + " finished with success"));
        backupToRemove.add(entry.getKey());
        if (ongoingBackupCommands.get(entry.getKey()).getType().equals(CloudCommandType.RESTORE)) {
          settings.setOngoingBackup(false);
        }
      }
    }

    ongoingBackup.keySet().removeAll(backupToRemove);
    ongoingBackupCommands.keySet().removeAll(backupToRemove);
  }

  private boolean noOngoingLibraryInstall() {
    final List<CondaCommands> allCondaCommandsOngoing = condaCommandFacade.findByStatus(CondaStatus.ONGOING);
    return allCondaCommandsOngoing.isEmpty();
  }
}
