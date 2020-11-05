/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.spark;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.featurestore.databricks.DatabricksController;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SparkConfigurationBuilder {

  @EJB
  private DatabricksController databricksController;

  private URI buildHref(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(project.getId().toString())
        .path(ResourceRequest.Name.INTEGRATIONS.toString().toLowerCase())
        .path(ResourceRequest.Name.SPARK.toString().toLowerCase())
        .path(ResourceRequest.Name.CONFIGURATION.toString().toLowerCase())
        .build();
  }

  public SparkConfigurationDTO build(UriInfo uriInfo, Project project) throws ServiceException {
    SparkConfigurationDTO sparkConfigurationDTO = new SparkConfigurationDTO();
    sparkConfigurationDTO.setHref(buildHref(uriInfo, project));

    try {
      Map<String, String> sparkProperties =
          databricksController.getSparkProperties("keyStore.jks", "trustStore.jks",
              "material_passwd", "hopsworks_jars/apache-hive/lib*");
      sparkConfigurationDTO.setCount((long) sparkProperties.size());
      sparkConfigurationDTO.setItems(sparkProperties.entrySet().stream()
          .map(p -> new SparkConfigurationDTO(p.getKey(), p.getValue()))
          .collect(Collectors.toList()));
    } catch (ServiceDiscoveryException se) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.SEVERE,
          "Cannot find running Hive Metastore", se.getMessage(), se);
    }

    return sparkConfigurationDTO;
  }
}
