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

package io.hops.hopsworks.api.featurestore.datavalidationv2.results;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

import java.util.TimeZone;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import org.json.JSONObject;
import org.json.JSONException;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ValidationResultBuilder {
  @EJB
  ValidationResultController validationResultController;

  public ValidationResultDTO uri(ValidationResultDTO dto, UriInfo uriInfo, Project project,
    Featuregroup featuregroup) {
    dto.setHref(
      uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString()).path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString()).path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.VALIDATIONRESULT.toString()).build());
    return dto;
  }

  public ValidationResultDTO build(UriInfo uriInfo, Project project,
    Featuregroup featuregroup, ValidationResult validationResult) {
    ValidationResultDTO dto = new ValidationResultDTO();
    uri(dto, uriInfo, project, featuregroup);

    dto.setId(validationResult.getId());
    dto.setSuccess(validationResult.getSuccess());
    dto.setExceptionInfo(validationResult.getExceptionInfo());
    dto.setExpectationConfig(validationResult.getExpectationConfig());
    dto.setResult(validationResult.getResult());
    dto.setIngestionResult(validationResult.getIngestionResult());

    try {
      JSONObject metaJson = new JSONObject(validationResult.getMeta());
      JSONObject configJson = new JSONObject(validationResult.getExpectationConfig());
      dto.setExpectationId(configJson.getJSONObject("meta").getInt("expectationId"));
      metaJson.put("ingestionResult", validationResult.getIngestionResult());
      // Same validation string as in validationController to parse time provided by GE
      String formatDateString = "yyyy-MM-dd'T'hh:mm:ss.SSSSSSX";
      SimpleDateFormat isoFormat = new SimpleDateFormat(formatDateString);
      isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      String validationTime = isoFormat.format(validationResult.getValidationTime());
      metaJson.put("validationTime", validationTime);
      dto.setMeta(metaJson.toString());
      dto.setValidationTime(validationTime);
    } catch (JSONException exception) {
      // Metafield stored in database should always be valid json. Silently discard exception
      dto.setMeta(validationResult.getMeta());
      dto.setValidationTime(null);
      dto.setExpectationId(null);
    }

    return dto;
  }

  public ValidationResultDTO buildHistory(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featuregroup featuregroup, Integer expectationId) {

    ValidationResultDTO dtos = new ValidationResultDTO();
    uri(dtos, uriInfo, project, featuregroup);

    CollectionInfo<ValidationResult> validationResults =
      validationResultController.getAllValidationResultByExpectationId(
        resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getSort(), resourceRequest.getFilter(),
        expectationId);

    dtos.setItems(validationResults.getItems().stream()
      .map(validationResult -> build(
        uriInfo, project, featuregroup, validationResult))
      .collect(Collectors.toList()));
    dtos.setCount(validationResults.getCount());

    return dtos;
  }
}