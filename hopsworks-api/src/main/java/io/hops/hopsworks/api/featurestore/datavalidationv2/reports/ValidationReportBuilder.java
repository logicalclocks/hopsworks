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

package io.hops.hopsworks.api.featurestore.datavalidationv2.reports;

import io.hops.hopsworks.api.featurestore.datavalidationv2.results.ValidationResultBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidationv2.reports.ValidationReportController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.reports.ValidationReportDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultDTO;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ValidationReportBuilder {

  @EJB
  private ValidationReportController validationReportController;
  @EJB
  private ValidationResultBuilder validationResultBuilder;


  public ValidationReportDTO uri(ValidationReportDTO dto, UriInfo uriInfo, Project project, 
    Featuregroup featuregroup) {
    dto.setHref(
      uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString()).path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString())
        .path(Integer.toString(featuregroup.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString()).path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.VALIDATIONREPORT.toString()).build());
    return dto;
  }

  public ValidationReportDTO build(UriInfo uriInfo, Project project,
    Featuregroup featuregroup, ValidationReport validationReport) {
    ValidationReportDTO dto = new ValidationReportDTO();
    uri(dto, uriInfo, project, featuregroup);
    Optional<Dataset> validationDatasetOptional = validationReportController.getValidationDataset(project);
    String path = "";
    // the validation dataset will probably always be there if we have the validation reports in db
    if (validationDatasetOptional.isPresent()) {
      Path validationReportDir = validationReportController
          .getValidationReportDirFullPath(featuregroup, validationDatasetOptional.get());
      Path validationReportFileFullPath = new Path(validationReportDir, validationReport.getFileName());
      path = validationReportFileFullPath.toString();
    }
    dto.setMeta(validationReport.getMeta());
    dto.setId(validationReport.getId());
    dto.setSuccess(validationReport.getSuccess());
    dto.setStatistics(validationReport.getStatistics());
    dto.setValidationTime(validationReport.getValidationTime());
    dto.setEvaluationParameters(validationReport.getEvaluationParameters());
    dto.setFullReportPath(path);
    dto.setIngestionResult(validationReport.getIngestionResult());

    List<ValidationResultDTO> validationResultDTOs = new ArrayList<>();

    for (ValidationResult validationResult : validationReport.getValidationResults()) {
      validationResultDTOs.add(validationResultBuilder.build(uriInfo, project, featuregroup, validationResult));
    }
    dto.setResults(validationResultDTOs);
    return dto;
  }

  public ValidationReportDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featuregroup featuregroup) {

    ValidationReportDTO dtos = new ValidationReportDTO();
    uri(dtos, uriInfo, project, featuregroup);

    CollectionInfo<ValidationReport> validationReports =
      validationReportController.getAllValidationReportByFeatureGroup(
      resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getSort(), resourceRequest.getFilter(),
      featuregroup);

    dtos.setItems(validationReports.getItems().stream()
          .map(validationReport -> build(
            uriInfo, project, featuregroup, validationReport))
          .collect(Collectors.toList()));
    dtos.setCount(validationReports.getCount());
    
    return dtos;
  }

}