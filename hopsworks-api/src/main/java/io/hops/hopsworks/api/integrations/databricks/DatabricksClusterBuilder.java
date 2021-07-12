/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.databricks;

import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.featurestore.databricks.DatabricksController;
import io.hops.hopsworks.featurestore.databricks.client.DbCluster;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatabricksClusterBuilder {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private UsersBuilder usersBuilder;

  public DatabricksClusterDTO build(UriInfo uriInfo, Project project, ResourceRequest resourceRequest,
                                    String dbInstance, DbCluster dbCluster) {
    DatabricksClusterDTO dbClusterDTO = new DatabricksClusterDTO();
    dbClusterDTO.setHref(buildHref(uriInfo, project, dbInstance, dbCluster));
    if (expand(resourceRequest)) {
      dbClusterDTO.setExpand(true);
      dbClusterDTO.setId(dbCluster.getId());
      dbClusterDTO.setName(dbCluster.getName());
      dbClusterDTO.setState(dbCluster.getState());
      dbClusterDTO.setSparkConfiguration(dbCluster.getSparkConfiguration());

      if (dbCluster.getTags() != null &&
          dbCluster.getTags().containsKey(DatabricksController.HOPSWORKS_PROJECT_TAG)) {
        projectFacade.findById(Integer.valueOf(dbCluster.getTags().get(DatabricksController.HOPSWORKS_PROJECT_TAG)))
            .ifPresent(p -> dbClusterDTO.setProject(p.getName()));
      }

      if (dbCluster.getTags() != null &&
          dbCluster.getTags().containsKey(DatabricksController.HOPSWORKS_USER_TAG)) {
        Users user = userFacade.findByUsername(dbCluster.getTags().get(DatabricksController.HOPSWORKS_USER_TAG));
        if (user != null) {
          dbClusterDTO.setUser(usersBuilder.build(uriInfo, resourceRequest, user));
        }
      }
    }
    return dbClusterDTO;
  }

  public DatabricksClusterDTO build(UriInfo uriInfo, Project project, ResourceRequest resourceRequest,
                                    String dbInstance, List<DbCluster> dbClusters) {
    DatabricksClusterDTO dbClusterDTO = new DatabricksClusterDTO();
    dbClusterDTO.setHref(buildHref(uriInfo, project, dbInstance));
    dbClusterDTO.setCount((long) (dbClusters == null ? 0 : dbClusters.size()));
    if(dbClusters != null && expand(resourceRequest)) {
      dbClusterDTO.setExpand(true);
      dbClusterDTO.setItems(
              dbClusters.stream().map(c -> build(uriInfo, project, resourceRequest, dbInstance, c))
                      .collect(Collectors.toList())
      );
    }
    return dbClusterDTO;
  }

  private URI buildHref(UriInfo uriInfo, Project project, String dbInstance) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(project.getId().toString())
        .path(ResourceRequest.Name.INTEGRATIONS.toString().toLowerCase())
        .path(ResourceRequest.Name.DATABRICKS.toString().toLowerCase())
        .path(dbInstance)
        .build();
  }

  private URI buildHref(UriInfo uriInfo, Project project, String dbInstance, DbCluster dbCluster) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(project.getId().toString())
        .path(ResourceRequest.Name.INTEGRATIONS.toString().toLowerCase())
        .path(ResourceRequest.Name.DATABRICKS.toString().toLowerCase())
        .path(dbInstance)
        .path("cluster")
        .path(dbCluster.getId())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.DATABRICKS);
  }
}
