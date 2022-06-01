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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "validation_result", catalog = "hopsworks")
@XmlRootElement
public class ValidationResult implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "validation_report_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private ValidationReport validationReport;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "success")
  private Boolean success;
  
  @Basic
  @Column(name = "meta")
  private String meta;

  @Basic
  @Column(name = "result")
  private String result;

  @Basic
  @Column(name = "expectation_config")
  private String expectationConfig;

  @Basic
  @Column(name = "exception_info")
  private String exceptionInfo;
  
  @JoinColumn(name = "expectation_id", referencedColumnName = "id")
  @ManyToOne
  private Expectation expectation;
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public ValidationReport getValidationReport() {
    return validationReport;
  }
  
  public void setValidationReport(
    ValidationReport validationReport) {
    this.validationReport = validationReport;
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
  
  public Expectation getExpectation() {
    return expectation;
  }
  
  public void setExpectation(Expectation expectation) {
    this.expectation = expectation;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidationResult that = (ValidationResult) o;
    return Objects.equals(id, that.id) && Objects.equals(validationReport, 
      that.validationReport) && Objects.equals(result, that.result) && 
      Objects.equals(meta, that.meta) && Objects.equals(expectation, that.expectation) &&
      Objects.equals(expectationConfig, that.expectationConfig);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(
      id, validationReport.getId(), result, expectationConfig, meta, expectation.getId());
  }
}
