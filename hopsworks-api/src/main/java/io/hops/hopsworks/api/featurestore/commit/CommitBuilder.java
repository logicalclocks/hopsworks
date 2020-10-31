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
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommitBuilder {

  @EJB
  FeatureGroupCommitController featureGroupCommitController;

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.COMMITS);
  }

  public CommitDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                         FeatureGroupCommit featureGroupCommit) {
    URI href = uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.COMMITS.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .build();

    CommitDTO dto = new CommitDTO();
    dto.setHref(href);
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto = convertFeatureGroupCommitToDTO(featureGroupCommit,dto);
    }

    return dto;
  }

  public List<CommitDTO> build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                               List<FeatureGroupCommit> featureGroupCommits) {
    URI href = uriInfo.getBaseUriBuilder()
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.COMMITS.toString().toLowerCase())
        .build();

    List<CommitDTO> dtos = new ArrayList<>();
    for (FeatureGroupCommit featureGroupCommit : featureGroupCommits){
      CommitDTO dto = new CommitDTO();
      dto.setHref(href);
      dto.setExpand(expand(resourceRequest));
      if (dto.isExpand()) {
        dtos.add(convertFeatureGroupCommitToDTO(featureGroupCommit,dto));
      }
    }

    return dtos;
  }

  private CommitDTO convertFeatureGroupCommitToDTO(FeatureGroupCommit featureGroupCommit, CommitDTO commitDTO) {
    commitDTO.setCommitID(featureGroupCommit.getFeatureGroupCommitPK().getCommitId());
    commitDTO.setCommitDateString(featureGroupCommit.getCommittedOn().toString());
    commitDTO.setCommittime(featureGroupCommit.getCommittedOn());
    commitDTO.setRowsUpdated(featureGroupCommit.getNumRowsUpdated());
    commitDTO.setRowsInserted(featureGroupCommit.getNumRowsInserted());
    commitDTO.setRowsDeleted(featureGroupCommit.getNumRowsDeleted());
    return commitDTO;
  }

}
