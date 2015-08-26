package se.kth.rest.application.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 *
 * @author Ermias
 */
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   * Prevents glassfish from autoloading other irrelevant services
   */
  public ApplicationConfig() {
    register(se.kth.hopsworks.filters.RequestAuthFilter.class);
    register(se.kth.hopsworks.rest.ActivityService.class);
    register(se.kth.hopsworks.rest.AdamService.class);
    register(se.kth.hopsworks.rest.AppExceptionMapper.class);
    register(se.kth.hopsworks.rest.AuthExceptionMapper.class);
    register(se.kth.hopsworks.rest.AuthService.class);
    register(se.kth.hopsworks.rest.CuneiformService.class);
    register(se.kth.hopsworks.rest.DataSetService.class);
    register(se.kth.hopsworks.rest.ExecutionService.class);
    register(se.kth.hopsworks.rest.JobService.class);
    register(se.kth.hopsworks.rest.ProjectMembers.class);
    register(se.kth.hopsworks.rest.ProjectService.class);
    register(se.kth.hopsworks.rest.RequestService.class);
    register(se.kth.hopsworks.rest.SparkService.class);
    register(se.kth.hopsworks.rest.ThrowableExceptionMapper.class);
    register(se.kth.hopsworks.rest.TransactionExceptionMapper.class);
    register(se.kth.hopsworks.rest.UploadService.class);
    register(se.kth.hopsworks.rest.UserService.class);
    register(se.kth.hopsworks.rest.TemplateService.class);

    // register resources and features
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    //register(org.glassfish.jersey.filter.LoggingFilter.class);

    // Enable Tracing support.
    property(ServerProperties.TRACING, "ALL");
  }
}
