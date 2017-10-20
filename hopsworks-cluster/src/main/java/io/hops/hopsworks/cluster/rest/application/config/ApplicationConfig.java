package io.hops.hopsworks.cluster.rest.application.config;

import io.swagger.annotations.Api;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   */
  public ApplicationConfig() {
    register(io.hops.hopsworks.cluster.Cluster.class);
    register(io.hops.hopsworks.cluster.exception.mapper.EJBExceptionMapper.class);
    register(io.hops.hopsworks.cluster.response.filter.CORSFilter.class);
    
    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
  
}
