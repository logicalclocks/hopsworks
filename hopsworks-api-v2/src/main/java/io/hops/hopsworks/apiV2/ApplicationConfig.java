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
