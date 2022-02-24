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
package io.hops.hopsworks.api.git.branch;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.git.GitCommitsFacade;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
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
public class BranchBuilder {
  private static final int DEFAULT_BRANCH_LIMIT = 20;
  @EJB
  private GitCommitsFacade gitCommitsFacade;

  public URI uri(UriInfo uriInfo, Project project, Integer repositoryId) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repositoryId))
        .path(ResourceRequest.Name.BRANCH.toString())
        .build();
  }

  public boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.BRANCH);
  }

  public BranchDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, GitRepository repository)
      throws GitOpException {
    BranchDTO branchDTO = new BranchDTO();
    branchDTO.setHref(uri(uriInfo, project, repository.getId()));
    branchDTO.setExpand(expand(resourceRequest));
    if (branchDTO.isExpand()) {
      Integer limit = resourceRequest.getLimit() == null ? DEFAULT_BRANCH_LIMIT : resourceRequest.getLimit();
      Integer offset = resourceRequest.getOffset() == null ? 0 : resourceRequest.getOffset();
      AbstractFacade.CollectionInfo<String> branches = gitCommitsFacade.getRepositoryBranches(repository, limit,
          offset);
      List<BranchDTO> branchDTOs = branches.getItems().stream().map(branchName -> new BranchDTO(branchName))
          .collect(Collectors.toList());
      branchDTO.setItems(branchDTOs);
      branchDTO.setCount(branches.getCount());
    }
    return branchDTO;
  }
}
