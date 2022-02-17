/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.transformationFunction;

import io.hops.hopsworks.common.api.RestDTO;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a trainingDataset transformation function builder function,
 * can be converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
public class TransformationFunctionDTO extends RestDTO<TransformationFunctionDTO> {
  private Integer id;
  private String name;
  private String outputType;
  private Integer version;
  private String sourceCodeContent;
  private Integer featurestoreId;

  public TransformationFunctionDTO(){
  }

  public TransformationFunctionDTO(Integer id, String name, String outputType, Integer version,
                                   String sourceCodeContent, Integer featurestoreId){
    this(name, outputType, version, sourceCodeContent, featurestoreId);
    this.id = id;
  }

  public TransformationFunctionDTO(String name, String outputType, Integer version,
    String sourceCodeContent, Integer featurestoreId){
    this.name = name;
    this.outputType = outputType;
    this.version = version;
    this.sourceCodeContent = sourceCodeContent;
    this.featurestoreId = featurestoreId;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOutputType() {
    return outputType;
  }

  public void setOutputType(String outputType) {
    this.outputType = outputType;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getSourceCodeContent() {
    return sourceCodeContent;
  }

  public void setSourceCodeContent(String sourceCodeContent) {
    this.sourceCodeContent = sourceCodeContent;
  }

  public Integer getFeaturestoreId() {
    return featurestoreId;
  }

  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }
}
