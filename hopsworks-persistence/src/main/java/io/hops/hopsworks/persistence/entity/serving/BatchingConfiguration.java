/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchingConfiguration implements Serializable {

  private Boolean batchingEnabled;
  private Integer maxBatchSize;
  private Integer maxLatency;
  private Integer timeout;

  public BatchingConfiguration() {
  }

  public Boolean isBatchingEnabled() { return batchingEnabled; }
  public void setBatchingEnabled(boolean batchingEnabled) { this.batchingEnabled = batchingEnabled; }

  public Integer getMaxBatchSize() { return maxBatchSize; }
  public void setMaxBatchSize(Integer maxBatchSize) { this.maxBatchSize = maxBatchSize; }

  public Integer getMaxLatency() { return maxLatency; }
  public void setMaxLatency(Integer maxLatency) { this.maxLatency = maxLatency; }

  public Integer getTimeout() { return timeout; }
  public void setTimeout(Integer timeout) { this.timeout = timeout; }
  
  @Override
  public int hashCode() {
    return Objects.hash(batchingEnabled, maxBatchSize, maxLatency, timeout);
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BatchingConfiguration configuration = (BatchingConfiguration) o;
    return batchingEnabled == configuration.isBatchingEnabled() &&
      Objects.equals(maxBatchSize, configuration.getMaxBatchSize()) &&
      Objects.equals(maxLatency, configuration.getMaxLatency())
       && Objects.equals(timeout, configuration.getTimeout());
  }

}