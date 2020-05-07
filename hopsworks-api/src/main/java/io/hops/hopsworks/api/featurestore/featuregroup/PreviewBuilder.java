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
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PreviewBuilder {

  @EJB
  private FeaturegroupController featuregroupController;

  private URI uri(UriInfo uriInfo, Project project, Featuregroup featuregroup) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.PREVIEW.toString().toLowerCase())
        .build();
  }

  public PreviewDTO build(UriInfo uriInfo, Users user, Project project, Featuregroup featuregroup,
                          String partition, boolean online, int limit)
      throws FeaturestoreException, HopsSecurityException {

    FeaturegroupPreview preview = null;
    try {
      preview = featuregroupController.getFeaturegroupPreview(featuregroup, project, user, partition, online, limit);
    } catch (SQLException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_PREVIEW_FEATUREGROUP,
          Level.SEVERE, "Feature Group id: " + featuregroup.getId(), e.getMessage(), e);
    }

    PreviewDTO previewDTO = new PreviewDTO();
    previewDTO.setHref(uri(uriInfo, project, featuregroup));
    previewDTO.setExpand(true);

    previewDTO.setItems(preview.getPreview().stream()
        .map(r -> r.getValues().stream()
            .map(c -> new ColumnDTO(c.getValue0(), c.getValue1())).collect(Collectors.toList()))
        .map(PreviewDTO::new)
        .collect(Collectors.toList()));

    return previewDTO;
  }

}
