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

package io.hops.hopsworks.common.featurestore.datavalidationv2;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.IngestionResult;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DTO containing the human-readable report from using an expectation 
 * suite to validate a dataframe in GE, can be converted to JSON or XML
 * representation
 * using jaxb.
 */


@XmlRootElement
public class ValidationReportDTO extends RestDTO<ValidationReportDTO> {
  @XmlElement
  private Integer id;
  @XmlElement
  private String evaluationParameters;
  @XmlElement
  private String statistics;
  @XmlElement
  private List<ValidationResultDTO> results;
  @XmlElement
  private String meta;
  @XmlElement
  private Boolean success;
  @XmlElement
  private Date validationTime;
  @XmlElement
  private String fullReportPath = null;
  @XmlElement
  private String fullJson;
  @XmlElement
  private IngestionResult ingestionResult;


  public ValidationReportDTO() {}

  public ValidationReportDTO(String statistics, String meta, Boolean success, 
    List<ValidationResultDTO> validationResults, String evaluationParameters) {
    this.statistics = statistics;
    this.meta = meta;
    this.success = success;
    this.results = validationResults;
    this.evaluationParameters = evaluationParameters;
  }

  public ValidationReportDTO(ValidationReport validationReport) {
    this.id = validationReport.getId();
    this.meta = validationReport.getMeta();
    this.statistics = validationReport.getStatistics();
    this.success = validationReport.getSuccess();
    this.evaluationParameters = validationReport.getEvaluationParameters();
    this.validationTime = validationReport.getValidationTime();
    
    ArrayList<ValidationResultDTO> validationResultDTOs = new ArrayList<ValidationResultDTO>();
    for(ValidationResult validationResult: validationReport.getValidationResults()) {
      ValidationResultDTO validationResultDTO = 
        new ValidationResultDTO(validationResult);
      validationResultDTOs.add(validationResultDTO);
    }
    this.results = validationResultDTOs;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getStatistics() {
    return statistics;
  }

  public void setStatistics(String statistics) {
    this.statistics = statistics;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  public String getFullJson() {
    return fullJson;
  }

  public void setFullJson(String fullJson) {
    this.fullJson = fullJson;
  }

  public String getFullReportPath() {
    return fullReportPath;
  }

  public void setFullReportPath(String fullReportPath) {
    this.fullReportPath = fullReportPath;
  }

  public String getEvaluationParameters() {
    return evaluationParameters;
  }

  public void setEvaluationParameters(String evaluationParameters) {
    this.evaluationParameters = evaluationParameters;
  }

  public List<ValidationResultDTO> getResults() {
    return results;
  }

  public void setResults(List<ValidationResultDTO> results) {
    this.results = results;
  }

  public Date getValidationTime() {
    return validationTime;
  }

  public void setValidationTime(Date validationTime) {
    this.validationTime = validationTime;
  }

  public IngestionResult getIngestionResult() {
    return ingestionResult;
  }

  public void setIngestionResult(
    IngestionResult ingestionResult) {
    this.ingestionResult = ingestionResult;
  }
  
  @Override
  public String toString() {
    return "ValidationReportDTO{" +
      "id=" + id +
      ", evaluationParameters='" + evaluationParameters + '\'' +
      ", statistics='" + statistics + '\'' +
      ", results=" + results +
      ", meta='" + meta + '\'' +
      ", success=" + success +
      ", validationTime=" + validationTime +
      ", fullReportPath='" + fullReportPath + '\'' +
      ", ingestionResult=" + ingestionResult +
      '}';
  }
}
