/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.cloud;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngrImpl;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@ManagedStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ManagedDockerRegistryMngr extends DockerRegistryMngrImpl implements
    DockerRegistryMngr {
  private static final Logger LOG =
      Logger.getLogger(ManagedDockerRegistryMngr.class.getName());
  
  @EJB
  Settings settings;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private ECRClientService ecrClient;
  @EJB
  private ACRClientService acrClient;
  
  @Override
  public List<String> deleteProjectImagesOnRegistry(String projectDockerImage)
      throws ServiceDiscoveryException, IOException {
    if (settings.isManagedDockerRegistry()) {
      final String repoName =
          settings.getDockerNamespace() + "/" +
              settings.getBaseNonPythonDockerImageWithNoTag();
      
      final String projectNameTagPrefix =
          projectUtils.getProjectNameFromDockerImageName(projectDockerImage) + "__";

      if (settings.getKubeType() == Settings.KubeType.AKS) {
        return acrClient.deleteImagesWithTagPrefix(repoName, projectNameTagPrefix);
      } else {
        return ecrClient.deleteImagesWithTagPrefix(repoName, projectNameTagPrefix);
      }
    } else {
      return super.deleteProjectImagesOnRegistry(projectDockerImage);
    }
  }
  
  @Override
  public void runRegistryGC() throws IOException {
    if (settings.isManagedDockerRegistry()) {
      // do nothing, we use repository lifecycle policies for gc
    } else {
      super.runRegistryGC();
    }
  }
}
