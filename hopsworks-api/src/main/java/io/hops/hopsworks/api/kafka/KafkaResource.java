/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.kafka.acls.AclBuilder;
import io.hops.hopsworks.api.kafka.acls.AclsBeanParam;
import io.hops.hopsworks.api.kafka.topics.TopicsBeanParam;
import io.hops.hopsworks.api.kafka.topics.TopicsBuilder;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopics;
import io.hops.hopsworks.common.dao.kafka.SharedTopicsDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicAcls;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.schemas.Compatibility;
import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityCheck;
import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityLevel;
import io.hops.hopsworks.common.dao.kafka.schemas.SchemaRegistryError;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.kafka.SchemasController;
import io.hops.hopsworks.common.kafka.SubjectsCompatibilityController;
import io.hops.hopsworks.common.kafka.SubjectsController;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.tuple.Pair;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaResource {

  private static final Logger LOGGER = Logger.getLogger(KafkaResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private TopicsBuilder topicsBuilder;
  @EJB
  private SharedTopicsFacade sharedTopicsFacade;
  @EJB
  private AclBuilder aclBuilder;
  @EJB
  private SubjectsController subjectsController;
  @EJB
  private SubjectsCompatibilityController subjectsCompatibilityController;
  @EJB
  private SchemasController schemasController;

  private Project project;

  public KafkaResource() {
  }
  public void setProjectId(Integer projectId) {
    this.project = this.projectFacade.find(projectId);
  }
  public Project getProject() {
    return project;
  }
  
  @ApiOperation(value = "Retrieve Kafka topics metadata .")
  @GET
  @Path("/topics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopics(
    @Context UriInfo uriInfo,
    @BeanParam Pagination pagination,
    @BeanParam TopicsBeanParam topicsBeanParam, @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KAFKA);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(topicsBeanParam.getSortBySet());
    resourceRequest.setFilter(topicsBeanParam.getFilter());
    TopicDTO dto = topicsBuilder.build(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Create a new Kafka topic.")
  @POST
  @Path("/topics")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response createTopic(TopicDTO topicDto, @Context UriInfo uriInfo, @Context SecurityContext sc)
    throws KafkaException, ProjectException, UserException,
      InterruptedException, ExecutionException {
    kafkaController.createTopic(project, topicDto, uriInfo);
    URI uri = uriInfo.getAbsolutePathBuilder().path(topicDto.getName()).build();
    topicDto.setHref(uri);
    return Response.created(uri).entity(topicDto).build();
  }
  
  @ApiOperation(value = "Delete a Kafka topic.")
  @DELETE
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeTopic(@PathParam("topic") String topicName, @Context SecurityContext sc) throws KafkaException {
    kafkaController.removeTopicFromProject(project, topicName);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get Kafka topic details.")
  @GET
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopic(@Context UriInfo uriInfo, @PathParam("topic") String topicName, @Context SecurityContext sc)
    throws KafkaException, InterruptedException, ExecutionException {
    
    PartitionDetailsDTO dto = topicsBuilder.buildTopicDetails(uriInfo, project, topicName);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Share a Kafka topic with a project.")
  @PUT
  @Path("/topics/{topic}/shared/{destProjectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response shareTopic(@PathParam("topic") String topicName, @PathParam("destProjectName") String destProjectName,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws KafkaException, ProjectException, UserException {
    URI uri = topicsBuilder.sharedProjectUri(uriInfo, project, topicName).build();
    Integer destProjectId = projectFacade.findByName(destProjectName).getId();
    Optional<SharedTopics> st = sharedTopicsFacade.findSharedTopicByProjectAndTopic(destProjectId, topicName);
    SharedTopicsDTO dto;
    if (st.isPresent()) {
      dto = new SharedTopicsDTO(st.get().getProjectId(), st.get().getSharedTopicsPK());
      dto.setHref(uri);
      return Response.ok(uri).entity(dto).build();
    } else {
      dto = kafkaController.shareTopicWithProject(project, topicName, destProjectId);
      dto.setHref(uri);
      return Response.created(uri).entity(dto).build();
    }
  }
  
  @ApiOperation(value = "Unshare Kafka topic from all projects if request is issued from the project owning the topic" +
    ". Other unshare it from the requester project.")
  @DELETE
  @Path("/topics/{topic}/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unshareTopicFromProjects(@PathParam("topic") String topicName, @Context SecurityContext sc)
    throws KafkaException, ProjectException {
    
    kafkaController.unshareTopic(project, topicName, null);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Unshare Kafka topic from shared-with project (specified as destProjectId). Request must be " +
    "issued from the owning project.")
  @DELETE
  @Path("/topics/{topic}/shared/{destProjectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unshareTopicFromProject(@PathParam("topic") String topicName,
    @PathParam("destProjectName") String destProjectName, @Context SecurityContext sc) throws KafkaException,
    ProjectException {
    
    kafkaController.unshareTopic(project, topicName, destProjectName);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get list of projects that a topic has been shared with.")
  @GET
  @Path("/topics/{topic}/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response topicIsSharedTo(@Context UriInfo uriInfo, @PathParam("topic") String topicName,
    @Context SecurityContext sc) {
    SharedProjectDTO dto = topicsBuilder.buildSharedProject(uriInfo, project, topicName);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get all ACLs for a specified topic.")
  @GET
  @Path("/topics/{topic}/acls")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopicAcls(@Context UriInfo uriInfo,
    @PathParam("topic") String topicName,
    @BeanParam Pagination pagination,
    @BeanParam AclsBeanParam aclsBeanParam, @Context SecurityContext sc) {
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KAFKA);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(aclsBeanParam.getSortBySet());
    resourceRequest.setFilter(aclsBeanParam.getFilter());
    AclDTO dto = aclBuilder.build(uriInfo, project, topicName, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Add a new ACL for a specified topic.")
  @POST
  @Path("/topics/{topic}/acls")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response addAclsToTopic(@Context UriInfo uriInfo, @PathParam("topic") String topicName, AclDTO aclDto,
    @Context SecurityContext sc) throws KafkaException, ProjectException, UserException {
    Pair<TopicAcls, Response.Status> aclTuple = kafkaController.addAclsToTopic(topicName, project.getId(), aclDto);
    AclDTO dto = aclBuilder.build(uriInfo, aclTuple.getLeft());
    return Response.status(aclTuple.getRight()).entity(dto).build();
  }

  @ApiOperation(value = "Remove ACL specified by id.")
  @DELETE
  @Path("/topics/{topic}/acls/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeAclsFromTopic(@PathParam("topic") String topicName, @PathParam("id") Integer aclId,
    @Context SecurityContext sc) throws KafkaException {
    kafkaController.removeAclFromTopic(topicName, aclId);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get ACL metadata specified by id.")
  @GET
  @Path("/topics/{topic}/acls/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopicAcl(@Context UriInfo uriInfo,
    @PathParam("topic") String topicName,
    @PathParam("id") Integer aclId, @Context SecurityContext sc) throws KafkaException {
    
    AclDTO dto = aclBuilder.getAclByTopicAndId(uriInfo, project, topicName, aclId);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Update ACL specified by id.")
  @PUT
  @Path("/topics/{topic}/acls/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response updateTopicAcls(@Context UriInfo uriInfo, @PathParam("topic") String topicName,
    @PathParam("id") Integer aclId, AclDTO aclDto, @Context SecurityContext sc)
      throws KafkaException, ProjectException, UserException {
    
    Integer updatedAclId = kafkaController.updateTopicAcl(project, topicName, aclId, aclDto);
    aclDto.setId(updatedAclId);
    URI uri = aclBuilder.getAclUri(uriInfo, project, topicName)
      .path(Integer.toString(updatedAclId))
      .build();
    aclDto.setHref(uri);
    return Response.ok(uri).entity(aclDto).build();
  }

  @ApiOperation(value = "Get schema by its id.")
  @GET
  @Path("/schemas/ids/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSchema(@PathParam("id") Integer id, @Context SecurityContext sc) {
    try {
      SubjectDTO dto = schemasController.findSchemaById(project, id);
      return Response.ok().entity(dto).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get all registered subjects as a list.")
  @GET
  @Path("/subjects")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSubjects(@Context SecurityContext sc) {
    List<String> subjects = subjectsController.getSubjects(project);
    String array = Arrays.toString(subjects.toArray());
    GenericEntity<String> entity = new GenericEntity<String>(array) {};
    return Response.ok().entity(entity).build();
  }
  
  @ApiOperation(value = "Check if the provided schema is registered under specified subject.")
  @POST
  @Path("/subjects/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response checkIfSubjectRegistered(@PathParam("subject") String subject, SubjectDTO dto,
    @Context SecurityContext sc) {
    try {
      SubjectDTO res = subjectsController.checkIfSchemaRegistered(project, subject, dto.getSchema());
      return Response.ok().entity(res).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Delete a subject with all its versions and its compatibility if exists.")
  @DELETE
  @Path("/subjects/{subject}")
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteSubject(@PathParam("subject") String subject, @Context SecurityContext sc)
    throws KafkaException {
    try {
      List<Integer> versions = subjectsController.deleteSubject(project, subject);
      String array = Arrays.toString(versions.toArray());
      GenericEntity<String> entity = new GenericEntity<String>(array) {};
      return Response.ok().entity(entity).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Delete a specific version of a subject (and its compatibility).")
  @DELETE
  @Path("/subjects/{subject}/versions/{version}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteSubjectsVersion(@PathParam("subject") String subject, @PathParam("version") String version,
    @Context SecurityContext sc) throws KafkaException {
    try {
      Integer deleted = subjectsController.deleteSubjectsVersion(project, subject, version);
      GenericEntity<Integer> entity = new GenericEntity<Integer>(deleted) {};
      return Response.ok().entity(entity).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Upload a new schema.")
  @POST
  @Path("/subjects/{subject}/versions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response postNewSchema(@PathParam("subject") String subject, SubjectDTO dto, @Context SecurityContext sc)
    throws KafkaException {
    try {
      SubjectDTO res = subjectsController.registerNewSubject(project, subject, dto.getSchema(), false);
      return Response.ok().entity(res).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get list of versions of a registered subject.")
  @GET
  @Path("/subjects/{subject}/versions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSubjectVersions(@PathParam("subject") String subject, @Context SecurityContext sc) {
    try {
      List<Integer> versions = subjectsController.getSubjectVersions(project, subject);
      String array = Arrays.toString(versions.toArray());
      GenericEntity<String> entity = new GenericEntity<String>(array) {};
      return Response.ok().entity(entity).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get details of a specific version of a subject.")
  @GET
  @Path("/subjects/{subject}/versions/{version}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSubjectDetails(@PathParam("subject") String subject, @PathParam("version") String version,
    @Context SecurityContext sc) {
    try {
      SubjectDTO dto = subjectsController.getSubjectDetails(project, subject, version);
      return Response.ok().entity(dto).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get avro schema of a subject and version.")
  @GET
  @Path("/subjects/{subject}/versions/{version}/schema")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSchema(@PathParam("subject") String subject, @PathParam("version") String version,
    @Context SecurityContext sc) {
    try {
      SubjectDTO dto = subjectsController.getSubjectDetails(project, subject, version);
      GenericEntity<String> entity = new GenericEntity<String>(dto.getSchema()) {};
      return Response.ok().entity(entity).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Check if schema is compatible with a specific subject version.")
  @POST
  @Path("/compatibility/subjects/{subject}/versions/{version}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response checkSchemaCompatibility(@PathParam("subject") String subject,
    @PathParam("version") String version, SubjectDTO dto, @Context SecurityContext sc) {
    try {
      CompatibilityCheck isCompatible =
        subjectsController.checkIfSchemaCompatible(project, subject, version, dto.getSchema());
      return Response.ok().entity(isCompatible).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get project compatibility level.")
  @GET
  @Path("/config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getProjectCompatibility(@Context SecurityContext sc) {
    try {
      CompatibilityLevel dto = subjectsCompatibilityController.getProjectCompatibilityLevel(project);
      return Response.ok().entity(dto).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Set project compatibility level.")
  @PUT
  @Path("/config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
  public Response setProjectCompatibility(Compatibility dto, @Context SecurityContext sc) {
    try {
      Compatibility result = subjectsCompatibilityController.setProjectCompatibility(project, dto);
      return Response.ok().entity(result).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get subject's compatibility level.")
  @GET
  @Path("/config/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSubjectCompatibility(@PathParam("subject") String subject, @Context SecurityContext sc) {
    try {
      CompatibilityLevel dto = subjectsCompatibilityController.getSubjectCompatibility(project, subject);
      return Response.ok().entity(dto).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Set subject's compatibility level.")
  @PUT
  @Path("/config/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
  public Response setSubjectCompatibility(@PathParam("subject") String subject, Compatibility dto,
    @Context SecurityContext sc){
    try {
      Compatibility result = subjectsCompatibilityController.setSubjectCompatibility(project, subject, dto);
      return Response.ok().entity(result).build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
  
  @ApiOperation(value = "Get topics subject details.")
  @GET
  @Path("/topics/{topic}/subjects")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopicSubject(@PathParam("topic") String topic, @Context SecurityContext sc) throws KafkaException {
    SubjectDTO subjectDTO = kafkaController.getSubjectForTopic(project, topic);
    return Response.ok().entity(subjectDTO).build();
  }
  
  @ApiOperation(value = "Update subject version for a specified topic.")
  @PUT
  @Path("/topics/{topic}/subjects/{subject}/versions/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateSubjectVersion(@PathParam("topic") String topic, @PathParam("subject") String subject,
    @PathParam("version") Integer version, @Context SecurityContext sc) throws KafkaException {
    try {
      kafkaController.updateTopicSubjectVersion(project, topic, subject, version);
      return Response.ok().build();
    } catch (SchemaException e) {
      SchemaRegistryError error =
        new SchemaRegistryError(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
      return Response.status(e.getErrorCode().getRespStatus()).entity(error).build();
    }
  }
}
