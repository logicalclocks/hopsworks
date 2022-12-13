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

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.git.GitCommandExecutionStateUpdateDTO;
import io.hops.hopsworks.common.git.GitExecutionController;
import io.hops.hopsworks.common.git.util.GitCommandConfigurationValidator;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Api(value = "Git Execution Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitExecutionResource {
  @EJB
  private GitExecutionController executionController;
  @EJB
  private GitOpExecutionBuilder gitOpExecutionBuilder;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private GitCommandConfigurationValidator gitCommandConfigurationValidator;

  private GitRepository gitRepository;
  private Project project;

  public GitExecutionResource setRepository(GitRepository gitRepository) {
    this.gitRepository = gitRepository;
    this.project = gitRepository.getProject();
    return this;
  }

  @ApiOperation(value = "Get all executions in a repository", response = GitOpExecutionDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getRepositoryExecutions(@Context UriInfo uriInfo,
                                          @Context SecurityContext sc,
                                          @BeanParam ExecutionBeanParam executionBeanParam,
                                          @BeanParam Pagination pagination) throws GitOpException {
    Users hopsworksUser = jwtHelper.getUserPrincipal(sc);
    gitCommandConfigurationValidator.verifyRepository(project, hopsworksUser, gitRepository.getId());
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setOffset(pagination.getOffset());
    GitOpExecutionDTO dto = gitOpExecutionBuilder.build(uriInfo, resourceRequest, project, gitRepository);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get execution of particular Id in a repository", response = GitOpExecutionDTO.class)
  @GET
  @Path("/{executionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecution(@PathParam("executionId") Integer executionId, @Context SecurityContext sc,
                               @Context UriInfo uriInfo,
                               @BeanParam ExecutionBeanParam executionBeanParam) throws GitOpException {
    Users hopsworksUser = jwtHelper.getUserPrincipal(sc);
    gitCommandConfigurationValidator.verifyRepository(project, hopsworksUser, gitRepository.getId());
    GitOpExecution executionObj = executionController.getExecutionInRepository(gitRepository, executionId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = gitOpExecutionBuilder.build(uriInfo, resourceRequest, executionObj);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Update the state of the running git operation", response = GitOpExecutionDTO.class)
  @PUT
  @Path("/{executionId}/state")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateGitCommandExecutionState(@PathParam("executionId") Integer executionId,
                                                 @PathParam("repositoryId") Integer repositoryId,
                                                 GitCommandExecutionStateUpdateDTO stateUpdateDTO,
                                                 @Context SecurityContext sc,
                                                 @Context UriInfo uriInfo,
                                                 @BeanParam ExecutionBeanParam executionBeanParam)
      throws GitOpException, IllegalArgumentException {
    Users hopsworksUser = jwtHelper.getUserPrincipal(sc);
    GitOpExecution newExec = executionController.updateGitExecutionState(project,hopsworksUser, stateUpdateDTO,
        repositoryId, executionId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = gitOpExecutionBuilder.build(uriInfo, resourceRequest, newExec);
    return Response.ok().entity(dto).build();
  }
}
