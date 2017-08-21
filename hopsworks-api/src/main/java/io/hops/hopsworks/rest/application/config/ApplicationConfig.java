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
    register(io.hops.hopsworks.api.exception.mapper.ThrowableExceptionMapper.class);
    register(io.hops.hopsworks.api.exception.mapper.TransactionExceptionMapper.class);
    register(io.hops.hopsworks.api.filter.RequestAuthFilter.class);
    register(io.hops.hopsworks.api.jobs.AdamService.class);
    register(io.hops.hopsworks.api.jobs.BiobankingService.class);
    register(io.hops.hopsworks.api.jobs.ExecutionService.class);
    register(io.hops.hopsworks.api.jobs.FlinkService.class);
    register(io.hops.hopsworks.api.jobs.JobService.class);
    register(io.hops.hopsworks.api.jobs.KafkaService.class);
    register(io.hops.hopsworks.api.jobs.SparkService.class);
    register(io.hops.hopsworks.api.jobs.TensorFlowService.class);
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
    register(io.hops.hopsworks.api.workflow.EdgeService.class);
    register(io.hops.hopsworks.api.workflow.NodeService.class);
    register(io.hops.hopsworks.api.workflow.WorkflowExecutionService.class);
    register(io.hops.hopsworks.api.workflow.WorkflowJobService.class);
    register(io.hops.hopsworks.api.workflow.WorkflowService.class);
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
    
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    
    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
}
