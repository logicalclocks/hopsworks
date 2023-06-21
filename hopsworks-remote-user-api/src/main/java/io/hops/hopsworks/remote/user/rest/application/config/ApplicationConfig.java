/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.rest.application.config;

import io.hops.hopsworks.remote.user.api.AuthResource;
import io.hops.hopsworks.remote.user.api.exception.mapper.RESTApiThrowableMapper;
import io.hops.hopsworks.remote.user.api.krb.KrbConfigResource;
import io.hops.hopsworks.remote.user.api.ldap.LdapConfigResource;
import io.hops.hopsworks.remote.user.api.mapping.GroupToProjectMappingResource;
import io.hops.hopsworks.remote.user.api.oauth2.OAuthClientResource;
import io.hops.hopsworks.remote.user.jwt.AuthFilter;
import io.hops.hopsworks.remote.user.jwt.JWTAutoRenewFilter;
import io.swagger.annotations.Api;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("api/remote/user")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   */
  public ApplicationConfig() {
    register(AuthResource.class);
    register(OAuthClientResource.class);
    register(LdapConfigResource.class);
    register(KrbConfigResource.class);
    register(GroupToProjectMappingResource.class);
    register(RESTApiThrowableMapper.class);
    register(AuthFilter.class);
    register(JWTAutoRenewFilter.class);
  
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  
    //uncomment to allow Cross-Origin Resource Sharing
    //register(io.hops.hopsworks.filters.AllowCORSFilter.class);
  }
}
