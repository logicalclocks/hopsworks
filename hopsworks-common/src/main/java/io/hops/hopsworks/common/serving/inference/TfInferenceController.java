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

package io.hops.hopsworks.common.serving.inference;

import io.hops.common.Pair;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.exceptions.InferenceException;

/**
 * Interface for sending inference requests to tfserving serving instances. Different type of tf serving
 * controllers e.g (localhost or Kubernetes) should implement this interface.
 */
public interface TfInferenceController {
  Pair<Integer, String> infer(Serving serving, Integer modelVersion,
                              String verb, String inferenceRequestJson) throws InferenceException;
}
