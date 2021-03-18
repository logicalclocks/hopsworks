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
package io.hops.hopsworks.common.python.environment;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.command.Operation;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public abstract class DockerRegistryMngrImpl implements DockerRegistryMngr {
  
  private static final Logger LOG =
      Logger.getLogger(DockerRegistryMngrImpl.class.getName());
  
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private CommandsController commandsController;
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private SystemCommandFacade systemCommandFacade;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private HttpClient httpClient;
  
  @Override
  public void gc() throws IOException, ServiceException, ProjectException {
    // 1. Get all conda commands of type REMOVE. Should be only 1 REMOVE per project
    final List<CondaCommands> condaCommandsRemove = condaCommandFacade.findByStatusAndCondaOp(CondaStatus.NEW,
      CondaOp.REMOVE);
    LOG.log(Level.FINE, "condaCommandsRemove: " + condaCommandsRemove);
    try {
      for (CondaCommands cc : condaCommandsRemove) {
        // We do not want to remove the base image! Get arguments from command as project may have already been deleted.
        String projectDockerImage = cc.getArg();
        String projectDockerRepoName = projectUtils.getProjectDockerRepoName(projectDockerImage);
        if (!projectUtils.dockerImageIsPreinstalled(projectDockerImage)) {
          try {
            // 1. Get and delete all the tags for each repository(project)
            List<String> projectTags =
                deleteProjectImagesOnRegistry(projectDockerImage);
            for (String tag : projectTags) {
              // Issue system command (kagent) to remove docker image from each host's docker daemon
              dockerImagesGC(projectUtils.getRegistryURL() + "/" +
                  projectDockerRepoName + ":" + tag);
            }
            
          } catch (Exception ex) {
            LOG.log(Level.WARNING,
                "Could not complete docker registry cleanup for: " + cc, ex);
            try {
              commandsController
                  .updateCondaCommandStatus(cc.getId(), CondaStatus.FAILED,
                      cc.getArg(), cc.getOp(),
                      "Could not complete docker registry cleanup: " +
                          ex.getMessage());
            } catch (ServiceException | ProjectException e) {
              LOG.log(Level.WARNING,
                  "Could not change conda command status to NEW.", e);
            }
          }
        }
        commandsController
            .updateCondaCommandStatus(cc.getId(), CondaStatus.SUCCESS,
                cc.getArg(), cc.getOp());
      }
    } finally {
      // Run docker gc in cli
      if (!condaCommandsRemove.isEmpty()) {
        runRegistryGC();
      }
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public final void dockerImagesGC(String image) {
    List<Hosts> allHosts = hostsFacade.findAll();
    for (Hosts host : allHosts) {
      SystemCommand condaGCCommand =
          new SystemCommand(host, Operation.CONDA_GC);
      condaGCCommand.setCommandArgumentsAsString(image);
      systemCommandFacade.persist(condaGCCommand);
    }
  }
  
  public List<String> deleteProjectImagesOnRegistry(String projectDockerImage)
      throws ServiceDiscoveryException, IOException {
    final String projectDockerImageNoTags =
        projectUtils.getProjectNameFromDockerImageName(projectDockerImage);
    
    URI registryURL =
        URI.create("https://" + projectUtils.getRegistryURL() + "/v2/" +
            projectDockerImageNoTags + "/tags" +
            "/list");
    
    HttpGet request = new HttpGet(registryURL);
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    
    HttpHost host = new HttpHost(registryURL.getHost(),
        registryURL.getPort(), registryURL.getScheme());
    String httpResp = httpClient.execute(host, request, httpResponse -> {
      if (httpResponse.getStatusLine().getStatusCode() >= 400) {
        throw new IOException(
            "Could not fetch tags from registry: " +
                httpResponse.getStatusLine().toString());
      }
      return EntityUtils.toString(httpResponse.getEntity());
    });
    
    List<String> projectImageTags = new ArrayList<>();
    JSONObject respJson = new JSONObject(httpResp);
    if (respJson.has("tags") && respJson.get("tags") != "null") {
      JSONArray tagsJSON = new JSONObject(httpResp).getJSONArray("tags");
      for (int i = 0; i < tagsJSON.length(); i++) {
        String tag = tagsJSON.get(i).toString();
        projectImageTags.add(tag);
        
        String prog = settings.getSudoersDir() + "/dockerImage.sh";
        ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
            .addCommand("/usr/bin/sudo")
            .addCommand(prog)
            .addCommand("delete")
            .addCommand(projectDockerImageNoTags)
            .redirectErrorStream(true)
            .setWaitTimeout(1, TimeUnit.MINUTES)
            .build();
  
        ProcessResult processResult =
            osProcessExecutor.execute(processDescriptor);
        if (processResult.getExitCode() != 0) {
          throw new IOException("Could not delete the docker image. Exit code: " +
              processResult.getExitCode() + " out: " + processResult.getStdout());
        }
      }
    }
    return projectImageTags;
  }
  
  public void runRegistryGC() throws IOException {
    String prog = settings.getSudoersDir() + "/dockerImage.sh";
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("gc")
        .redirectErrorStream(true)
        .setWaitTimeout(5, TimeUnit.MINUTES)
        .build();
    
    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      LOG.log(Level.WARNING, "Could not delete the docker image. Exit code: " +
          processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " +
          processResult.getStderr());
    }
  }
}
