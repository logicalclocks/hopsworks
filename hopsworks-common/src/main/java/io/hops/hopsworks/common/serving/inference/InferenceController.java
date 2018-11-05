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
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.dao.serving.TfServingFacade;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.serving.inference.logger.InferenceLogger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class InferenceController {

  private static final Logger logger = Logger.getLogger(InferenceLogger.class.getName());

  @EJB
  private TfServingFacade tfServingFacade;

  @Inject
  private TfInferenceController tfInferenceController;
  @Inject
  @Any
  private Instance<InferenceLogger> inferenceLoggers;


  public String infer(Project project, String modelName, Integer modelVersion,
                      String verb, String inferenceRequestJson) throws InferenceException {

    TfServing tfServing = tfServingFacade.findByProjectModelName(project, modelName);
    if (tfServing == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVINGNOTFOUND, Level.FINE, "name: " + modelName);
    }

    // TODO(Fabio): ATM all the serving are tfServings. so we just redirect everything to the TfInferenceController
    // When we will add spark serving and or sklearn here we will invoke the different controllers
    Pair<Integer, String> inferenceResult =
        tfInferenceController.infer(tfServing, modelVersion, verb, inferenceRequestJson);

    // Log the inference
    for (InferenceLogger inferenceLogger : inferenceLoggers) {
      try {
        inferenceLogger.logInferenceRequest(tfServing, inferenceRequestJson,
            inferenceResult.getL(), inferenceResult.getR());
      } catch (Exception e) {
        // We don't want to fill the logs with inference logging errors
        logger.log(Level.FINE, "Error logging inference for logger: " + inferenceLogger.getClassName(), e);
      }
    }

    // If the inference server returned something different than 200 then throw an exception to the user
    if (inferenceResult.getL() >= 500) {
      logger.log(Level.FINE, "Request error: " + inferenceResult.getL() + " - " + inferenceResult.getR());
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVINGINSTANCEINTERNAL, Level.FINE,
          inferenceResult.getR());
    } else if (inferenceResult.getL() >= 400) {
      logger.log(Level.FINE, "Request error: " + inferenceResult.getL() + " - " + inferenceResult.getR());
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVINGINSTANCEBADREQUEST, Level.FINE,
          inferenceResult.getR());
    }

    return inferenceResult.getR();
  }
}
