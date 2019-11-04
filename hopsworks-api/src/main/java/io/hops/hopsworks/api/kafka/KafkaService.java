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
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopics;
import io.hops.hopsworks.common.dao.kafka.SharedTopicsDTO;
import io.hops.hopsworks.common.dao.kafka.SharedTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicAcls;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
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
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaService {

  private static final Logger LOGGER = Logger.getLogger(KafkaService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private TopicsBuilder topicsBuilder;
  @EJB
  private SharedTopicsFacade sharedTopicsFacade;
  @EJB
  private AclBuilder aclBuilder;

  private Project project;

  public KafkaService() {
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
    @BeanParam TopicsBeanParam topicsBeanParam) {
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
  public Response createTopic(TopicDTO topicDto, @Context UriInfo uriInfo)
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
  public Response removeTopic(@PathParam("topic") String topicName) throws KafkaException, ServiceException {
    kafkaController.removeTopicFromProject(project, topicName);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get Kafka topic details.")
  @GET
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopic(@Context UriInfo uriInfo, @PathParam("topic") String topicName)
    throws KafkaException, InterruptedException, ExecutionException {
    
    PartitionDetailsDTO dto = topicsBuilder.buildTopicDetails(uriInfo, project, topicName);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Share a Kafka topic with a project.")
  @PUT
  @Path("/topics/{topic}/shared/{destProjectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response shareTopic(@PathParam("topic") String topicName, @PathParam("destProjectId") Integer destProjectId,
    @Context UriInfo uriInfo) throws KafkaException, ProjectException, UserException {
    URI uri = topicsBuilder.sharedProjectUri(uriInfo, project, topicName).build();
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
  
  @ApiOperation(value = "Unshare Kafka topic from all projects.")
  @DELETE
  @Path("/topics/{topic}/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unshareTopicFromProjects(@PathParam("topic") String topicName)
    throws KafkaException, ProjectException {
    
    kafkaController.unshareTopicFromAllProjects(project, topicName);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Unshare Kafka topic from a project (specified as destProjectId).")
  @DELETE
  @Path("/topics/{topic}/shared/{destProjectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unshareTopicFromProject(@PathParam("topic") String topicName,
    @PathParam("destProjectId") Integer destProjectId) throws KafkaException, ProjectException {
    
    kafkaController.unshareTopic(project, topicName, destProjectId);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get list of projects that a topic has been shared with.")
  @GET
  @Path("/topics/{topic}/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response topicIsSharedTo(@Context UriInfo uriInfo, @PathParam("topic") String topicName) {
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
    @BeanParam AclsBeanParam aclsBeanParam) {
    
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
  public Response addAclsToTopic(@Context UriInfo uriInfo, @PathParam("topic") String topicName, AclDTO aclDto)
    throws KafkaException, ProjectException, UserException {
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
  public Response removeAclsFromTopic(@PathParam("topic") String topicName, @PathParam("id") Integer aclId) throws
      KafkaException {
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
    @PathParam("id") Integer aclId) throws KafkaException {
    
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
    @PathParam("id") Integer aclId, AclDTO aclDto)
      throws KafkaException, ProjectException, UserException {
    
    Integer updatedAclId = kafkaController.updateTopicAcl(project, topicName, aclId, aclDto);
    aclDto.setId(updatedAclId);
    URI uri = aclBuilder.getAclUri(uriInfo, project, topicName)
      .path(Integer.toString(updatedAclId))
      .build();
    aclDto.setHref(uri);
    return Response.ok(uri).entity(aclDto).build();
  }

  //validate the new schema
  @POST
  @Path("/schema/validate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response ValidateSchemaForTopics(SchemaDTO schemaData) throws KafkaException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaFacade.validateSchema(schemaData);
    switch (kafkaFacade.schemaBackwardCompatibility(schemaData)) {
      case INVALID:
        json.setErrorMsg("schema is invalid");
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      case INCOMPATIBLE:
        json.setErrorMsg("schema is not backward compatible");
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      default:
        json.setSuccessMessage("schema is valid");
        return Response.ok().entity(json).build();
    }
  }

  @POST
  @Path("/schema/add")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response addTopicSchema(SchemaDTO schemaData) throws KafkaException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaFacade.validateSchema(schemaData);
    switch (kafkaFacade.schemaBackwardCompatibility(schemaData)) {
      case INVALID:
        json.setErrorMsg("schema is invalid");
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      case INCOMPATIBLE:
        json.setErrorMsg("schema is not backward compatible");
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      default:
        kafkaFacade.addSchemaForTopics(schemaData);
        json.setSuccessMessage("Schema for Topic created/updated successfuly");
        return Response.ok().entity(json).build();
    }
  }

  //This API used is to select a schema and its version from the list
  //of available schemas when creating a topic. 
  @GET
  @Path("/schemas")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response listSchemasForTopics() {
    List<SchemaDTO> schemaDtos = kafkaFacade.listSchemasForTopics();
    GenericEntity<List<SchemaDTO>> schemas = new GenericEntity<List<SchemaDTO>>(schemaDtos) {};
    return Response.ok().entity(schemas).build();
  }
  
  
  @GET
  @Path("/{topic}/schema")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getSchema(@PathParam("topic") String topic) {
    SchemaDTO schemaDto = kafkaFacade.getSchemaForTopic(topic);
    if (schemaDto != null) {
      return Response.ok().
        entity(schemaDto).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
  
  @POST
  @Path("/{topic}/schema/version/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateSchemaVersion(@PathParam("topic") String topic, @PathParam("version") Integer version)
  throws KafkaException {
    kafkaController.updateTopicSchemaVersion(project, topic, version);
    return Response.ok().build();
  }
  

  //This API used to select a schema and its version from the list
  //of available schemas when listing all the available schemas.
  @GET
  @Path("/showSchema/{schemaName}/{schemaVersion}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSchemaContent(@PathParam("schemaName") String schemaName,
      @PathParam("schemaVersion") Integer schemaVersion) throws KafkaException {
    SchemaDTO schemaDtos = kafkaFacade.getSchemaContent(schemaName, schemaVersion);
    return Response.ok().entity(schemaDtos).build();
  }

  //delete the specified version of the given schema.
  @DELETE
  @Path("/removeSchema/{schemaName}/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteSchema(@PathParam("schemaName") String schemaName, @PathParam("version") Integer version) throws
      KafkaException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaFacade.deleteSchema(schemaName, version);
    json.setSuccessMessage("Schema version for topic removed successfuly");
    return Response.ok().entity(json).build();
  }

}
