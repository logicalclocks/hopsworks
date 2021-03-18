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

package io.hops.hopsworks.api.featurestore.commit;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommitBuilder {

  @EJB
  private FeatureGroupCommitController featureGroupCommitController;

  private URI uri(UriInfo uriInfo, Project project, Featuregroup featuregroup) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.COMMITS.toString().toLowerCase())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.COMMITS);
  }

  public CommitDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                         Featuregroup featuregroup, FeatureGroupCommit featureGroupCommit) {
    CommitDTO commitDTO = new CommitDTO();
    commitDTO.setHref(uri(uriInfo, project, featuregroup));
    commitDTO.setExpand(expand(resourceRequest));

    if (commitDTO.isExpand()) {
      commitDTO.setCommitID(featureGroupCommit.getFeatureGroupCommitPK().getCommitId());
      commitDTO.setCommitTime(featureGroupCommit.getCommittedOn());
      commitDTO.setRowsUpdated(featureGroupCommit.getNumRowsUpdated());
      commitDTO.setRowsInserted(featureGroupCommit.getNumRowsInserted());
      commitDTO.setRowsDeleted(featureGroupCommit.getNumRowsDeleted());
    }
    return commitDTO;
  }

  public CommitDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Featuregroup featuregroup) {
    CommitDTO commitDTO = new CommitDTO();
    commitDTO.setHref(uri(uriInfo, project, featuregroup));
    commitDTO.setExpand(expand(resourceRequest));
    if (commitDTO.isExpand()) {

      AbstractFacade.CollectionInfo featureGroupCommits =
          featureGroupCommitController.getCommitDetails(featuregroup.getId(),
              resourceRequest.getLimit(),
              resourceRequest.getOffset(),
              resourceRequest.getSort(),
              resourceRequest.getFilter());

      commitDTO.setItems((List<CommitDTO>) featureGroupCommits.getItems().stream()
          .map(c -> build(uriInfo, resourceRequest, project, featuregroup, (FeatureGroupCommit) c))
          .collect(Collectors.toList()));
      commitDTO.setCount(featureGroupCommits.getCount());
    }
    return commitDTO;
  }
}