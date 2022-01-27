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
package io.hops.hopsworks.api.git;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.git.GitCommitsFacade;
import io.hops.hopsworks.common.git.GitCommitDTO;
import io.hops.hopsworks.common.git.GitController;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.persistence.entity.git.GitCommit;
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
public class GitCommitsBuilder {
  @EJB
  private GitController gitController;
  @EJB
  private GitCommitsFacade gitCommitsFacade;

  private static final Integer DEFAULT_BRANCH_COMMITS_LIMIT = 20;

  private URI uri(UriInfo uriInfo, Project project, Integer repositoryId, String branchName) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repositoryId))
        .path(ResourceRequest.Name.BRANCH.toString())
        .path(branchName)
        .path(ResourceRequest.Name.COMMIT.toString())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.COMMIT);
  }

  public GitCommitDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                            GitRepository repository, String branchName) throws GitOpException {
    GitCommitDTO dto = new GitCommitDTO();
    dto.setHref(uri(uriInfo, project, repository.getId(), branchName));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      Integer limit = resourceRequest.getLimit() == null ? DEFAULT_BRANCH_COMMITS_LIMIT : resourceRequest.getLimit();
      Integer offset = resourceRequest.getOffset() == null ? 0 : resourceRequest.getOffset();
      AbstractFacade.CollectionInfo<GitCommit> branchCommits =
          gitCommitsFacade.getBranchCommits(repository, branchName, limit, offset);
      List<GitCommitDTO> commitDTOs = branchCommits.getItems().stream().map(gitCommit ->
          new GitCommitDTO(gitCommit.getName(), gitCommit.getEmail(), gitCommit.getMessage(), gitCommit.getHash(),
          gitCommit.getDate())).collect(Collectors.toList());
      dto.setItems(commitDTOs);
      dto.setCount(branchCommits.getCount());
    }
    return dto;
  }
}
