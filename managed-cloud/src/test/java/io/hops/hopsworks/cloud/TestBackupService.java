/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.BackupCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CommandStatus;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author gautier
 */
public class TestBackupService {

  @Test
  public void testBackupError()
    throws InterruptedException, ExecutionException, IOException, ServiceDiscoveryException, ServiceException {
    Settings mockSettings = Mockito.mock(Settings.class);

    DockerRegistryMngr mockDockerRegistryMngr = Mockito.mock(DockerRegistryMngr.class);
    Mockito.when(mockDockerRegistryMngr.backupImages(Mockito.any())).thenThrow(new IOException("test"));

    CondaCommandFacade mockCondaCommandFacade = Mockito.mock(CondaCommandFacade.class);
    Mockito.when(mockCondaCommandFacade.findByStatus(Mockito.any())).thenReturn(new ArrayList<>());

    BackupService backupService = new BackupService(mockDockerRegistryMngr, mockSettings, mockCondaCommandFacade);

    List<BackupCommand> backupCommands = new ArrayList();
    Map<String, CommandStatus> commandsStatus = new HashMap();

    BackupCommand command = new BackupCommand("commandId", "backupId");
    backupCommands.add(command);

    backupService.handleBackup(backupCommands, commandsStatus);

    Assert.assertEquals("command should have been failed", CommandStatus.CLOUD_COMMAND_STATUS.FAILED, commandsStatus.get("commandId").getStatus());
  }

  public void testOngoingLibInstall() throws InterruptedException, ExecutionException {
    Settings mockSettings = Mockito.mock(Settings.class);

    DockerRegistryMngr mockDockerRegistryMngr = Mockito.mock(DockerRegistryMngr.class);

    CondaCommandFacade mockCondaCommandFacade = Mockito.mock(CondaCommandFacade.class);
    CondaCommands cc = new CondaCommands();
    List<CondaCommands> ccs = new ArrayList<>();
    ccs.add(cc);
    Mockito.when(mockCondaCommandFacade.findByStatus(Mockito.any())).thenReturn(ccs);

    BackupService backupService = new BackupService(mockDockerRegistryMngr, mockSettings, mockCondaCommandFacade);

    List<BackupCommand> backupCommands = new ArrayList();
    Map<String, CommandStatus> commandsStatus = new HashMap();

    BackupCommand command = new BackupCommand("commandId", "backupId");
    backupCommands.add(command);

    backupService.handleBackup(backupCommands, commandsStatus);

    Assert.assertEquals("should be waiting for a backup command", 1, backupService.waitingBackupCommands.size());
  }

  
}
