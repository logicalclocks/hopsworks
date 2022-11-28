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
package io.hops.hopsworks.api.git.repository;

import io.hops.hopsworks.api.git.execution.GitOpExecutionBuilder;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.git.GitCommitsFacade;
import io.hops.hopsworks.common.dao.git.GitOpExecutionFacade;
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.git.GitCommitDTO;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.git.GitCommit;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitRepositoryBuilder {
  private static final Logger LOGGER = Logger.getLogger(GitRepositoryFacade.class.getName());
  private static final int DEFAULT_REPOSITORY_LIMIT = 20;
  @EJB
  private GitRepositoryFacade gitRepositoryFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private GitOpExecutionFacade gitOpExecutionFacade;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private GitOpExecutionBuilder gitOpExecutionBuilder;
  @EJB
  private GitCommitsFacade gitCommitsFacade;
  @EJB
  private Settings settings;

  private URI uri(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project, Integer repositoryId) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repositoryId))
        .build();
  }

  public boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.REPOSITORY);
  }

  public GitRepositoryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                GitRepository repository) {
    GitRepositoryDTO repositoryDTO = new GitRepositoryDTO();
    repositoryDTO.setHref(uri(uriInfo, project, repository.getId()));
    repositoryDTO.setExpand(expand(resourceRequest));
    if (repositoryDTO.isExpand()) {
      repositoryDTO.setId(repository.getId());
      repositoryDTO.setName(repository.getInode().getInodePK().getName());
      repositoryDTO.setPath(inodeController.getPath(repository.getInode()));
      repositoryDTO.setProvider(repository.getGitProvider());
      repositoryDTO.setCurrentBranch(repository.getCurrentBranch());
      repositoryDTO.setCreator(
          usersBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.CREATOR), repository.getCreator()));
      repositoryDTO.setReadOnly(settings.getEnableGitReadOnlyRepositories());
      Optional<GitCommit> commitOptional = gitCommitsFacade.findByHashAndRepository(repository);
      if (commitOptional.isPresent()) {
        GitCommit commit = commitOptional.get();
        GitCommitDTO gitCommitDTO = new GitCommitDTO(commit.getName(), commit.getEmail(), commit.getMessage(),
            commit.getHash(), commit.getDate());
        gitCommitDTO.setExpand(true);
        repositoryDTO.setCurrentCommit(gitCommitDTO);
      }

      //check if repository has an ongoing operation
      if (repository.getCid() != null) {
        Optional<GitOpExecution> optional = gitOpExecutionFacade.findRunningInRepository(repository);
        if (optional.isPresent()) {
          repositoryDTO.setOngoingOperation(gitOpExecutionBuilder.build(uriInfo, null, optional.get()));
        } else {
          LOGGER.log(Level.SEVERE, "There is a repository in locked state but with no execution object");
        }
      }
    }
    return repositoryDTO;
  }

  public GitRepositoryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user) {
    GitRepositoryDTO repoDTO = new GitRepositoryDTO();
    repoDTO.setHref(uri(uriInfo, project));
    repoDTO.setExpand(expand(resourceRequest));
    if (repoDTO.isExpand()) {
      Integer limit = resourceRequest.getLimit() == null ? DEFAULT_REPOSITORY_LIMIT : resourceRequest.getLimit();
      Integer offset = resourceRequest.getOffset() == null ? 0 : resourceRequest.getOffset();
      AbstractFacade.CollectionInfo<GitRepository> projectRepos = gitRepositoryFacade.getAllInProjectForUser(project,
              user, resourceRequest.getFilter(), resourceRequest.getSort(), limit, offset);
      List<GitRepositoryDTO> dtos = projectRepos.getItems().stream().map(r -> build(uriInfo, resourceRequest, project,
          r)).collect(Collectors.toList());
      repoDTO.setItems(dtos);
      repoDTO.setCount(projectRepos.getCount());
    }
    return repoDTO;
  }
}
