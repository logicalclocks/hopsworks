/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.kafka.KafkaClusterInfoDTO;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaClusterInfoBuilder {
  
  public KafkaClusterInfoDTO build(UriInfo uriInfo, Project project, List<String> brokers) {
    URI href = uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(ResourceRequest.Name.CLUSTERINFO.toString().toLowerCase())
      .build();
    
    KafkaClusterInfoDTO dto = new KafkaClusterInfoDTO();
    dto.setHref(href);
    dto.setBrokers(brokers);
    return dto;
  }
}
