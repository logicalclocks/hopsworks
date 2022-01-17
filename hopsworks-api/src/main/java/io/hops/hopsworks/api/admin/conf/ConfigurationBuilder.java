/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.admin.conf;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.util.VariablesFacade;
import io.hops.hopsworks.persistence.entity.util.Variables;

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
public class ConfigurationBuilder {

  @EJB
  private VariablesFacade variablesFacade;

  private URI uri(UriInfo uriInfo) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.ADMIN.toString().toLowerCase())
        .path(ResourceRequest.Name.CONFIGURATION.toString().toLowerCase())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.CONFIGURATION);
  }

  public ConfigurationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest) {
    List<Variables> variables = variablesFacade.findAll();

    ConfigurationDTO configurationDTO = new ConfigurationDTO();
    configurationDTO.setHref(uri(uriInfo));
    configurationDTO.setExpand(expand(resourceRequest));
    configurationDTO.setCount((long) variables.size());
    if (configurationDTO.isExpand()) {
      configurationDTO.setItems(variables.stream()
          .map(v -> new ConfigurationDTO(v.getId(), v.getValue(), v.getVisibility(), v.isHide()))
          .collect(Collectors.toList()));
    }

    return configurationDTO;
  }
}
