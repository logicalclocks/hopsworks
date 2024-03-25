/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.explicit;

import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.provenance.ModelLink;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Class controlling the interaction with the model_link table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelLinkController {
  @EJB
  private ModelLinkFacade modelLinkFacade;

  public ModelLink createParentLink(ModelVersion model, TrainingDataset trainingDataset) {
    ModelLink link = new ModelLink();
    link.setModel(model);
    link.setParentTrainingDataset(trainingDataset);
    //we denormalize here so that if the parent gets deleted, there is still user readable information left
    link.setParentFeatureStore(trainingDataset.getFeaturestore().getProject().getName());
    link.setParentFeatureViewName(trainingDataset.getFeatureView().getName());
    link.setParentFeatureViewVersion(trainingDataset.getFeatureView().getVersion());
    link.setParentTrainingDatasetVersion(trainingDataset.getVersion());
    modelLinkFacade.persist(link);
    return link;
  }
}