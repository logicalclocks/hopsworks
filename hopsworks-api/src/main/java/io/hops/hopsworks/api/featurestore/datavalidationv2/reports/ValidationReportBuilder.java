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

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ValidationReportController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ValidationReportDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.ValidationResultDTO;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ValidationReportBuilder {

  @EJB
  private ValidationReportController validationReportController;
  @EJB
  private InodeController inodeController;


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

    dto.setMeta(validationReport.getMeta());
    dto.setId(validationReport.getId());
    dto.setSuccess(validationReport.getSuccess());
    dto.setStatistics(validationReport.getStatistics());
    dto.setValidationTime(validationReport.getValidationTime());
    dto.setEvaluationParameters(validationReport.getEvaluationParameters());
    dto.setFullReportPath(inodeController.getPath(validationReport.getInode()));
    dto.setIngestionResult(validationReport.getIngestionResult());

    List<ValidationResultDTO> validationResultDTOs = new ArrayList<ValidationResultDTO>();

    for (ValidationResult validationResult : validationReport.getValidationResults()) {
      ValidationResultDTO validationResultDTO = new ValidationResultDTO(validationResult);
      validationResultDTOs.add(validationResultDTO);
    }
    dto.setResults(validationResultDTOs);
    return dto;
  }

  public ValidationReportDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featuregroup featuregroup) throws FeaturestoreException {

    ValidationReportDTO dtos = new ValidationReportDTO();
    uri(dtos, uriInfo, project, featuregroup);

    CollectionInfo<ValidationReport> validationReports =
      validationReportController.getAllValidationReportByFeatureGroup(
      resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getSort(), resourceRequest.getFilter(),
      featuregroup);

    dtos.setItems((List<ValidationReportDTO>) validationReports.getItems().stream()
          .map(validationReport -> build(
            uriInfo, project, featuregroup, (ValidationReport) validationReport))
          .collect(Collectors.toList()));
    dtos.setCount(validationReports.getCount());
    
    return dtos;
  }

}