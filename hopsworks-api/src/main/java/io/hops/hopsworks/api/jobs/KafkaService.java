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

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.util.JsonResponse;
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
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.util.Settings;
import javax.persistence.EntityExistsException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
//import org.apache.avro.Schema;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaService {

  private final static Logger logger = Logger.getLogger(KafkaService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private UserFacade userFacade;

  private Integer projectId;
  private Project project;

  public KafkaService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  /**
   * Gets the list of topics for this project
   *
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/topics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getTopics(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    List<TopicDTO> listTopics = kafkaFacade.findTopicsByProject(projectId);
    GenericEntity<List<TopicDTO>> topics
            = new GenericEntity<List<TopicDTO>>(listTopics) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            topics).build();
  }

  @GET
  @Path("/sharedTopics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getSharedTopics(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    List<TopicDTO> listTopics = kafkaController.findSharedTopicsByProject(projectId);
    GenericEntity<List<TopicDTO>> topics
            = new GenericEntity<List<TopicDTO>>(listTopics) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            topics).build();
  }

  @GET
  @Path("/projectAndSharedTopics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getProjectAndSharedTopics(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    List<TopicDTO> allTopics = kafkaFacade.findTopicsByProject(projectId);

    allTopics.addAll(kafkaController.findSharedTopicsByProject(projectId));
    GenericEntity<List<TopicDTO>> topics
            = new GenericEntity<List<TopicDTO>>(allTopics) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            topics).build();
  }

  @POST
  @Path("/topic/add")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response createTopic(TopicDTO topicDto,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    //create the topic in the database and the Kafka cluster
    kafkaFacade.createTopicInProject(projectId, topicDto);
    //By default, all members of the project are granted full permissions 
    //on the topic
    AclDTO aclDto = new AclDTO(project.getName(),
            Settings.KAFKA_ACL_WILDCARD,
            "allow", Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD,
            Settings.KAFKA_ACL_WILDCARD);
    kafkaFacade.addAclsToTopic(topicDto.getName(), projectId, aclDto);

    json.setSuccessMessage("The Topic has been created.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("/topic/{topic}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removeTopic(@PathParam("topic") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    //remove the topic from the database and Kafka cluster
    kafkaFacade.removeTopicFromProject(this.project, topicName);

    json.setSuccessMessage("The topic has been removed.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/details/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getTopicDetails(@PathParam("topic") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(userEmail);
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    List<PartitionDetailsDTO> topic;
    try {
      topic = kafkaFacade.getTopicDetails(project, user, topicName);
    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Advanced option is currently not available. Please try again later.");
    }

    GenericEntity<List<PartitionDetailsDTO>> topics
            = new GenericEntity<List<PartitionDetailsDTO>>(topic) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            topics).build();
  }

  @GET
  @Path("/topic/defaultValues")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response topicDefaultValues(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    TopicDefaultValueDTO values
            = kafkaFacade.topicDefaultValues();

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            values).build();
  }

  @GET
  @Path("/topic/{topic}/share/{projId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response shareTopic(
          @PathParam("topic") String topicName,
          @PathParam("projId") int projectId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (this.projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    kafkaFacade.shareTopic(this.projectId, topicName, projectId);
    //By default, all members of the project are granted full permissions 
    //on the topic
    Project projectShared = projectFacade.find(projectId);
    if (projectShared == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not find project for topic");
    }
    AclDTO aclDto = new AclDTO(projectShared.getName(),
            Settings.KAFKA_ACL_WILDCARD,
            "allow", Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD,
            Settings.KAFKA_ACL_WILDCARD);
    kafkaFacade.addAclsToTopic(topicName, this.projectId, aclDto);
    json.setSuccessMessage("The topic has been shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("/topic/{topic}/unshare/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response unShareTopic(
          @PathParam("topic") String topicName,
          @PathParam("projectId") int projectId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    kafkaFacade.unShareTopic(topicName, this.projectId, projectId);
    json.setSuccessMessage("Topic has been removed from shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("/topic/{topic}/unshare")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response unShareTopicFromProject(
          @PathParam("topic") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    kafkaFacade.unShareTopic(topicName, this.projectId);
    json.setSuccessMessage("Topic has been removed from shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/{topic}/sharedwith")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response topicIsSharedTo(@PathParam("topic") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    List<SharedProjectDTO> projectDtoList = kafkaFacade
            .topicIsSharedTo(topicName, this.projectId);

    GenericEntity<List<SharedProjectDTO>> projectDtos
            = new GenericEntity<List<SharedProjectDTO>>(projectDtoList) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projectDtos).build();
  }

  @GET
  @Path("/aclUsers/topic/{topicName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response aclUsers(@PathParam("topicName") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    List<AclUserDTO> aclUsersDtos
            = kafkaFacade.aclUsers(projectId, topicName);

    GenericEntity<List<AclUserDTO>> aclUsers
            = new GenericEntity<List<AclUserDTO>>(aclUsersDtos) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            aclUsers).build();
  }

  @POST
  @Path("/topic/{topic}/addAcl")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response addAclsToTopic(@PathParam("topic") String topicName,
          AclDTO aclDto,
          @Context SecurityContext sc, @Context HttpServletRequest req)
          throws AppException {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    boolean aclAdded = false;
    try {
      aclAdded = kafkaFacade.addAclsToTopic(topicName, projectId, aclDto);
    } catch (EntityExistsException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "This ACL definition already existes in database.");
    } catch (IllegalArgumentException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Wrong imput values");
    }

    if(aclAdded){
      json.setSuccessMessage("ACL has been added to the topic.");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "ACL(s) already exist(s).");
    }
    
  }

  @DELETE
  @Path("/topic/{topic}/removeAcl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removeAclsFromTopic(@PathParam("topic") String topicName,
          @PathParam("aclId") int aclId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    try {
      kafkaFacade.removeAclsFromTopic(topicName, aclId);
    } catch (IllegalArgumentException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Wrong imput values");
    }

    json.setSuccessMessage("Topic acls has been removed.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/acls/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getTopicAcls(@PathParam("topic") String topicName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    List<AclDTO> aclDto = null;
    try {
      aclDto = kafkaFacade.getTopicAcls(topicName, projectId);
    } catch (AppException e) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    GenericEntity<List<AclDTO>> aclDtos
            = new GenericEntity<List<AclDTO>>(aclDto) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            aclDtos).build();
  }

  @PUT
  @Path("/topic/{topic}/updateAcl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response updateTopicAcls(@PathParam("topic") String topicName,
          @PathParam("aclId") String aclId, AclDTO aclDto,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    try {
      kafkaFacade.updateTopicAcl(projectId, topicName, Integer.parseInt(aclId),
              aclDto);
    } catch (EntityExistsException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "This ACL definition already existes in database.");
    } catch (IllegalArgumentException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Wrong imput values");
    } catch (Exception ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Problem adding ACL to topic.");
    }

    json.setSuccessMessage("TopicAcl updated successfuly");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  //validate the new schema
  @POST
  @Path("/schema/validate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response ValidateSchemaForTopics(SchemaDTO schemaData,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    switch (kafkaFacade.schemaBackwardCompatibility(schemaData)) {

      case INVALID:
        json.setErrorMsg("schema is invalid");
        return noCacheResponse.getNoCacheResponseBuilder(
                Response.Status.NOT_ACCEPTABLE).entity(
                        json).build();
      case INCOMPATIBLE:
        json.setErrorMsg("schema is not backward compatible");
        return noCacheResponse.getNoCacheResponseBuilder(
                Response.Status.NOT_ACCEPTABLE).entity(
                        json).build();
      default:
        json.setSuccessMessage("schema is valid");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(
                        json).build();
    }
  }

  @POST
  @Path("/schema/add")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response addTopicSchema(SchemaDTO schemaData,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    //String schemaContent = schemaData.getContents();
    switch (kafkaFacade.schemaBackwardCompatibility(schemaData)) {

      case INVALID:
        json.setErrorMsg("schema is invalid");
        return noCacheResponse.getNoCacheResponseBuilder(
                Response.Status.NOT_ACCEPTABLE).entity(
                        json).build();
      case INCOMPATIBLE:
        json.setErrorMsg("schema is not backward compatible");
        return noCacheResponse.getNoCacheResponseBuilder(
                Response.Status.NOT_ACCEPTABLE).entity(
                        json).build();
      default:
        kafkaFacade.addSchemaForTopics(schemaData);

        json.setSuccessMessage("Schema for Topic created/updated successfuly");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();
    }
  }

  //This API used is to select a schema and its version from the list
  //of available schemas when creating a topic. 
  @GET
  @Path("/schemas")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response listSchemasForTopics(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    List<SchemaDTO> schemaDtos = kafkaFacade.listSchemasForTopics();
    GenericEntity<List<SchemaDTO>> schemas
            = new GenericEntity<List<SchemaDTO>>(schemaDtos) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            schemas).build();
  }

  //This API used to select a schema and its version from the list
  //of available schemas when listing all the available schemas.
  @GET
  @Path("/showSchema/{schemaName}/{schemaVersion}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getSchemaContent(@PathParam("schemaName") String schemaName,
          @PathParam("schemaVersion") Integer schemaVersion,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    SchemaDTO schemaDtos = kafkaFacade.getSchemaContent(schemaName, schemaVersion);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            schemaDtos).build();
  }

  //delete the specified version of the given schema.
  @DELETE
  @Path("/removeSchema/{schemaName}/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteSchema(@PathParam("schemaName") String schemaName,
          @PathParam("version") Integer version,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    kafkaFacade.deleteSchema(schemaName, version);
    json.setSuccessMessage("Schema version for topic removed successfuly");

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

}
