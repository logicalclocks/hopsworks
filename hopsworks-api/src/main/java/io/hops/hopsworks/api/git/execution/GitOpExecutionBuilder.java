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
package io.hops.hopsworks.api.git.execution;

import io.hops.hopsworks.api.git.GitRepositoryBuilder;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.git.GitOpExecutionFacade;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitOpExecutionBuilder {
  private static final Logger LOGGER = Logger.getLogger(GitOpExecutionBuilder.class.getName());
  private static final Integer DEFAULT_EXECUTIONS_LIMIT = 20;

  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private GitOpExecutionFacade gitOpExecutionFacade;
  @EJB
  private GitRepositoryBuilder repositoryBuilder;

  public URI uri(UriInfo uriInfo, Project project, Integer repositoryId, Integer executionId) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repositoryId))
        .path(ResourceRequest.Name.EXECUTION.toString())
        .path(Integer.toString(executionId))
        .build();
  }

  public URI uri(UriInfo uriInfo, Project project, Integer repositoryId) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repositoryId))
        .path(ResourceRequest.Name.EXECUTION.toString())
        .build();
  }

  public boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.EXECUTION);
  }

  public GitOpExecutionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                 GitRepository gitRepository) {
    GitOpExecutionDTO dto = new GitOpExecutionDTO();
    dto.setHref(uri(uriInfo, project, gitRepository.getId()));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      Integer limit = resourceRequest.getLimit() == null ? DEFAULT_EXECUTIONS_LIMIT : resourceRequest.getLimit();
      Integer offset = resourceRequest.getOffset() == null ? 0 : resourceRequest.getOffset();
      AbstractFacade.CollectionInfo<GitOpExecution> repositoryExecutions =
          gitOpExecutionFacade.getAllForRepository(gitRepository, limit, offset);
      List<GitOpExecutionDTO> dtos = repositoryExecutions.getItems().stream().map(e ->
          build(uriInfo, resourceRequest, e)).collect(Collectors.toList());
      dto.setItems(dtos);
      dto.setCount(repositoryExecutions.getCount());
    }
    return dto;
  }

  public GitOpExecutionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, GitOpExecution execution) {
    GitOpExecutionDTO dto = new GitOpExecutionDTO();
    dto.setHref(uri(uriInfo, execution.getRepository().getProject(), execution.getRepository().getId(),
        execution.getId()));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setId(execution.getId());
      dto.setExecutionStart(execution.getExecutionStart());
      dto.setExecutionStop(execution.getExecutionStop());
      dto.setSubmissionTime(execution.getSubmissionTime());
      dto.setConfigSecret(execution.getConfigSecret());
      dto.setCommandResultMessage(execution.getCommandResultMessage());
      dto.setGitCommandConfiguration(execution.getGitCommandConfiguration());
      dto.setState(execution.getState());
      dto.setRepository(repositoryBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.REPOSITORY),
          execution.getRepository().getProject(), execution.getRepository()));
      dto.setUser(usersBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.CREATOR),
          execution.getUser()));
    }
    return dto;
  }
}
