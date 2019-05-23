/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.auth.rest.application.config;

import io.swagger.annotations.Api;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("api/remote/user")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   */
  public ApplicationConfig() {
    register(io.hops.hopsworks.remote.user.auth.api.AuthResource.class);
    register(io.hops.hopsworks.remote.user.auth.api.oauth2.OAuthClientResource.class);
    register(io.hops.hopsworks.remote.user.auth.api.exception.mapper.RESTApiThrowableMapper.class);
  
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
}
