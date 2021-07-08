/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inference logging type
 */

public enum InferenceLogging {
  // Order is important
  @JsonProperty("PREDICTIONS")
  PREDICTIONS,
  @JsonProperty("MODEL_INPUTS")
  MODEL_INPUTS,
  @JsonProperty("ALL")
  ALL,
}
