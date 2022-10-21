/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidationv2.expectations;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExpectationBuilder {
  @EJB
  private ExpectationController expectationController;

  public ExpectationDTO uri(ExpectationDTO dto, UriInfo uriInfo, Project project, Featuregroup featuregroup,
    ExpectationSuite expectationSuite) {
    dto.setHref(
      uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString()).path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString()).path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.EXPECTATIONSUITE.toString()).path(Integer.toString(expectationSuite.getId()))
        .path(ResourceRequest.Name.EXPECTATIONS.toString()).build());
    return dto;
  }

  public ExpectationDTO build(UriInfo uriInfo, Project project, Featuregroup featureGroup,
    ExpectationSuite expectationSuite, Expectation expectation) throws FeaturestoreException {

    Expectation expectationWithIdInMeta = expectationController.addExpectationIdToMetaField(expectation);
    
    ExpectationDTO expectationDTO = new ExpectationDTO(expectationWithIdInMeta);
    
    uri(expectationDTO, uriInfo, project, featureGroup, expectationSuite);

    return expectationDTO;
  }

  public ExpectationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featuregroup featuregroup, ExpectationSuite expectationSuite) throws FeaturestoreException {

    ExpectationDTO dtos = new ExpectationDTO();
    uri(dtos, uriInfo, project, featuregroup, expectationSuite);

    CollectionInfo<Expectation> expectations =
      expectationController.getExpectationsByExpectationSuite(expectationSuite);
    
    ArrayList<ExpectationDTO> listOfExpectationDTO = new ArrayList<>();
    for (Expectation expectation: expectations.getItems()) {
      listOfExpectationDTO.add(build(uriInfo, project, featuregroup, expectationSuite, expectation));
    }

    dtos.setItems(listOfExpectationDTO);
    dtos.setCount(expectations.getCount());
    
    return dtos;
  }
}
