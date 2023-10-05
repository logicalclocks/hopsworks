/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.python.commands.custom;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.python.commands.CommandsController;
import org.apache.commons.io.FilenameUtils;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.dataset.FilePreviewMode;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CustomCommandsController {
  private static final Logger LOGGER = Logger.getLogger(CustomCommandsController.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private DatasetController datasetController;
  @EJB
  private CommandsController commandsController;

  public CondaCommands buildEnvWithCustomCommands(Project project, Users users, CustomCommandsSettings cmdSettings,
                                                  String version)
      throws IOException, DatasetException, ServiceException {
    validateCommandsFile(project, users, cmdSettings.getCommandsFile());
    if (!Strings.isNullOrEmpty(cmdSettings.getArtifacts())) {
      validateArtifacts(cmdSettings);
    }
    CondaCommands cc = new CondaCommands(users, CondaOp.CUSTOM_COMMANDS, CondaStatus.NEW,
      CondaInstallType.CUSTOM_COMMANDS, project, null, version, null, cmdSettings.getArtifacts(), null, false, null,
      null);
    cc.setCustomCommandsFile(cmdSettings.getCommandsFile());
    commandsController.create(cc);
    return cc;
  }

  private void validateArtifacts(CustomCommandsSettings cmdSettings) throws IOException, ServiceException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String[] artifacts = cmdSettings.getArtifacts().split(",");
      for (String artifact: artifacts) {
        if (!dfso.exists(artifact)) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_DOCKER_COMMAND_FILE, Level.INFO,
              "Artifact path: " + artifact + " does not exist");
        } else if (dfso.isDir(artifact)) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_DOCKER_COMMAND_FILE, Level.INFO,
              "Artifact, " + artifact + ", is a directory");
        }
      }
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }

  private void validateCommandsFile(Project project, Users user, String commandsFile)
      throws DatasetException, ServiceException {
    if (Strings.isNullOrEmpty(commandsFile)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_DOCKER_COMMAND_FILE, Level.INFO,
          "Please provide the file path that contains the commands you would like to execute");
    }
    if (!FilenameUtils.getExtension(commandsFile).equals("sh")) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_DOCKER_COMMAND_FILE, Level.INFO,
          "The commands file should have .sh extension");
    }
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      DatasetPath datasetPath = datasetHelper.getDatasetPathIfFileExist(project, commandsFile, DatasetType.DATASET);
      FilePreviewDTO previewDTO = datasetController.filePreview(datasetPath.getAccessProject(), user,
          datasetPath.getFullPath(), FilePreviewMode.TAIL, new ArrayList<String>());
      String commands = previewDTO.getContent();
      if (Strings.isNullOrEmpty(commands)) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_DOCKER_COMMAND_FILE, Level.INFO,
            "The commands file provided is empty");
      } else if (!commands.startsWith("#!/bin/bash")) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.INVALID_DOCKER_COMMAND_FILE, Level.INFO,
            "The commands file provided is not a valid bash script: " +
                "the first line in the script should be #!/bin/bash");
      }
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }
}
