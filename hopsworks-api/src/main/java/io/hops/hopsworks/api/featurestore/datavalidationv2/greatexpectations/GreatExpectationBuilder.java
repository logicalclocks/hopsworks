/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.datavalidationv2.greatexpectations;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.GreatExpectation;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GreatExpectationBuilder {

  @EJB
  ExpectationSuiteController expectationSuiteController;

  public GreatExpectationDTO uri(GreatExpectationDTO dto, UriInfo uriInfo, Project project, 
    Featurestore featurestore) {
    dto.setHref(
      uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString()).path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString())
        .path(Integer.toString(featurestore.getId()))
        .path(ResourceRequest.Name.GREATEXPECTATIONS.toString()).build());
    return dto;
  }

  public GreatExpectationDTO build(UriInfo uriInfo, Project project,
    Featurestore featurestore, GreatExpectation greatExpectation) {
    GreatExpectationDTO greatExpectationDTO = new GreatExpectationDTO(greatExpectation);
    uri(greatExpectationDTO, uriInfo, project, featurestore);

    return greatExpectationDTO;
  }

  public GreatExpectationDTO build(UriInfo uriInfo, Project project, Featurestore featurestore) {
    GreatExpectationDTO dtos = new GreatExpectationDTO();
    uri(dtos, uriInfo, project, featurestore);

    CollectionInfo<GreatExpectation> greatExpectations = expectationSuiteController.getAllGreatExpectations();
    dtos.setItems((List<GreatExpectationDTO>) greatExpectations.getItems().stream()
          .map(greatExpectation -> build(
            uriInfo, project, featurestore, (GreatExpectation) greatExpectation))
          .collect(Collectors.toList()));
    dtos.setCount(greatExpectations.getCount());

    return dtos;
  }

}
