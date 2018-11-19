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

package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.AclUserDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDefaultValueDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.KafkaException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import org.apache.zookeeper.KeeperException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
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
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaService {

  private static final Logger LOGGER = Logger.getLogger(KafkaService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;

  public KafkaService() {
  }

  public void setProjectId(Integer projectId) {
    this.project = this.projectFacade.find(projectId);
  }

  public Project getProject() {
    return project;
  }

  /**
   * Gets the list of topics for this project
   *
   * @return 
   */
  @GET
  @Path("/topics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopics() {
    List<TopicDTO> listTopics = kafkaFacade.findTopicsByProject(project);
    GenericEntity<List<TopicDTO>> topics = new GenericEntity<List<TopicDTO>>(listTopics) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(topics).build();
  }

  @GET
  @Path("/sharedTopics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getSharedTopics() {
    List<TopicDTO> listTopics = kafkaController.findSharedTopicsByProject(project.getId());
    GenericEntity<List<TopicDTO>> topics = new GenericEntity<List<TopicDTO>>(listTopics) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(topics).build();
  }

  @GET
  @Path("/projectAndSharedTopics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getProjectAndSharedTopics() {
    List<TopicDTO> allTopics = kafkaFacade.findTopicsByProject(project);
    allTopics.addAll(kafkaController.findSharedTopicsByProject(project.getId()));
    GenericEntity<List<TopicDTO>> topics = new GenericEntity<List<TopicDTO>>(allTopics) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(topics).build();
  }

  @POST
  @Path("/topic/add")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response createTopic(TopicDTO topicDto) throws KafkaException, ProjectException, ServiceException,
      UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    //create the topic in the database and the Kafka cluster
    kafkaFacade.createTopicInProject(project.getId(), topicDto);
    //By default, all members of the project are granted full permissions 
    //on the topic
    AclDTO aclDto = new AclDTO(project.getName(),
            Settings.KAFKA_ACL_WILDCARD,
            "allow", Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD,
            Settings.KAFKA_ACL_WILDCARD);
    kafkaFacade.addAclsToTopic(topicDto.getName(), project.getId(), aclDto);

    json.setSuccessMessage("The Topic has been created.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @DELETE
  @Path("/topic/{topic}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeTopic(@PathParam("topic") String topicName) throws KafkaException, ServiceException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    //remove the topic from the database and Kafka cluster
    kafkaFacade.removeTopicFromProject(this.project, topicName);
    json.setSuccessMessage("The topic has been removed.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("/details/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopicDetails(@PathParam("topic") String topicName, @Context SecurityContext sc) throws
      Exception {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<PartitionDetailsDTO> topic;
    topic = kafkaFacade.getTopicDetails(project, user, topicName);
    GenericEntity<List<PartitionDetailsDTO>> topics = new GenericEntity<List<PartitionDetailsDTO>>(topic) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(topics).build();
  }

  @GET
  @Path("/topic/defaultValues")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response topicDefaultValues() throws KafkaException {
    TopicDefaultValueDTO values;
    try {
      values = kafkaFacade.topicDefaultValues();
    } catch (InterruptedException | IOException | KeeperException ex) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.KAFKA_GENERIC_ERROR, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
  
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(values).build();
  }

  @GET
  @Path("/topic/{topic}/share/{projId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response shareTopic(@PathParam("topic") String topicName, @PathParam("projId") int projectId) throws
      KafkaException, ProjectException, UserException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    //By default, all members of the project are granted full permissions on the topic
    Project projectShared = projectFacade.find(projectId);
    if (projectShared == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE,
        "Could not find project: " + projectId);
    }
    kafkaFacade.shareTopic(project, topicName, projectId);

    AclDTO aclDto = new AclDTO(projectShared.getName(),
            Settings.KAFKA_ACL_WILDCARD,
            "allow", Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD,
            Settings.KAFKA_ACL_WILDCARD);
    kafkaFacade.addAclsToTopic(topicName, project.getId(), aclDto);
    json.setSuccessMessage("The topic has been shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @DELETE
  @Path("/topic/{topic}/unshare/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unShareTopic(@PathParam("topic") String topicName, @PathParam("projectId") int projectId) throws
      KafkaException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    kafkaFacade.unShareTopic(topicName, project.getId());
    json.setSuccessMessage("Topic has been removed from shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @DELETE
  @Path("/topic/{topic}/unshare")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unShareTopicFromProject(@PathParam("topic") String topicName) throws KafkaException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    kafkaFacade.unShareTopic(topicName, project.getId());
    json.setSuccessMessage("Topic has been removed from shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("/{topic}/sharedwith")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response topicIsSharedTo(@PathParam("topic") String topicName) {
    List<SharedProjectDTO> projectDtoList = kafkaFacade.topicIsSharedTo(topicName, project.getId());
    GenericEntity<List<SharedProjectDTO>> projectDtos = new GenericEntity<List<SharedProjectDTO>>(projectDtoList) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectDtos).build();
  }

  @GET
  @Path("/aclUsers/topic/{topicName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response aclUsers(@PathParam("topicName") String topicName) {
    List<AclUserDTO> aclUsersDtos = kafkaFacade.aclUsers(project.getId(), topicName);
    GenericEntity<List<AclUserDTO>> aclUsers = new GenericEntity<List<AclUserDTO>>(aclUsersDtos) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(aclUsers).build();
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
    kafkaFacade.addAclsToTopic(topicName, project.getId(), aclDto);
    json.setSuccessMessage("ACL has been added to the topic.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("/acls/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTopicAcls(@PathParam("topic") String topicName) throws KafkaException {
    List<AclDTO> aclDto = kafkaFacade.getTopicAcls(topicName, project);
    GenericEntity<List<AclDTO>> aclDtos = new GenericEntity<List<AclDTO>>(aclDto) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(aclDtos).build();
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  //validate the new schema
  @POST
  @Path("/schema/validate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response ValidateSchemaForTopics(SchemaDTO schemaData)  {
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    switch (kafkaFacade.schemaBackwardCompatibility(schemaData)) {
      case INVALID:
        json.setErrorMsg("schema is invalid");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      case INCOMPATIBLE:
        json.setErrorMsg("schema is not backward compatible");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      default:
        json.setSuccessMessage("schema is valid");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
  }

  @POST
  @Path("/schema/add")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response addTopicSchema(SchemaDTO schemaData) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    //String schemaContent = schemaData.getContents();
    switch (kafkaFacade.schemaBackwardCompatibility(schemaData)) {

      case INVALID:
        json.setErrorMsg("schema is invalid");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      case INCOMPATIBLE:
        json.setErrorMsg("schema is not backward compatible");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_ACCEPTABLE).entity(json).build();
      default:
        kafkaFacade.addSchemaForTopics(schemaData);
        json.setSuccessMessage("Schema for Topic created/updated successfuly");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(schemas).build();
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(schemaDtos).build();
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

}
