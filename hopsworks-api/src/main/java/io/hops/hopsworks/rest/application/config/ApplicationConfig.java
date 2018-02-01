/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.rest.application.config;

import io.swagger.annotations.Api;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   */
  public ApplicationConfig() {
    register(io.hops.hopsworks.api.agent.AgentResource.class);
    register(io.hops.hopsworks.api.elastic.ElasticService.class);
    register(io.hops.hopsworks.api.exception.mapper.AccessControlExceptionMapper.class);
    register(io.hops.hopsworks.api.exception.mapper.AppExceptionMapper.class);
    register(io.hops.hopsworks.api.exception.mapper.AuthExceptionMapper.class);
    register(io.hops.hopsworks.api.exception.mapper.LoginExceptionMapper.class);
    register(io.hops.hopsworks.api.exception.mapper.ThrowableExceptionMapper.class);
    register(io.hops.hopsworks.api.exception.mapper.TransactionExceptionMapper.class);
    register(io.hops.hopsworks.api.filter.RequestAuthFilter.class);
    register(io.hops.hopsworks.api.jobs.AdamService.class);
    register(io.hops.hopsworks.api.jobs.ExecutionService.class);
    register(io.hops.hopsworks.api.jobs.FlinkService.class);
    register(io.hops.hopsworks.api.jobs.JobService.class);
    register(io.hops.hopsworks.api.jupyter.JupyterService.class);
    register(io.hops.hopsworks.api.tensorflow.TfServingService.class);
    register(io.hops.hopsworks.api.jobs.KafkaService.class);
    register(io.hops.hopsworks.api.jobs.SparkService.class);
    register(io.hops.hopsworks.api.project.DataSetService.class);
    register(io.hops.hopsworks.api.project.HistoryService.class);
    register(io.hops.hopsworks.api.project.MessageService.class);
    register(io.hops.hopsworks.api.project.MetadataService.class);
    register(io.hops.hopsworks.api.project.ProjectMembersService.class);
    register(io.hops.hopsworks.api.project.ProjectService.class);
    register(io.hops.hopsworks.api.project.RequestService.class);
    register(io.hops.hopsworks.api.project.CertService.class);
    register(io.hops.hopsworks.api.pythonDeps.PythonDepsService.class);
    register(io.hops.hopsworks.api.user.ActivityService.class);
    register(io.hops.hopsworks.api.user.AuthService.class);
    register(io.hops.hopsworks.api.user.UserService.class);
    register(io.hops.hopsworks.api.util.BannerService.class);
    register(io.hops.hopsworks.api.util.ClusterUtilisationService.class);
    register(io.hops.hopsworks.api.util.DownloadService.class);
    register(io.hops.hopsworks.api.util.EndpointService.class);
    register(io.hops.hopsworks.api.util.LocalFsService.class);
    register(io.hops.hopsworks.api.util.UploadService.class);
    register(io.hops.hopsworks.api.util.VariablesService.class);
    register(io.hops.hopsworks.api.zeppelin.rest.ConfigurationsRestApi.class);
    register(io.hops.hopsworks.api.zeppelin.rest.HeliumService.class);
    register(io.hops.hopsworks.api.zeppelin.rest.HeliumRestApi.class);
    register(io.hops.hopsworks.api.zeppelin.rest.InterpreterRestApi.class);
    register(io.hops.hopsworks.api.zeppelin.rest.InterpreterService.class);
    register(io.hops.hopsworks.api.zeppelin.rest.LoginRestApi.class);
    register(io.hops.hopsworks.api.zeppelin.rest.NotebookRestApi.class);
    register(io.hops.hopsworks.api.zeppelin.rest.NotebookService.class);
    register(io.hops.hopsworks.api.zeppelin.rest.SecurityRestApi.class);
    register(io.hops.hopsworks.api.zeppelin.rest.ZeppelinRestApi.class);
    register(io.hops.hopsworks.api.app.ApplicationService.class);
    register(io.hops.hopsworks.api.cluster.Monitor.class);

    // admin
    register(io.hops.hopsworks.api.admin.UsersAdmin.class);
    register(io.hops.hopsworks.api.admin.SystemAdminService.class);
    register(io.hops.hopsworks.api.admin.ProjectsAdmin.class);
    register(io.hops.hopsworks.api.admin.llap.LlapAdmin.class);
    register(io.hops.hopsworks.api.admin.CertificateMaterializerAdmin.class);

    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    
    //dela
    register(io.hops.hopsworks.api.dela.DelaClusterService.class);
    register(io.hops.hopsworks.api.dela.DelaClusterProjectService.class);
    register(io.hops.hopsworks.api.dela.DelaService.class);
    register(io.hops.hopsworks.api.dela.DelaProjectService.class);
    register(io.hops.hopsworks.api.dela.RemoteDelaService.class);
    register(io.hops.hopsworks.api.hopssite.HopssiteService.class);
    register(io.hops.hopsworks.api.hopssite.CommentService.class);
    register(io.hops.hopsworks.api.hopssite.RatingService.class);

    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
}
