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

package io.hops.hopsworks.common.featurestore.datavalidationv2.results;

import javax.xml.bind.annotation.XmlRootElement;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.IngestionResult;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;

@XmlRootElement
public class ValidationResultDTO extends RestDTO<ValidationResultDTO> {
  private Integer id;
  private String expectationConfig;
  private String result;
  private Boolean success;
  private String meta;
  private String exceptionInfo;
  private String validationTime;
  private IngestionResult ingestionResult;
  private Integer expectationId;

  public ValidationResultDTO() {}

  public ValidationResultDTO(String expectationConfig, Boolean success, 
    String result, String meta, String exceptionInfo) {
    this.expectationConfig = expectationConfig;
    this.success = success;
    this.result = result;
    this.meta = meta;
    this.exceptionInfo = exceptionInfo;
  }

  public ValidationResultDTO(ValidationResult validationResult) {
    this.id = validationResult.getId();
    this.expectationConfig = validationResult.getExpectationConfig();
    this.meta = validationResult.getMeta();
    this.success = validationResult.getSuccess();
    this.exceptionInfo = validationResult.getExceptionInfo();
    this.result = validationResult.getResult();
  }

  public String getExpectationConfig() {
    return expectationConfig;
  }

  public void setExpectationConfig(String expectationConfig) {
    this.expectationConfig = expectationConfig;
  }

  public String getExceptionInfo() {
    return exceptionInfo;
  }

  public void setExceptionInfo(String exceptionInfo) {
    this.exceptionInfo = exceptionInfo;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getExpectationId() {
    return expectationId;
  }

  public void setExpectationId(Integer expectationId) {
    this.expectationId = expectationId;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  public String getValidationTime() {
    return validationTime;
  }

  public void setValidationTime(String validationTime) {
    this.validationTime = validationTime;
  }

  public IngestionResult getIngestionResult() {
    return ingestionResult;
  }

  public void setIngestionResult(IngestionResult ingestionResult) {
    this.ingestionResult = ingestionResult;
  }

  @Override
  public String toString() {
    return "ValidationResultDTO{"
      + "success: " + success
      + ", result: " + result
      + ", expectationConfig: " + expectationConfig
      + ", exceptionInfo: " + exceptionInfo
      + ", meta: " + meta
      + ", validationTime: " + validationTime
      + ", ingestionResult: " + ingestionResult
      + "}";
  }
}
