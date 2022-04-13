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

package io.hops.hopsworks.api.jupyter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.opensearch.OpenSearchController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.jupyter.NotebookDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.opensearch.search.SearchHit;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NotebookBuilder {

  private static final Logger LOGGER = Logger.getLogger(NotebookBuilder.class.getName());

  @EJB
  private OpenSearchController elasticController;
  @EJB
  private InodeController inodeController;
  @EJB
  private InodeFacade inodeFacade;

  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JaxbAnnotationModule());
  }

  private UriBuilder uri(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.JUPYTER.toString().toLowerCase());
  }

  private URI uri(UriInfo uriInfo, Project project, int count) {
    return uri(uriInfo, project)
        .path("recent")
        .queryParam("count", count)
        .build();
  }

  public NotebookDTO build(UriInfo uriInfo, Project project, int count) throws OpenSearchException {
    NotebookDTO dto = new NotebookDTO();
    dto.setHref(uri(uriInfo, project, count));
    SearchHit[] elasticHits = elasticController.recentJupyterNotebookSearch(count, project.getId());
    for (SearchHit hit : elasticHits) {
      buildNotebook(hit, dto);
    }
    return dto;
  }

  private void buildNotebook(SearchHit hit, NotebookDTO notebooks) {
    NotebookDTO item = new NotebookDTO();
    Map xattrMap = (Map)hit.getSourceAsMap().get("xattr");

    Long inodeId = Long.parseLong(hit.getId());
    Inode inode = inodeFacade.findById(inodeId);
    if (inode != null) {
      item.setPath(inodeController.getPath(inode));
    }

    Map jupyterConfigMap = (Map)xattrMap.get(Settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME);
    if (jupyterConfigMap != null && !jupyterConfigMap.isEmpty()) {
      try {
        String jupyterSettingsStr = jupyterConfigMap.get(Settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME).toString();
        item.setJupyterSettings(objectMapper.readValue(jupyterSettingsStr, JupyterSettings.class));
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Could not parse JupyterSettings for notebook at path: " + item.getPath(), e);
      }
      item.setDate(new Date(Long.parseLong(jupyterConfigMap.get(Settings.META_USAGE_TIME).toString())));
    }


    notebooks.addItem(item);
  }
}
