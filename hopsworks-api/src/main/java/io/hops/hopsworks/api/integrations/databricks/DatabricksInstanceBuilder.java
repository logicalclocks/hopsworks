/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.databricks;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.featurestore.DatabricksInstanceFacade;
import io.hops.hopsworks.persistence.entity.integrations.DatabricksInstance;
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
public class DatabricksInstanceBuilder {

  @EJB
  private DatabricksInstanceFacade databricksInstanceFacade;

  private URI buildHref(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(project.getId().toString())
        .path(ResourceRequest.Name.INTEGRATIONS.toString().toLowerCase())
        .path(ResourceRequest.Name.DATABRICKS.toString().toLowerCase())
        .build();
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

  public DatabricksInstanceDTO build(UriInfo uriInfo, Project project, ResourceRequest resourceRequest,
                                     DatabricksInstance databricksInstance) {
    DatabricksInstanceDTO databricksInstanceDTO = new DatabricksInstanceDTO();
    databricksInstanceDTO.setHref(buildHref(uriInfo, project, databricksInstance.getUrl()));
    if (expand(resourceRequest)) {
      databricksInstanceDTO.setExpand(true);
      databricksInstanceDTO.setUrl(databricksInstance.getUrl());
    }
    return databricksInstanceDTO;
  }

  public DatabricksInstanceDTO build(UriInfo uriInfo, Project project, ResourceRequest resourceRequest, Users user) {
    List<DatabricksInstance> databricksInstances = databricksInstanceFacade.getInstances(user);

    DatabricksInstanceDTO databricksInstanceDTO = new DatabricksInstanceDTO();
    databricksInstanceDTO.setHref(buildHref(uriInfo, project));
    databricksInstanceDTO.setCount((long) databricksInstances.size());

    if (expand(resourceRequest)) {
      databricksInstanceDTO.setExpand(true);
      databricksInstanceDTO.setItems(
          databricksInstances.stream().map(c -> build(uriInfo, project, resourceRequest, c))
              .collect(Collectors.toList())
      );
    }
    return databricksInstanceDTO;
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.DATABRICKS);
  }
}
