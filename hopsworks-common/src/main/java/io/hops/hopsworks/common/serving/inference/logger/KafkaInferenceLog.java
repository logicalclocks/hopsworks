/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.inference.logger;

import io.hops.hopsworks.common.dao.serving.TfServing;

import java.io.Serializable;

public class KafkaInferenceLog implements Serializable {

  private Integer modelId;
  private String modelName;
  private Integer modelVersion;

  private Long requestTimestamp;

  private Integer responseHttpCode;
  private String inferenceRequest;
  private String inferenceResponse;

  public KafkaInferenceLog() {
  }

  public KafkaInferenceLog(TfServing tfServing, String inferenceRequest,
                           Integer responseHttpCode, String inferenceResponse) {
    this.modelId = tfServing.getId();
    this.modelName = tfServing.getModelName();
    this.modelVersion = tfServing.getVersion();

    this.requestTimestamp = System.currentTimeMillis();

    this.responseHttpCode = responseHttpCode;
    this.inferenceRequest = inferenceRequest;
    this.inferenceResponse = inferenceResponse;
  }

  public Integer getModelId() {
    return modelId;
  }

  public void setModelId(Integer modelId) {
    this.modelId = modelId;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public Integer getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(Integer modelVersion) {
    this.modelVersion = modelVersion;
  }

  public Long getRequestTimestamp() {
    return requestTimestamp;
  }

  public void setRequestTimestamp(Long requestTimestamp) {
    this.requestTimestamp = requestTimestamp;
  }

  public String getInferenceRequest() {
    return inferenceRequest;
  }

  public void setInferenceRequest(String inferenceRequest) {
    this.inferenceRequest = inferenceRequest;
  }

  public String getInferenceResponse() {
    return inferenceResponse;
  }

  public void setInferenceResponse(String inferenceResponse) {
    this.inferenceResponse = inferenceResponse;
  }

  public Integer getResponseHttpCode() {
    return responseHttpCode;
  }

  public void setResponseHttpCode(Integer responseHttpCode) {
    this.responseHttpCode = responseHttpCode;
  }
}
