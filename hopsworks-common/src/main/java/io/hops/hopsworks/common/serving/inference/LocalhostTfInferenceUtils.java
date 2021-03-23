/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
      pathBuilder.append("/versions").append(modelVersion);
    }
    
    pathBuilder.append(verb);
    return pathBuilder.toString();
  }
}
