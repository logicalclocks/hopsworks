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
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.command.Operation;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.host.Hosts;
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
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public abstract class DockerRegistryMngrImpl implements DockerRegistryMngr {
  
  private static final Logger LOG =
      Logger.getLogger(DockerRegistryMngrImpl.class.getName());
  
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private SystemCommandFacade systemCommandFacade;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private HttpClient httpClient;
  @EJB
  private DockerImageController dockerImageController;
  
  @Override
  public void deleteProjectDockerImage(String projectDockerImage)
    throws ServiceException, IOException, ServiceDiscoveryException {
    String projectDockerRepoName = projectUtils.getProjectDockerRepoName(projectDockerImage);
    // 1. Get and delete all the tags for each repository(project)
    List<String> projectTags =
      deleteProjectImagesOnRegistry(projectDockerImage);
    for (String tag : projectTags) {
      // Issue system command (kagent) to remove docker image from each host's docker daemon
      dockerImagesGC(projectUtils.getRegistryURL() + "/" +
        projectDockerRepoName + ":" + tag);
    }
  }
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private final void dockerImagesGC(String image) {
    List<Hosts> allHosts = hostsFacade.findAll();
    for (Hosts host : allHosts) {
      SystemCommand condaGCCommand =
          new SystemCommand(host, Operation.CONDA_GC);
      condaGCCommand.setCommandArgumentsAsString(image);
      systemCommandFacade.persist(condaGCCommand);
    }
  }
  
  protected List<String> deleteProjectImagesOnRegistry(String projectDockerImage)
    throws ServiceDiscoveryException, IOException, ServiceException {
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
        dockerImageController.deleteImage(projectDockerImageNoTags);
      }
    }
    return projectImageTags;
  }
  
  @Override
  public void runRegistryGC() throws ServiceException {
    dockerImageController.gcImages();
  }
  
  @Override
  public Map<String, Future<ProcessResult>> backupImages(String backupId)
    throws IOException, ServiceDiscoveryException, ServiceException {
    throw new UnsupportedOperationException("online backup only supported on cluster with managed repo");
  }

  @Override
  public Map<String, Future<ProcessResult>> restoreImages(String backupId)
    throws IOException, ServiceDiscoveryException, ServiceException {
    throw new UnsupportedOperationException("online backup only supported on cluster with managed repo");
  }
  
  @Override
  public List<String> deleteBackup(String backupId)
          throws ServiceDiscoveryException {
    throw new UnsupportedOperationException("online backup only supported on cluster with managed repo");
  }
}
