package se.kth.rest.application.config;

import org.glassfish.jersey.server.ResourceConfig;

@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   */
  public ApplicationConfig() {
    register(se.kth.hopsworks.filters.RequestAuthFilter.class);
    register(se.kth.hopsworks.rest.ActivityService.class);
    register(se.kth.hopsworks.rest.AdamService.class);
    register(se.kth.hopsworks.rest.AppExceptionMapper.class);
    register(se.kth.hopsworks.rest.AuthExceptionMapper.class);
    register(se.kth.hopsworks.rest.AuthService.class);
    register(se.kth.hopsworks.rest.DataSetService.class);
    register(se.kth.hopsworks.rest.ExecutionService.class);
    register(se.kth.hopsworks.rest.JobService.class);
    register(se.kth.hopsworks.rest.ProjectMembers.class);
    register(se.kth.hopsworks.rest.ProjectService.class);
    register(se.kth.hopsworks.rest.RequestService.class);
    register(se.kth.hopsworks.rest.SparkService.class);
    register(se.kth.hopsworks.rest.FlinkService.class);
    register(se.kth.hopsworks.rest.ThrowableExceptionMapper.class);
    register(se.kth.hopsworks.rest.TransactionExceptionMapper.class);
    register(se.kth.hopsworks.rest.AccessControlExceptionMapper.class);
    register(se.kth.hopsworks.rest.DownloadService.class);
    register(se.kth.hopsworks.rest.UploadService.class);
    register(se.kth.hopsworks.rest.UserService.class);
    register(se.kth.hopsworks.zeppelin.rest.InterpreterService.class);
    register(se.kth.hopsworks.zeppelin.rest.NotebookService.class);
    register(se.kth.hopsworks.zeppelin.rest.InterpreterRestApi.class);
    register(se.kth.hopsworks.zeppelin.rest.NotebookRestApi.class);
    register(se.kth.hopsworks.zeppelin.rest.ZeppelinRestApi.class);
    register(se.kth.hopsworks.zeppelin.rest.LoginRestApi.class);
    register(se.kth.hopsworks.zeppelin.rest.ConfigurationsRestApi.class);
    register(se.kth.hopsworks.zeppelin.rest.SecurityRestApi.class);
    register(se.kth.hopsworks.rest.MetadataService.class);
    register(se.kth.hopsworks.drelephant.rest.HistoryService.class);
    register(se.kth.hopsworks.rest.MessageService.class);
    register(se.kth.hopsworks.rest.ElasticService.class);
    register(se.kth.hopsworks.rest.VariablesService.class);
    register(se.kth.hopsworks.rest.BannerService.class);
    register(io.hops.hdfs.EndpointService.class);
    register(se.kth.hopsworks.rest.LocalFsService.class);
    register(se.kth.hopsworks.rest.KafkaService.class);
    register(se.kth.hopsworks.rest.TensorflowService.class);
    // register resources and features
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    //register(org.glassfish.jersey.filter.LoggingFilter.class);

    register(se.kth.hopsworks.rest.WorkflowService.class);
    register(se.kth.hopsworks.rest.NodeService.class);
    register(se.kth.hopsworks.rest.EdgeService.class);
    register(se.kth.hopsworks.rest.WorkflowExecutionService.class);

    // KMON REST Apis
    register(se.kth.hopsworks.rest.AgentResource.class);
    register(se.kth.hopsworks.rest.AgentService.class);

    // Enable Tracing support.
    //property(ServerProperties.TRACING, "OFF");
  }
}
