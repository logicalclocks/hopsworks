/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.common.serving.inference;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * SkLearn Localhost Inference Controller
 *
 * Sends inference requests to a local sklearn flask server to get a prediction response
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LocalhostSkLearnInferenceUtils {
  
  public String getPath(String verb) {
    StringBuilder pathBuilder = new StringBuilder().append("/").append(verb.replaceFirst(":", ""));
    return pathBuilder.toString();
  }
}
