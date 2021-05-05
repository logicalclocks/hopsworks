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
package io.hops.hopsworks.common.provenance.core;

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.elastic.ElasticClientController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.rest.RestStatus;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvenanceCleanerController {
  private final static Logger LOGGER = Logger.getLogger(ProvenanceCleanerController.class.getName());
  
  @EJB
  private ElasticClientController client;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeFacade inodeFacade;
  
  public Pair<Integer, String> indexCleanupRound(String nextToCheck, Integer limit)
    throws ProvenanceException, ElasticException {
    String indexRegex = "*" + Settings.PROV_FILE_INDEX_SUFFIX;
    String[] indices = client.mngIndicesGetBySimplifiedRegex(indexRegex);
    
    int cleaned = 0;
    String nextToCheckAux = "";
    for(String indexName : indices) {
      if(cleaned > limit) {
        nextToCheckAux = indexName;
        break;
      }
      if(indexName.compareTo(nextToCheck) < 0) {
        continue;
      }
      Project project = getProject(indexName);
      if(project == null) {
        LOGGER.log(Level.FINE, "deleting prov index:{0} with no corresponding project", indexName);
        deleteProvIndex(indexName);
        cleaned++;
        continue;
      }
    }
    return Pair.with(cleaned, nextToCheckAux);
  }
  
  private Project getProject(String indexName) throws ProvenanceException {
    int endIndex = indexName.indexOf(Settings.PROV_FILE_INDEX_SUFFIX);
    String sInodeId = indexName.substring(0, endIndex);
    long inodeId;
    try {
      inodeId = Long.parseLong(sInodeId);
    } catch(NumberFormatException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
        "error extracting project from prov index name - format error", e.getMessage(), e);
    }
    Inode inode = inodeFacade.findById(inodeId);
    if(inode == null) {
      return null;
    }
    return projectFacade.findByInodeId(inode.getInodePK().getParentId(), inode.getInodePK().getName());
  }
  
  private void deleteProvIndex(String indexName) {
    DeleteIndexRequest request = new DeleteIndexRequest(indexName);
    try {
      client.mngIndexDelete(request);
    } catch (ElasticException e) {
      if(e.getCause() instanceof ElasticsearchException) {
        ElasticsearchException ex = (ElasticsearchException)e.getCause();
        if(ex.status() == RestStatus.NOT_FOUND) {
          LOGGER.log(Level.INFO, "trying to delete index:{0} - does not exist", indexName);
          return;
        }
      }
      LOGGER.log(Level.WARNING, "trying to delete index:{0}", e.getStackTrace());
    }
  }
}
