/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("batchingConfiguration")
public class BatchingConfiguration {
  private Boolean batchingEnabled;
  private Integer maxBatchSize;
  private Integer maxLatency;
  private Integer timeout;

  public Boolean isBatchingEnabled() { return batchingEnabled; }

  public void setBatchingEnabled(boolean batchingEnabled) { this.batchingEnabled = batchingEnabled; }

  public Integer getMaxBatchSize() { return maxBatchSize; }

  public void setMaxBatchSize(Integer maxBatchSize) { this.maxBatchSize = maxBatchSize; }

  public Integer getMaxLatency() { return maxLatency; }

  public void setMaxLatency(Integer maxLatency) { this.maxLatency = maxLatency; }

  public Integer getTimeout() { return timeout; }

  public void setTimeout(Integer timeout) { this.timeout = timeout; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BatchingConfiguration configuration = (BatchingConfiguration) o;
    return batchingEnabled == configuration.isBatchingEnabled() &&
        maxBatchSize == configuration.getMaxBatchSize() && maxLatency == configuration.getMaxLatency()
        && timeout == configuration.getTimeout();
  }

}