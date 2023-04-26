/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.utils;

import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.NamenodeTags;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

@Stateless
public class FeaturestoreUtils {

  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  public enum ActionMessage {
    // Feature Group
    CREATE_FEATURE_GROUP("create feature group"),
    CLEAR_FEATURE_GROUP("clear feature group"),
    UPDATE_FEATURE_GROUP_METADATA("update feature group metadata"),
    ENABLE_FEATURE_GROUP_ONLINE("enable online feature group"),
    DISABLE_FEATURE_GROUP_ONLINE("disable online feature group"),
    UPDATE_FEATURE_GROUP_STATS_CONFIG("update feature group stats config"),
    DELETE_FEATURE_GROUP("delete feature group"),
    // Feature View
    CREATE_FEATURE_VIEW("create feature view"),
    DELETE_FEATURE_VIEW("delete feature view"),
    UPDATE_FEATURE_VIEW("update feature view"),
    // Training Dataset
    CREATE_TRAINING_DATASET("create training dataset"),
    DELETE_TRAINING_DATASET("delete training dataset"),
    DELETE_TRAINING_DATASET_DATA_ONLY("delete training dataset data only"),
    UPDATE_TRAINING_DATASET_METADATA("update training dataset metadata"),
    UPDATE_TRAINING_DATASET_STATS_CONFIG("update training dataset stats config"),
    // Job
    SETUP_TRAINING_DATASET_JOB("setup training dataset job"),
    // Storage Connector
    CREATE_STORAGE_CONNECTOR("create storage connector"),
    UPDATE_STORAGE_CONNECTOR("update storage connector"),
    DELETE_STORAGE_CONNECTOR("delete storage connector");

    private String message;

    ActionMessage(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  private void throwForbiddenOperation(Featurestore featureStore, String userRole, Project project, Object object,
                                       ActionMessage actionMessage) throws FeaturestoreException {
    List<String> messageContent = new ArrayList<>();
    if (project != null) {
      messageContent.add(String.format("project: %s", project.getName()));
    }
    if (featureStore != null) {
      messageContent.add(String.format("featurestoreId: %s", featureStore.getId()));
    }
    if (userRole != null) {
      messageContent.add(String.format("userRole: %s", userRole));
    }
    if (object != null) {
      if (object instanceof Featuregroup) {
        Featuregroup featuregroup = ((Featuregroup) object);
        messageContent.add(String.format("feature group: %s, version: %s",
            featuregroup.getName(), featuregroup.getVersion()));
      } else if (object instanceof TrainingDataset) {
        TrainingDataset trainingDataset = ((TrainingDataset) object);
        messageContent.add(String.format("training dataset: %s, version: %s",
            trainingDataset.getName(), trainingDataset.getName()));
      } else if (object instanceof FeatureView) {
        FeatureView featureView = ((FeatureView) object);
        messageContent.add(String.format("feature view: %s, version: %s",
            featureView.getName(), featureView.getVersion()));
      } else if (object instanceof FeaturestoreConnector) {
        FeaturestoreConnector featurestoreConnector = ((FeaturestoreConnector) object);
        messageContent.add(String.format("storage connector: %s", featurestoreConnector.getName()));
      } else if (object instanceof Jobs) {
        Jobs job = ((Jobs) object);
        messageContent.add(String.format("jobs: %s", job.getName()));
      }
    }
    if (actionMessage != null) {
      messageContent.add(String.format("actionMessage: Forbidden to %s", actionMessage.getMessage()));
    }
    throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FORBIDDEN_FEATURESTORE_OPERATION, Level.FINE,
        String.join(", ", messageContent));
  }

  /**
   * Verify that the action is allowed to be executed
   *
   * @param user the user requesting the operation
   * @param project the project of the featurestore
   * @param featureStore the featurestore that the operation concerns
   * @param actionMessage the message describing action taking place
   * @throws FeaturestoreException
   */
  public void verifyUserProjectEqualsFsProject(Users user, Project project, Featurestore featureStore,
                                               ActionMessage actionMessage)
      throws FeaturestoreException {
    if (!featureStore.getProject().equals(project)) {
      String userRole = projectTeamFacade.findCurrentRole(project, user);
      throwForbiddenOperation(featureStore, userRole, project, null, actionMessage);
    }
  }

  /**
   * Verify that the action is allowed to be executed based on his/hers project role being DATA_OWNER
   *
   * @param user the user requesting the operation
   * @param project the project of the featurestore
   * @param featureStore the featurestore that the operation concerns
   * @param actionMessage the message describing action taking place
   * @throws FeaturestoreException
   */
  public void verifyUserProjectEqualsFsProjectAndDataOwner(Users user, Project project, Featurestore featureStore,
                                                           ActionMessage actionMessage)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!featureStore.getProject().equals(project) || !AllowedRoles.DATA_OWNER.equalsIgnoreCase(userRole)) {
      throwForbiddenOperation(featureStore, userRole, project, null, actionMessage);
    }
  }

  /**
   * Verify that the action is allowed to be executed on the training dataset
   * <p>
   * Only data owners are allowed to update/delete training datasets created by someone else in the project
   *
   * @param user the user requesting the operation
   * @param project the project of the featurestore
   * @param trainingDataset the training dataset the operation concerns
   * @param actionMessage the message describing action taking place
   * @throws FeaturestoreException
   */
  public void verifyTrainingDatasetDataOwnerOrSelf(Users user, Project project, TrainingDataset trainingDataset,
                                                   ActionMessage actionMessage)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!trainingDataset.getFeaturestore().getProject().equals(project) ||
        (!AllowedRoles.DATA_OWNER.equalsIgnoreCase(userRole) && !trainingDataset.getCreator().equals(user))) {
      throwForbiddenOperation(trainingDataset.getFeaturestore(), userRole, project, trainingDataset, actionMessage);
    }
  }

  /**
   * Verify that the action is allowed to be executed on the feature view
   * <p>
   * Only data owners are allowed to update/delete feature view created by someone else in the project
   *
   * @param user the user requesting the operation
   * @param project the project of the featurestore
   * @param featureView the feature view the operation concerns
   * @param actionMessage the message describing action taking place
   * @throws FeaturestoreException
   */
  public void verifyFeatureViewDataOwnerOrSelf(Users user, Project project, FeatureView featureView,
                                               ActionMessage actionMessage)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!featureView.getFeaturestore().getProject().equals(project) ||
        (!AllowedRoles.DATA_OWNER.equalsIgnoreCase(userRole) && !featureView.getCreator().equals(user))) {
      throwForbiddenOperation(featureView.getFeaturestore(), userRole, project, featureView, actionMessage);
    }
  }

  public String resolveLocationURI(String locationURI) throws ServiceException {
    URI uri = URI.create(locationURI);
    if (Strings.isNullOrEmpty(uri.getHost())) {
      return locationURI;
    }
    if (InetAddresses.isInetAddress(uri.getHost())) {
      return locationURI;
    }
    try {
      Service nn =
          serviceDiscoveryController.getService(Type.DNS, ServiceQuery.of(uri.getHost(), Collections.emptySet()))
              .findAny()
              .orElseThrow(() -> new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.SEVERE,
                  "Service Discovery is enabled but could not resolve domain " + uri.getHost()));

      return new URI(uri.getScheme(), uri.getUserInfo(), nn.getAddress(), uri.getPort(), uri.getPath(),
          uri.getQuery(), uri.getFragment()).toString();
    } catch (ServiceDiscoveryException | URISyntaxException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.SEVERE,
          "Service Discovery is enabled but could not resolve domain " + uri.getHost(), ex.getMessage(), ex);
    }
  }

  /**
   * Prepend hdfs://namenode_ip:port to hdfs path
   * @param hdfsPath
   * @return HDFS path with namenode authority
   */
  public String prependNameNode(String hdfsPath) throws ServiceDiscoveryException {
    Service namenode = serviceDiscoveryController
            .getAnyAddressOfServiceWithDNS(HopsworksService.NAMENODE.getNameWithTag(NamenodeTags.rpc));
    return DistributedFsService.HOPSFS_SCHEME + namenode.getName() + ":" + namenode.getPort() + hdfsPath;
  }
  
  public UriBuilder featureGroupByIdURI(UriBuilder uriBuilder, Project accessProject, Featuregroup featureGroup) {
    return uriBuilder
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(accessProject.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
      .path(Integer.toString(featureGroup.getFeaturestore().getId()))
      .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
      .path(Integer.toString(featureGroup.getId()));
  }
  
  public UriBuilder featureViewURI(UriBuilder uriBuilder, Project accessProject, FeatureView featureView) {
    return uriBuilder
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(accessProject.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
      .path(Integer.toString(featureView.getFeaturestore().getId()))
      .path(ResourceRequest.Name.FEATUREVIEW.toString().toLowerCase())
      .path(featureView.getName())
      .path(ResourceRequest.Name.VERSION.toString().toLowerCase())
      .path(Integer.toString(featureView.getVersion()));
  }
  
  public UriBuilder trainingDatasetURI(UriBuilder uriBuilder, Project accessProject, TrainingDataset trainingDataset) {
    return featureViewURI(uriBuilder, accessProject, trainingDataset.getFeatureView())
      .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
      .path(ResourceRequest.Name.VERSION.toString().toLowerCase())
      .path(Integer.toString(trainingDataset.getVersion()));
  }
}
