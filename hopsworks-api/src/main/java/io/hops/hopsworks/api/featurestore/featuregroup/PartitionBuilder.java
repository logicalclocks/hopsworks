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
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;

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
public class PartitionBuilder {

  @EJB
  private FeaturegroupController featuregroupController;

  private URI uri(UriInfo uriInfo, Project project, Featuregroup featuregroup) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.PARTITIONS.toString().toLowerCase())
        .build();
  }

  public PartitionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                            Project project, Featuregroup featuregroup) throws FeaturestoreException {
    PartitionDTO partitionDTO = new PartitionDTO();
    partitionDTO.setHref(uri(uriInfo, project, featuregroup));

    List<PartitionDTO> fgPartitions =
        featuregroupController.getPartitions(featuregroup, resourceRequest.getOffset(), resourceRequest.getLimit())
            .stream()
            .map(p -> new PartitionDTO(p.getPartId(), p.getPartName()))
            .collect(Collectors.toList());
    partitionDTO.addItems(fgPartitions);

    return partitionDTO;
  }

}
