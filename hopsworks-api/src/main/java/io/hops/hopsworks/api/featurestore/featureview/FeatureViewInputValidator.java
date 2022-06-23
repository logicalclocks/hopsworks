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

package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetInputValidation;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewInputValidator {

  @EJB
  private TrainingDatasetInputValidation trainingDatasetInputValidation;
  @EJB
  private QueryController queryController;

  public void validate(FeatureViewDTO featureViewDTO, Project project, Users user) throws FeaturestoreException {
    validateCreationInput(featureViewDTO);
    Query query = queryController.convertQueryDTO(project, user, featureViewDTO.getQuery(), false);
    validateVersion(featureViewDTO.getVersion());
    // TODO feature view: featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);
    trainingDatasetInputValidation.validateFeatures(query, featureViewDTO.getFeatures());
    if (featureViewDTO.getFeatures() != null) {
      trainingDatasetInputValidation.verifyTrainingDatasetFeatureList(featureViewDTO.getFeatures());
    }
  }

  public void validateCreationInput(FeatureViewDTO featureViewDTO) throws FeaturestoreException {
    if (featureViewDTO.getQuery() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_CREATION_ERROR, Level.FINE,
          "`Query` is missing from input.");
    }
  }

  public void validateVersion(Integer version) throws FeaturestoreException {
    if(version != null && version <= 0) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_VERSION, Level.FINE,
          " version cannot be negative or zero");
    }
  }


}
