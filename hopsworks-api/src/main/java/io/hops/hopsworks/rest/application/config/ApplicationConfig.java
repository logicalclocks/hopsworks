package io.hops.hopsworks.rest.application.config;

import java.util.Set;
import javax.ws.rs.core.Application;

@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> resources = new java.util.HashSet<>();
    addRestResourceClasses(resources);
    return resources;
  }

  /**
   * Do not modify addRestResourceClasses() method.
   * It is automatically populated with
   * all resources defined in the project.
   * If required, comment out calling this method in getClasses().
   */
  private void addRestResourceClasses(
          Set<Class<?>> resources) {
    resources.add(io.hops.hopsworks.api.agent.AgentResource.class);
    resources.add(io.hops.hopsworks.api.agent.AgentService.class);
    resources.add(io.hops.hopsworks.api.elastic.ElasticService.class);
    resources.add(io.hops.hopsworks.api.exception.mapper.AccessControlExceptionMapper.class);
    resources.add(io.hops.hopsworks.api.exception.mapper.AppExceptionMapper.class);
    resources.add(io.hops.hopsworks.api.exception.mapper.AuthExceptionMapper.class);
    resources.add(io.hops.hopsworks.api.exception.mapper.ThrowableExceptionMapper.class);
    resources.add(io.hops.hopsworks.api.exception.mapper.TransactionExceptionMapper.class);
    resources.add(io.hops.hopsworks.api.filter.RequestAuthFilter.class);
    resources.add(io.hops.hopsworks.api.project.HistoryService.class);
    resources.add(io.hops.hopsworks.api.project.MessageService.class);
    resources.add(io.hops.hopsworks.api.project.MetadataService.class);
    resources.add(io.hops.hopsworks.api.project.ProjectService.class);
    resources.add(io.hops.hopsworks.api.project.RequestService.class);
    resources.add(io.hops.hopsworks.api.user.ActivityService.class);
    resources.add(io.hops.hopsworks.api.user.AuthService.class);
    resources.add(io.hops.hopsworks.api.user.UserService.class);
    resources.add(io.hops.hopsworks.api.util.BannerService.class);
    resources.add(io.hops.hopsworks.api.util.ClusterUtilisationService.class);
    resources.add(io.hops.hopsworks.api.util.EndpointService.class);
    resources.add(io.hops.hopsworks.api.util.VariablesService.class);
    resources.add(io.hops.hopsworks.api.zeppelin.rest.ConfigurationsRestApi.class);
    resources.add(io.hops.hopsworks.api.zeppelin.rest.InterpreterService.class);
    resources.add(io.hops.hopsworks.api.zeppelin.rest.LoginRestApi.class);
    resources.add(io.hops.hopsworks.api.zeppelin.rest.NotebookService.class);
    resources.add(io.hops.hopsworks.api.zeppelin.rest.SecurityRestApi.class);
    resources.add(io.hops.hopsworks.api.zeppelin.rest.ZeppelinRestApi.class);
  }
}
