/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2;

import io.hops.hopsworks.apiV2.mapper.AccessControlExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.AppExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.AuthExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.ThrowableExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.TransactionExceptionMapper;
import io.hops.hopsworks.apiV2.projects.BlobsResource;
import io.hops.hopsworks.apiV2.projects.DatasetsResource;
import io.hops.hopsworks.apiV2.projects.MembersResource;
import io.hops.hopsworks.apiV2.projects.PathValidator;
import io.hops.hopsworks.apiV2.projects.ProjectsResource;
import io.hops.hopsworks.apiV2.users.UsersResource;
import io.swagger.annotations.Api;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("/")
public class ApplicationConfig extends ResourceConfig {
  public ApplicationConfig(){
  
    register(AccessControlExceptionMapper.class);
    register(AppExceptionMapper.class);
    register(AuthExceptionMapper.class);
    register(ThrowableExceptionMapper.class);
    register(TransactionExceptionMapper.class);
    
    //API V2
    //Projects & Datasets
    //register(ProjectAuthFilter.class);
    register(ProjectsResource.class);
    register(DatasetsResource.class);
    register(MembersResource.class);
    register(BlobsResource.class);
    register(PathValidator.class);
  
    //Hopsworks-Users
    register(UsersResource.class);
  
    //swagger
    register(ApiListingResource.class);
    register(SwaggerSerializers.class);
  }
}
