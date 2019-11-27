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
    register(io.hops.hopsworks.api.exception.mapper.RESTApiThrowableMapper.class);
    register(io.hops.hopsworks.api.filter.ProjectAuthFilter.class);
    register(io.hops.hopsworks.api.filter.AuthFilter.class);
    register(io.hops.hopsworks.api.filter.apiKey.ApiKeyFilter.class);
    register(io.hops.hopsworks.api.filter.JWTAutoRenewFilter.class);
    register(io.hops.hopsworks.api.jwt.JWTResource.class);
    register(io.hops.hopsworks.api.jobs.executions.ExecutionsResource.class);
    register(io.hops.hopsworks.api.jobs.JobsResource.class);
    register(io.hops.hopsworks.api.jupyter.JupyterService.class);
    register(io.hops.hopsworks.api.serving.ServingService.class);
    register(io.hops.hopsworks.api.serving.inference.InferenceResource.class);
    register(io.hops.hopsworks.api.kafka.KafkaService.class);
    register(io.hops.hopsworks.api.project.MessageService.class);
    register(io.hops.hopsworks.api.project.MetadataService.class);
    register(io.hops.hopsworks.api.project.ProjectMembersService.class);
    register(io.hops.hopsworks.api.project.ProjectService.class);
    register(io.hops.hopsworks.api.project.RequestService.class);
    register(io.hops.hopsworks.api.activities.ProjectActivitiesResource.class);
    register(io.hops.hopsworks.api.python.environment.command.EnvironmentCommandsResource.class);
    register(io.hops.hopsworks.api.python.environment.EnvironmentResource.class);
    register(io.hops.hopsworks.api.python.library.command.LibraryCommandsResource.class);
    register(io.hops.hopsworks.api.python.library.LibraryResource.class);
    register(io.hops.hopsworks.api.python.PythonResource.class);
    register(io.hops.hopsworks.api.user.AuthService.class);
    register(io.hops.hopsworks.api.airflow.AirflowService.class);
    register(io.hops.hopsworks.api.user.UsersResource.class);
    register(io.hops.hopsworks.api.user.apiKey.ApiKeyResource.class);
    
    register(io.hops.hopsworks.api.util.BannerService.class);
    register(io.hops.hopsworks.api.util.ClusterUtilisationService.class);
    register(io.hops.hopsworks.api.util.DownloadService.class);
    register(io.hops.hopsworks.api.util.EndpointService.class);
    register(io.hops.hopsworks.api.util.LocalFsService.class);
    register(io.hops.hopsworks.api.util.UploadService.class);
    register(io.hops.hopsworks.api.util.VariablesService.class);
    register(io.hops.hopsworks.api.cluster.Monitor.class);
    register(io.hops.hopsworks.api.serving.ServingConfResource.class);
    register(io.hops.hopsworks.api.featurestore.FeaturestoreService.class);
    register(io.hops.hopsworks.api.host.machine.MachineTypeResource.class);

    // admin
    register(io.hops.hopsworks.api.admin.UsersAdmin.class);
    register(io.hops.hopsworks.api.admin.SystemAdminService.class);
    register(io.hops.hopsworks.api.admin.ProjectsAdmin.class);
    register(io.hops.hopsworks.api.admin.llap.LlapAdmin.class);
    register(io.hops.hopsworks.api.admin.security.CertificateMaterializerAdmin.class);
    register(io.hops.hopsworks.api.admin.security.CredentialsResource.class);
    register(io.hops.hopsworks.api.admin.security.X509Resource.class);

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

    //maggy
    register(io.hops.hopsworks.api.maggy.MaggyService.class);

    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
}
