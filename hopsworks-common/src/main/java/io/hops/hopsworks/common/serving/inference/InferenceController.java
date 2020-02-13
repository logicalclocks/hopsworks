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

import com.google.common.base.Strings;
import io.hops.common.Pair;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.persistence.entity.serving.ServingType;
import io.hops.hopsworks.common.serving.inference.logger.InferenceLogger;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains the common functionality between serving instances for doing inference, delegates type-specific
 * functionality to specific inference controllers like TfInferenceController, and SkLearnInferenceController
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class InferenceController {

  private static final Logger logger = Logger.getLogger(InferenceLogger.class.getName());

  @EJB
  private ServingFacade servingFacade;

  @Inject
  private TfInferenceController tfInferenceController;
  @Inject
  private SkLearnInferenceController skLearnInferenceController;
  @Inject
  @Any
  private Instance<InferenceLogger> inferenceLoggers;
  
  
  /**
   * Makes an inference request to a running serving instance
   *
   * @param project the project where the serving is running
   * @param modelName the name of the serving
   * @param modelVersion the version of the serving
   * @param verb the predictiont type (predict, regress, or classify)
   * @param inferenceRequestJson the user-provided JSON payload for the inference request
   * @return a string representation of the inference result
   * @throws InferenceException
   */
  public String infer(Project project, String modelName, Integer modelVersion,
                      String verb, String inferenceRequestJson) throws InferenceException {

    Serving serving = servingFacade.findByProjectAndName(project, modelName);
    if (serving == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_FOUND, Level.FINE, "name: " + modelName);
    }
  
    if (Strings.isNullOrEmpty(verb)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.MISSING_VERB, Level.FINE);
    }

    Pair<Integer, String> inferenceResult = null;
    if(serving.getServingType() == ServingType.TENSORFLOW){
      inferenceResult =
          tfInferenceController.infer(serving, modelVersion, verb, inferenceRequestJson);
    }
    if(serving.getServingType() == ServingType.SKLEARN){
      inferenceResult =
          skLearnInferenceController.infer(serving, modelVersion, verb, inferenceRequestJson);
    }

    // Log the inference
    for (InferenceLogger inferenceLogger : inferenceLoggers) {
      try {
        inferenceLogger.logInferenceRequest(serving, inferenceRequestJson,
            inferenceResult.getL(), inferenceResult.getR());
      } catch (Exception e) {
        // We don't want to fill the logs with inference logging errors
        logger.log(Level.FINE, "Error logging inference for logger: " + inferenceLogger.getClassName(), e);
      }
    }

    // If the inference server returned something different than 200 then throw an exception to the user
    if (inferenceResult.getL() >= 500) {
      logger.log(Level.FINE, "Request error: " + inferenceResult.getL() + " - " + inferenceResult.getR());
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.FINE,
          inferenceResult.getR());
    } else if (inferenceResult.getL() >= 400) {
      logger.log(Level.FINE, "Request error: " + inferenceResult.getL() + " - " + inferenceResult.getR());
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_BAD_REQUEST, Level.FINE,
          inferenceResult.getR());
    }

    return inferenceResult.getR();
  }

}
