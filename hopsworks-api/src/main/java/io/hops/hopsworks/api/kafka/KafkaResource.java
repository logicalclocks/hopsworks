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
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.kafka.topics.TopicsBeanParam;
import io.hops.hopsworks.api.kafka.topics.TopicsBuilder;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.kafka.KafkaClusterInfoDTO;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.schemas.Compatibility;
import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityCheck;
import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityLevel;
import io.hops.hopsworks.common.dao.kafka.schemas.SchemaRegistryError;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDTO;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.kafka.FeatureStoreKafkaConnectorDTO;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.kafka.SchemasController;
import io.hops.hopsworks.common.kafka.SubjectsCompatibilityController;
import io.hops.hopsworks.common.kafka.SubjectsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
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
import java.util.logging.Logger;

@Logged
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
  private SubjectsController subjectsController;
  @EJB
  private SubjectsCompatibilityController subjectsCompatibilityController;
  @EJB
  private SchemasController schemasController;
  @EJB
  private FeaturestoreStorageConnectorController storageConnectorController;
  @EJB
  private KafkaClusterInfoBuilder kafkaClusterInfoBuilder;

  private Project project;

  public KafkaResource() {
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setProjectId(Integer projectId) {
    this.project = this.projectFacade.find(projectId);
  }

  @Logged(logLevel = LogLevel.OFF)
  public Project getProject() {
    return project;
  }

  @ApiOperation(value = "Retrieve Kafka broker endpoints.")
  @GET
  @Path("/clusterinfo")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getBrokers(@Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws FeaturestoreException {
    FeatureStoreKafkaConnectorDTO connectorDTO = storageConnectorController.getKafkaConnector(project);
    KafkaClusterInfoDTO dto = kafkaClusterInfoBuilder.build(uriInfo, project,
        Arrays.asList(connectorDTO.getBootstrapServers().split(",")));
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Retrieve Kafka topics metadata.")
  @GET
  @Path("/topics")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTopics(@Context UriInfo uriInfo,
                            @BeanParam Pagination pagination,
                            @BeanParam TopicsBeanParam topicsBeanParam,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createTopic(TopicDTO topicDto, @Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws KafkaException {
    kafkaController.createTopic(project, topicDto);
    URI uri = uriInfo.getAbsolutePathBuilder().path(topicDto.getName()).build();
    topicDto.setHref(uri);
    return Response.created(uri).entity(topicDto).build();
  }

  @ApiOperation(value = "Delete a Kafka topic.")
  @DELETE
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response removeTopic(@PathParam("topic") String topicName,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws KafkaException {
    kafkaController.removeTopicFromProject(project, topicName);
    return Response.noContent().build();
  }

  @ApiOperation(value = "Get Kafka topic details.")
  @GET
  @Path("/topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTopic(@Context UriInfo uriInfo,
                           @PathParam("topic") String topicName,
                           @Context HttpServletRequest req,
                           @Context SecurityContext sc) throws KafkaException {
    PartitionDetailsDTO dto = topicsBuilder.buildTopicDetails(uriInfo, project, topicName);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get schema by its id.")
  @GET
  @Path("/schemas/ids/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "AGENT", "HOPS_SERVICE_USER"})
  public Response getSchema(@PathParam("id") Integer id,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSubjects(@Context SecurityContext sc,
                              @Context HttpServletRequest req) {
    List<String> subjects = subjectsController.getSubjects(project);
    String array = Arrays.toString(subjects.toArray());
    GenericEntity<String> entity = new GenericEntity<String>(array) {
    };
    return Response.ok().entity(entity).build();
  }

  @ApiOperation(value = "Check if the provided schema is registered under specified subject.")
  @POST
  @Path("/subjects/{subject}")
  @Consumes({MediaType.APPLICATION_JSON, "application/vnd.schemaregistry.v1+json"})
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response checkIfSubjectRegistered(@PathParam("subject") String subject, SubjectDTO dto,
                                           @Context HttpServletRequest req,
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteSubject(@PathParam("subject") String subject,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc) throws KafkaException {
    try {
      List<Integer> versions = subjectsController.deleteSubject(project, subject);
      String array = Arrays.toString(versions.toArray());
      GenericEntity<String> entity = new GenericEntity<String>(array) {
      };
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteSubjectsVersion(@PathParam("subject") String subject, @PathParam("version") String version,
                                        @Context HttpServletRequest req,
                                        @Context SecurityContext sc) throws KafkaException {
    try {
      Integer deleted = subjectsController.deleteSubjectsVersion(project, subject, version);
      GenericEntity<Integer> entity = new GenericEntity<Integer>(deleted) {
      };
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
  @Consumes({MediaType.APPLICATION_JSON, "application/vnd.schemaregistry.v1+json"})
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response postNewSchema(@PathParam("subject") String subject, SubjectDTO dto,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc) throws KafkaException {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSubjectVersions(@PathParam("subject") String subject,
                                     @Context HttpServletRequest req,
                                     @Context SecurityContext sc) {
    try {
      List<Integer> versions = subjectsController.getSubjectVersions(project, subject);
      String array = Arrays.toString(versions.toArray());
      GenericEntity<String> entity = new GenericEntity<String>(array) {
      };
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSubjectDetails(@PathParam("subject") String subject,
                                    @PathParam("version") String version,
                                    @Context HttpServletRequest req,
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSchema(@PathParam("subject") String subject,
                            @PathParam("version") String version,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) {
    try {
      SubjectDTO dto = subjectsController.getSubjectDetails(project, subject, version);
      GenericEntity<String> entity = new GenericEntity<String>(dto.getSchema()) {
      };
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
  @Consumes({MediaType.APPLICATION_JSON, "application/vnd.schemaregistry.v1+json"})
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response checkSchemaCompatibility(@PathParam("subject") String subject,
                                           @PathParam("version") String version, SubjectDTO dto,
                                           @Context HttpServletRequest req,
                                           @Context SecurityContext sc) {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getProjectCompatibility(@Context HttpServletRequest req, @Context SecurityContext sc) {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response setProjectCompatibility(Compatibility dto,
                                          @Context HttpServletRequest req,
                                          @Context SecurityContext sc) {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSubjectCompatibility(@PathParam("subject") String subject,
                                          @Context HttpServletRequest req,
                                          @Context SecurityContext sc) {
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
  @Consumes({MediaType.APPLICATION_JSON, "application/vnd.schemaregistry.v1+json"})
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response setSubjectCompatibility(@PathParam("subject") String subject, Compatibility dto,
                                          @Context HttpServletRequest req,
                                          @Context SecurityContext sc) {
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTopicSubject(@PathParam("topic") String topic,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc) throws KafkaException {
    SubjectDTO subjectDTO = kafkaController.getSubjectForTopic(project, topic);
    return Response.ok().entity(subjectDTO).build();
  }

  @ApiOperation(value = "Update subject version for a specified topic.")
  @PUT
  @Path("/topics/{topic}/subjects/{subject}/versions/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.KAFKA},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response updateSubjectVersion(@PathParam("topic") String topic, @PathParam("subject") String subject,
                                       @PathParam("version") Integer version,
                                       @Context HttpServletRequest req,
                                       @Context SecurityContext sc) throws KafkaException {
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
