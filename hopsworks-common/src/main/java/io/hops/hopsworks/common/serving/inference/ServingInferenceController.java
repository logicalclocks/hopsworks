/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.common.serving.inference;

import io.hops.common.Pair;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.exceptions.InferenceException;

/**
 * Interface for sending inference requests to localhost or Kubernetes serving instances. Different type of serving
 * controllers e.g (localhost or Kubernetes) should implement this interface.
 */
public interface ServingInferenceController {
  Pair<Integer, String> infer(Serving serving, Integer modelVersion, String verb, String inferenceRequestJson)
    throws InferenceException;
}
