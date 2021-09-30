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

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.jupyter.NotebookDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.elasticsearch.search.SearchHit;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NotebookBuilder {

  @EJB
  private JupyterController jupyterController;
  @EJB
  private ElasticController elasticController;
  @EJB
  private InodeController inodeController;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private Settings settings;

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

  public NotebookDTO build(UriInfo uriInfo, Project project, int count) throws ElasticException {
    NotebookDTO dto = new NotebookDTO();
    dto.setHref(uri(uriInfo, project, count));
    SearchHit[] elasticHits = elasticController.recentJupyterNotebookSearch(count, project.getId());
    for (SearchHit hit : elasticHits) {
      buildNotebook(hit, dto);
    }
    return dto;
  }

  public void buildNotebook(SearchHit hit, NotebookDTO notebooks) {
    NotebookDTO item = new NotebookDTO();

    Map xattrMap = (Map)hit.getSourceAsMap().get("xattr");
    Map jupyterConfigMap = (Map)xattrMap.get(settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME);
    item.setJupyterConfiguration(
            jupyterConfigMap.get(settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME).toString());

    Long inodeId = Long.parseLong(hit.getId());
    if (inodeId != null) {
      Inode inode = inodeFacade.findById(inodeId);
      if (inode != null) {
        item.setPath(inodeController.getPath(inode));
      }
    }
    notebooks.addItem(item);
  }
}