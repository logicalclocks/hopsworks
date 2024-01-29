/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.python.environment.DockerImageController;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngr;
import io.hops.hopsworks.common.python.environment.DockerRegistryMngrImpl;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.hops.hopsworks.common.dataset.FolderNameValidator;
import io.hops.hopsworks.exceptions.ServiceException;

@Stateless
@ManagedStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ManagedDockerRegistryMngr extends DockerRegistryMngrImpl implements
        DockerRegistryMngr {

  private static final Logger LOG
          = Logger.getLogger(ManagedDockerRegistryMngr.class.getName());

  @EJB
  Settings settings;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private ECRClientService ecrClient;
  @EJB
  private ACRClientService acrClient;
  @EJB
  private GCRClientService gcrClient;
  @EJB
  private DockerImageController dockerImageController;

  @Override
  public List<String> deleteProjectImagesOnRegistry(String projectDockerImage)
    throws ServiceDiscoveryException, IOException, ServiceException {
    if (settings.isManagedDockerRegistryOnManagedCloud()) {
      final String repoName
              = settings.getDockerNamespace() + "/"
              + settings.getBaseNonPythonDockerImageWithNoTag();

      final String projectNameTagPrefix
              = projectUtils.getProjectNameFromDockerImageName(projectDockerImage) + "__";

      return this.deleteImagesWithTagPrefix(repoName, projectNameTagPrefix);
    } else {
      return super.deleteProjectImagesOnRegistry(projectDockerImage);
    }
  }

  private List<String> deleteImagesWithTagPrefix(String repoName, String projectNameTagPrefix) throws
          ServiceDiscoveryException {
    if (settings.getCloudType() == Settings.CLOUD_TYPES.AZURE) {
      return acrClient.deleteImagesWithTagPrefix(repoName, projectNameTagPrefix);
    } else if (settings.getCloudType() == Settings.CLOUD_TYPES.AWS) {
      return ecrClient.deleteImagesWithTagPrefix(repoName, projectNameTagPrefix);
    } else if (settings.getCloudType() == Settings.CLOUD_TYPES.GCP) {
      return gcrClient.deleteImagesWithTagPrefix(repoName, projectNameTagPrefix);
    } else {
      throw new UnsupportedOperationException("Unsupported operation on " + settings.getCloudType());
    }
  }

  @Override
  public void runRegistryGC() throws ServiceException {
    if (settings.isManagedDockerRegistryOnManagedCloud()) {
      // do nothing, we use repository lifecycle policies for gc
    } else {
      super.runRegistryGC();
    }
  }

  private List<String> getImageTags(String repoName, String filter) throws ServiceDiscoveryException, ServiceException {
    if (settings.getCloudType() == Settings.CLOUD_TYPES.AZURE) {
      return acrClient.getImageTags(repoName, filter);
    } else if (settings.getCloudType() == Settings.CLOUD_TYPES.AWS) {
      return ecrClient.getImageTags(repoName, filter);
    } else if (settings.getCloudType() == Settings.CLOUD_TYPES.GCP) {
      return gcrClient.getImageTags(repoName, filter);
    } else {
      throw new UnsupportedOperationException("Unsupported operation on " + settings.getCloudType());
    }
  }

  @Override
  public Map<String, Future<ProcessResult>> backupImages(String backupId)
    throws IOException, ServiceDiscoveryException, ServiceException {
    LOG.info("Backing up images");

    if (settings.isManagedDockerRegistryOnManagedCloud()) {

      Map<String, Future<ProcessResult>> result = new HashMap<>();
      final String repoName
              = settings.getDockerNamespace() + "/"
              + settings.getBaseNonPythonDockerImageWithNoTag();
      List<String> tags = getImageTags(repoName, "__");

      String projectNameRegex = FolderNameValidator.getProjectNameRegexStr(settings.getReservedProjectNames());

      Pattern projectTagPattern = Pattern.compile(
              projectNameRegex.substring(0, projectNameRegex.length() - 1) + "(__).+$");

      for (String tag : tags) {
        Matcher projectTagMatcher = projectTagPattern.matcher(tag);
        if (projectTagMatcher.matches()) {
          //if the tag match a project tag tag the image with a backup tag so that it does not get removed at the
          //same time as the project environment
          String targetTag = "__backup_" + backupId + "_" + tag;
          String baseImage = settings.getBaseNonPythonDockerImageWithNoTag() + ":" + tag;
          String targetImage = settings.getBaseNonPythonDockerImageWithNoTag() + ":" + targetTag;
          result.put(targetImage, dockerImageController.tag(baseImage, targetImage));
        }
      }

      return result;
    } else {
      return super.backupImages(backupId);
    }
  }

  @Override
  public Map<String, Future<ProcessResult>> restoreImages(String backupId)
    throws IOException, ServiceDiscoveryException, ServiceException {
    if (settings.isManagedDockerRegistryOnManagedCloud()) {
      Map<String, Future<ProcessResult>> result = new HashMap<>();
      final String repoName
              = settings.getDockerNamespace() + "/"
              + settings.getBaseNonPythonDockerImageWithNoTag();
      List<String> tags = getImageTags(repoName, "__backup_" + backupId + "_");

      for (String tag : tags) {
        //if the tag match a backup tag for this backup retag the image to the expected project image
        String targetTag = tag.replaceFirst("__backup_" + backupId + "_", "");
        String baseImage = settings.getBaseNonPythonDockerImageWithNoTag() + ":" + tag;
        String targetImage = settings.getBaseNonPythonDockerImageWithNoTag() + ":" + targetTag;
        result.put(targetImage, dockerImageController.tag(baseImage, targetImage));
      }

      return result;
    } else {
      return super.restoreImages(backupId);
    }
  }

  @Override
  public List<String> deleteBackup(String backupId)
          throws ServiceDiscoveryException {
    LOG.info("Deleting backing up " + backupId);
    final String repoName
            = settings.getDockerNamespace() + "/"
            + settings.getBaseNonPythonDockerImageWithNoTag();

    return this.deleteImagesWithTagPrefix(repoName, "__backup_" + backupId + "_");

  }
}
