/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Tensorflow Localhost Inference Controller
 *
 * Provides utility methods concerning Tensorflow local serving
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LocalhostTfInferenceUtils {
  
  public String getPath(String servingName, Integer modelVersion, String verb) {
    
    // TODO(Fabio) does Tf model server support TLS?
    StringBuilder pathBuilder = new StringBuilder()
      .append("/v1/models/")
      .append(servingName);
  
    // Append the version if the user specified it.
    if (modelVersion != null) {
      pathBuilder.append("/versions/").append(modelVersion);
    }
    
    pathBuilder.append(verb);
    return pathBuilder.toString();
  }
}
