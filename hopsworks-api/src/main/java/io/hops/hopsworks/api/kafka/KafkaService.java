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
import io.hops.hopsworks.api.kafka.topics.TopicsBeanParam;
import io.hops.hopsworks.api.kafka.topics.TopicsBuilder;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.AclUserDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
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
import java.util.List;
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
  @Path("/topics2")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopics2(@BeanParam Pagination pagination, @BeanParam
    TopicsBeanParam topicsBeanParam) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KAFKA);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(topicsBeanParam.getSortBySet());
    resourceRequest.setFilter(topicsBeanParam.getFilter());
    List<TopicDTO> topicDTOList = topicsBuilder.build(project, resourceRequest);
    GenericEntity<List<TopicDTO>> entity = new GenericEntity<List<TopicDTO>>(topicDTOList) {};
    return Response.ok().entity(entity).build();
  }
  
  @ApiOperation(value = "Retrieve Kafka topics metadata .")
  @GET
  @Path("/topics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopics(@BeanParam Pagination pagination, @BeanParam
    TopicsBeanParam topicsBeanParam) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KAFKA);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(topicsBeanParam.getSortBySet());
    resourceRequest.setFilter(topicsBeanParam.getFilter());
    List<TopicDTO> topicDTOList = topicsBuilder.buildItems(project, resourceRequest);
    GenericEntity<List<TopicDTO>> entity = new GenericEntity<List<TopicDTO>>(topicDTOList) {};
    return Response.ok().entity(entity).build();
  }
  
  @ApiOperation(value = "Create a new Kafka topic.")
  @POST
  @Path("/topics")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response createTopic(TopicDTO topicDto, @Context SecurityContext sc)
    throws KafkaException, ProjectException, UserException,
      InterruptedException, ExecutionException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaController.createTopic(project, topicDto);
  
    json.setSuccessMessage("The Topic has been created.");
    return Response.ok().entity(json).build();
  }
  
  @ApiOperation(value = "Delete a Kafka topic.")
  @DELETE
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeTopic(@PathParam("topic") String topicName) throws KafkaException, ServiceException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaController.removeTopicFromProject(project, topicName);
    json.setSuccessMessage("The topic has been removed.");
    return Response.ok().entity(json).build();
  }
  
  @ApiOperation(value = "Get Kafka topic details.")
  @GET
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopic(@PathParam("topic") String topicName) throws KafkaException, InterruptedException,
    ExecutionException {
    
    List<PartitionDetailsDTO> topic = kafkaController.getTopicDetails(project, topicName).get();
    GenericEntity<List<PartitionDetailsDTO>> topics = new GenericEntity<List<PartitionDetailsDTO>>(topic) {};
    return Response.ok().entity(topics).build();
  }
  
  @ApiOperation(value = "Share a Kafka topic with a project.")
  @PUT
  @Path("/topics/{topic}/shared/{destProjectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response shareTopic(@PathParam("topic") String topicName, @PathParam("destProjectId") Integer destProjectId)
    throws KafkaException, ProjectException, UserException {
    
    kafkaController.shareTopicWithProject(project, topicName, destProjectId);
    
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("The topic has been shared.");
    return Response.ok().entity(json).build();
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
    
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("Topic has been unshared from all projects.");
    return Response.ok().entity(json).build();
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
    
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("Topic has been unshared.");
    return Response.ok().entity(json).build();
  }
  
  @ApiOperation(value = "Get list of projects that a topic has been shared with.")
  @GET
  @Path("/topics/{topic}/shared")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response topicIsSharedTo(@PathParam("topic") String topicName) {
    List<SharedProjectDTO> projectDtoList = kafkaController.getTopicSharedProjects(topicName, project.getId());
    GenericEntity<List<SharedProjectDTO>> projectDtos = new GenericEntity<List<SharedProjectDTO>>(projectDtoList) {};
    return Response.ok().entity(projectDtos).build();
  }

  @GET
  @Path("/aclUsers/topic/{topicName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response aclUsers(@PathParam("topicName") String topicName) {
    List<AclUserDTO> aclUsersDtos = kafkaFacade.aclUsers(project.getId(), topicName);
    GenericEntity<List<AclUserDTO>> aclUsers = new GenericEntity<List<AclUserDTO>>(aclUsersDtos) {};
    return Response.ok().entity(aclUsers).build();
  }

  @POST
  @Path("/topic/{topic}/addAcl")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response addAclsToTopic(@PathParam("topic") String topicName, AclDTO aclDto) throws KafkaException,
      ProjectException, UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaController.addAclsToTopic(topicName, project.getId(), aclDto);
    json.setSuccessMessage("ACL has been added to the topic.");
    return Response.ok().entity(json).build();
  }

  @DELETE
  @Path("/topic/{topic}/removeAcl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeAclsFromTopic(@PathParam("topic") String topicName, @PathParam("aclId") int aclId) throws
      KafkaException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    kafkaFacade.removeAclFromTopic(topicName, aclId);
    json.setSuccessMessage("Topic acls has been removed.");
    return Response.ok().entity(json).build();
  }

  @GET
  @Path("/acls/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopicAcls(@PathParam("topic") String topicName) throws KafkaException {
    List<AclDTO> aclDto = kafkaFacade.getTopicAcls(topicName, project);
    GenericEntity<List<AclDTO>> aclDtos = new GenericEntity<List<AclDTO>>(aclDto) {};
    return Response.ok().entity(aclDtos).build();
  }

  @PUT
  @Path("/topic/{topic}/updateAcl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response updateTopicAcls(@PathParam("topic") String topicName, @PathParam("aclId") String aclId, AclDTO aclDto)
      throws KafkaException, ProjectException, UserException {
    kafkaFacade.updateTopicAcl(project, topicName, Integer.parseInt(aclId), aclDto);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("TopicAcl updated successfully");
    return Response.ok().entity(json).build();
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
