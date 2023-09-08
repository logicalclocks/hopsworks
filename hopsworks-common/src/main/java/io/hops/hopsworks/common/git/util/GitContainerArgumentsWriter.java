/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.git.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import freemarker.template.TemplateException;
import io.hops.hopsworks.common.dao.git.GitPaths;
import io.hops.hopsworks.common.git.BasicAuthSecrets;
import io.hops.hopsworks.common.git.GitJWTManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.TemplateEngine;
import io.hops.hopsworks.common.util.templates.git.GitContainerLaunchScriptArgumentTemplateBuilder;
import io.hops.hopsworks.common.util.templates.git.GitContainerLaunchScriptArgumentsTemplate;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GitContainerArgumentsWriter {
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private GitCommandOperationUtil gitCommandOperationUtil;
  @EJB
  private GitJWTManager gitJWTManager;
  @EJB
  private Settings settings;
  @EJB
  private TemplateEngine templateEngine;

  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JaxbAnnotationModule());
  }

  public void createArgumentFile(GitOpExecution gitOpExecution, GitPaths gitPaths, BasicAuthSecrets authSecrets)
      throws ServiceDiscoveryException, IOException {
    File argumentsFile = new File(gitPaths.getConfDirPath(), GitContainerLaunchScriptArgumentsTemplate.FILE_NAME);
    if (!argumentsFile.exists()) {
      try (Writer out = new FileWriter(argumentsFile, false)) {
        writeTemplateToFile(out, setUpTemplate(gitOpExecution, gitPaths, authSecrets));
      }
    }
  }

  private void writeTemplateToFile(Writer out, GitContainerLaunchScriptArgumentsTemplate template) throws  IOException {
    Map<String, Object> dataModel = new HashMap<>(1);
    dataModel.put("args", template);
    try {
      templateEngine.template(GitContainerLaunchScriptArgumentsTemplate.TEMPLATE_NAME, dataModel, out);
    } catch (TemplateException ex) {
      throw new IOException(ex);
    }
  }

  private GitContainerLaunchScriptArgumentsTemplate setUpTemplate(GitOpExecution gitOpExecution, GitPaths gitPaths,
                                                                  BasicAuthSecrets authSecrets)
      throws ServiceDiscoveryException, JsonProcessingException {
    String hdfsUser = hdfsUsersController.getHdfsUserName(gitOpExecution.getRepository().getProject(),
        gitOpExecution.getUser());
    //Separate log file names for git logs
    String fullCommandLogFilePath = gitCommandOperationUtil.getLogFileFullPath(gitOpExecution, gitPaths.getLogDirPath(),
        hdfsUser, GitCommandOperationUtil.COMMAND_LOG_FILE_NAME);
    String fullHopsfsMountLogFilePath = gitCommandOperationUtil.getLogFileFullPath(gitOpExecution,
        gitPaths.getLogDirPath(), hdfsUser, GitCommandOperationUtil.HOPSFS_MOUNT_LOG_FILE_NAME);

    String gitCommand = gitOpExecution.getGitCommandConfiguration().getCommandType().getGitCommand();
    return GitContainerLaunchScriptArgumentTemplateBuilder.newBuilder()
            .setGitHome(gitPaths.getGitPath())
            .setHdfsUser(hdfsUser)
            .setCertificatesDir(gitPaths.getCertificatesDirPath())
            .setImageName(projectUtils.getFullDockerImageName(settings.getGitImageName()))
            .setCommandLogFile(fullCommandLogFilePath)
            .setHopsfsMountLogFile(fullHopsfsMountLogFilePath)
            .setProjectName(gitOpExecution.getRepository().getProject().getName())
            .setGitCommand(gitCommand)
            .setCommandConfiguration(objectMapper.writeValueAsString(gitOpExecution.getGitCommandConfiguration()))
            .setTokenPath(gitJWTManager.getTokenFullPath(gitPaths.getTokenPath()).toString())
            .setExecutionId(String.valueOf(gitOpExecution.getId()))
            .setProjectId(String.valueOf(gitOpExecution.getRepository().getProject().getId()))
            .setGitUsername(authSecrets.getUsername())
            .setGitToken(authSecrets.getPassword())
            .setRepositoryId(String.valueOf(gitOpExecution.getRepository().getId()))
            .build();
  }
}
