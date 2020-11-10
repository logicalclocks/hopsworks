/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featuregroup;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.hive.HiveTableType;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.Storage;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DetailsBuilder {

  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;


  private URI uri(UriInfo uriInfo, Project project, Featuregroup featuregroup) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.DETAILS.toString().toLowerCase())
        .build();
  }

  public DetailsDTO build(UriInfo uriInfo, Users user, Project project, Featuregroup featuregroup, Storage storage)
      throws FeaturestoreException, HopsSecurityException {
    DetailsDTO detailsDTO = null;

    switch (storage) {
      case OFFLINE:
        detailsDTO = buildOfflineDetails(user, project, featuregroup);
        break;
      case ONLINE:
        detailsDTO = buildOnlineDetails(featuregroup);
        break;
      case ALL:
        throw new IllegalArgumentException("Cannot retrieve feature group details for storage ALL");
    }

    detailsDTO.setHref(uri(uriInfo, project, featuregroup));
    return detailsDTO;
  }

  private DetailsDTO buildOfflineDetails(Users user, Project project, Featuregroup featuregroup)
      throws FeaturestoreException, HopsSecurityException{
    DetailsDTO detailsDTO = new DetailsDTO();
    detailsDTO.setSchema(cachedFeaturegroupController.getDDLSchema(featuregroup, project, user));
    detailsDTO.setHiveTableType(HiveTableType.valueOf(featuregroup.getCachedFeaturegroup().getHiveTbls().getTblType()));
    detailsDTO.setInputFormat(featuregroup.getCachedFeaturegroup().getHiveTbls().getSdId().getInputFormat());

    // Get table param containing the table size
    detailsDTO.setSize(featuregroup.getCachedFeaturegroup().getHiveTbls().getHiveTableParamsCollection().stream()
        .filter(param -> param.getHiveTableParamsPK().getParamKey().equalsIgnoreCase("totalSize"))
        .map(param -> Long.valueOf(param.getParamValue()))
        .findFirst()
        .orElse(null));

    return detailsDTO;
  }

  private DetailsDTO buildOnlineDetails(Featuregroup featuregroup) throws FeaturestoreException {
    if (!featuregroup.getCachedFeaturegroup().isOnlineEnabled()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_ONLINE, Level.FINE,
          featuregroup.getName() + " is not available online");
    }

    DetailsDTO detailsDTO = new DetailsDTO();
    detailsDTO.setSchema(onlineFeaturegroupController.getFeaturegroupSchema(featuregroup));
    detailsDTO.setSize(onlineFeaturegroupController.getFeaturegroupSize(featuregroup));
    return detailsDTO;
  }
}
