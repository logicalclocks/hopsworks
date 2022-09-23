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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationIngestionPolicy;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information about expectation suite from GE, can be converted to JSON or XML
 * representation
 * using jaxb.
 */


@XmlRootElement
public class ExpectationSuiteDTO extends RestDTO<ExpectationSuiteDTO> {
  private Integer id;
  private String dataAssetType = null;
  private String expectationSuiteName;
  private List<ExpectationDTO> expectations;
  private String meta = "{\"great_expectations_version\": \"0.14.12\"}";
  private String geCloudId = null;
  private ValidationIngestionPolicy validationIngestionPolicy = ValidationIngestionPolicy.ALWAYS;
  private boolean runValidation = true;

  public ExpectationSuiteDTO() {}

  public ExpectationSuiteDTO(ExpectationSuite expectationSuite) {
    this.id = expectationSuite.getId();
    this.geCloudId = expectationSuite.getGeCloudId();
    this.dataAssetType = expectationSuite.getDataAssetType();
    this.meta = expectationSuite.getMeta();
    this.expectationSuiteName = expectationSuite.getName();
    this.validationIngestionPolicy = expectationSuite.getValidationIngestionPolicy();
    this.runValidation = expectationSuite.getRunValidation();
    
    ArrayList<ExpectationDTO> expectationDTOs = new ArrayList<>();
    for(Expectation expectation: expectationSuite.getExpectations()) {
      ExpectationDTO expectationDTO = new ExpectationDTO(
        expectation.getExpectationType(), expectation.getKwargs(), expectation.getMeta()
      );
      expectationDTOs.add(expectationDTO);
    }
    this.expectations = expectationDTOs;
  }

  public String getExpectationSuiteName() {
    return expectationSuiteName;
  }

  public void setExpectationSuiteName(String expectationSuiteName) {
    this.expectationSuiteName = expectationSuiteName;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }


  public String getDataAssetType() {
    return dataAssetType;
  }

  public void setDataAssetType(String dataAssetType) {
    this.dataAssetType = dataAssetType;
  }

  public String getGeCloudId() {
    return geCloudId;
  }

  public void setGeCloudId(String geCloudId) {
    this.geCloudId = geCloudId;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  public List<ExpectationDTO> getExpectations() {
    return expectations;
  }

  public void setExpectations(List<ExpectationDTO> expectations) {
    this.expectations = expectations;
  }

  public boolean getRunValidation() {
    return  runValidation;
  }

  public void setRunValidation(boolean runValidation) {
    this.runValidation = runValidation;
  }

  public ValidationIngestionPolicy getValidationIngestionPolicy() {
    return validationIngestionPolicy;
  }

  public void setValidationIngestionPolicy(ValidationIngestionPolicy validationIngestionPolicy) {
    this.validationIngestionPolicy = validationIngestionPolicy;
  }

  @Override
  public String toString() {
    return "ExpectationSuiteDTO{"
      + "expectationSuiteName: " + expectationSuiteName 
      + ", meta: " + meta
      + ", geCloudId:" + geCloudId
      + ", dataAssetType: " + dataAssetType
      + ", validationIngestionPolicy: " + validationIngestionPolicy
      + ", runValidation :" + runValidation
      + ", expectations:[" + expectations
      + "]}";
  }
}
