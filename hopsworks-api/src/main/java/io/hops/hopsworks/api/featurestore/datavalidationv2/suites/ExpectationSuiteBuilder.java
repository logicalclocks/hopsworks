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

package io.hops.hopsworks.api.featurestore.datavalidationv2.suites;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ExpectationDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ExpectationSuiteDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExpectationSuiteBuilder {

  public ExpectationSuiteDTO uri(ExpectationSuiteDTO dto, UriInfo uriInfo, Project project, Featuregroup featuregroup) {
    dto.setHref(
      uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString()).path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString()).path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.EXPECTATIONSUITE.toString()).build());
    return dto;
  }

  public ExpectationSuiteDTO build(UriInfo uriInfo, Project project,
    Featuregroup featureGroup, ExpectationSuite expectationSuite) throws FeaturestoreException {
    ExpectationSuiteDTO dto = new ExpectationSuiteDTO();
    uri(dto, uriInfo, project, featureGroup);
    
    if (expectationSuite == null) {
      dto.setCount(0L);
      return dto;
    }

    dto.setGeCloudId(expectationSuite.getGeCloudId());
    dto.setDataAssetType(expectationSuite.getDataAssetType());
    dto.setRunValidation(expectationSuite.getRunValidation());
    dto.setValidationIngestionPolicy(expectationSuite.getValidationIngestionPolicy());
    dto.setId(expectationSuite.getId());
    dto.setMeta(expectationSuite.getMeta());
    dto.setExpectationSuiteName(expectationSuite.getName());

    List<ExpectationDTO> expectationDTOs = new ArrayList<ExpectationDTO>();

    for (Expectation expectation : expectationSuite.getExpectations()) {
      ExpectationDTO expectationDTO = new ExpectationDTO(expectation);

      // Set expectationId in the meta field
      try {
        JSONObject meta = new JSONObject(expectation.getMeta());
        meta.put("expectationId", expectation.getId());
        expectationDTO.setMeta(meta.toString());
      } catch (JSONException e) {
        // Argument can be made that we simply return it, rather than throwing an exception
        throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.FAILED_TO_PARSE_EXPECTATION_META_FIELD, Level.SEVERE,
          String.format("Expectation meta field is not valid json : %s", expectation.getMeta())
        );
      }
      expectationDTOs.add(expectationDTO);
    }
    dto.setExpectations(expectationDTOs);
    return dto;
  }

}