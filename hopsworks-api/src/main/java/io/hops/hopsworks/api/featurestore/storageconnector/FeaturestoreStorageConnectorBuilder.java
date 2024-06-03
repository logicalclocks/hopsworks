/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.storageconnector;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.util.AbstractFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreStorageConnectorBuilder {

  @EJB
  private FeaturestoreStorageConnectorController storageConnectorController;

  public FeaturestoreStorageConnectorDTO build(AbstractFacade.CollectionInfo<FeaturestoreConnector> connectors,
        Featurestore featurestore, Project project, Users user, ResourceRequest resourceRequest, UriInfo uriInfo)
      throws FeaturestoreException {
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO = new FeaturestoreStorageConnectorDTO();
    featurestoreStorageConnectorDTO.setHref(uriInfo.getRequestUri());

    for (FeaturestoreConnector featurestoreConnector : connectors.getItems()) {
      featurestoreStorageConnectorDTO.addItem(
        storageConnectorController.convertToConnectorDTO(user, project, featurestoreConnector));
    }
    featurestoreStorageConnectorDTO.setCount(connectors.getCount());
    return featurestoreStorageConnectorDTO;
  }

}
