/*
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.python.environment.DockerImageController;
import io.hops.hopsworks.exceptions.ServiceException;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GCRClientService {

  private static final Logger LOG =
      Logger.getLogger(GCRClientService.class.getName());

  @EJB
  private DockerImageController dockerImageController;


  public List<String> deleteImagesWithTagPrefix(final String repositoryName,
                                                final String imageTagPrefix) throws ServiceDiscoveryException {
    try {
      return dockerImageController.deleteGCR(repositoryName, imageTagPrefix);
    } catch (ServiceException ex) {
      String errorMsg = "Could not delete docker images in " + repositoryName + " under prefix " + imageTagPrefix + "."
          + "Exception caught: " + ex.getMessage();
      if(ex.getCause() != null) {
        errorMsg += ex.getStackTrace().toString();
      }
      LOG.severe(errorMsg);
      return new ArrayList<>();
    }
  }

  public List<String> getImageTags(final String repositoryName, String filter)
      throws ServiceDiscoveryException, ServiceException {
    return dockerImageController.listTagsGCR(repositoryName, filter);
  }
}
