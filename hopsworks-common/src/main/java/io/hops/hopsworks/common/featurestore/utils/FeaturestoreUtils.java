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
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.logging.Level;

@Stateless
public class FeaturestoreUtils {

  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  /**
   * Verify that the user is allowed to execute the requested operation based on his/hers project role
   * <p>
   * Only data owners are allowed to update/delete feature groups/training datasets
   * created by someone else in the project
   *
   * @param trainingDataset the training dataset the operation concerns
   * @param featurestore the featurestore that the operation concerns
   * @param project the project of the featurestore
   * @param user the user requesting the operation
   * @throws FeaturestoreException
   */
  public void verifyUserRole(TrainingDataset trainingDataset, Featurestore featurestore, Users user, Project project)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!trainingDataset.getCreator().equals(user) &&
        !userRole.equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.UNAUTHORIZED_FEATURESTORE_OPERATION, Level.FINE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", Training dataset: " + trainingDataset.getName() + ", userRole:" + userRole +
              ", creator of the featuregroup: " + trainingDataset.getCreator().getEmail());
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
}
