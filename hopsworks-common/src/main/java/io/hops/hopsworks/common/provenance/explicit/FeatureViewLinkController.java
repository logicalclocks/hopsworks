/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.provenance.FeatureViewLink;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the feature_view_link table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewLinkController {
  @EJB
  private FeatureViewLinkFacade featureViewLinkFacade;
  
  private static final Logger LOGGER = Logger.getLogger(FeatureViewLinkController.class.getName());
  
  public Collection<FeatureViewLink> createParentLinks(FeatureView featureView) {
    Map<Integer, FeatureViewLink> links = new HashMap<>();
    for (TrainingDatasetFeature parentFG : featureView.getFeatures()) {
      if(!links.containsKey(parentFG.getFeatureGroup().getId())) {
        FeatureViewLink link = new FeatureViewLink();
        link.setFeatureView(featureView);
        link.setParentFeatureGroup(parentFG.getFeatureGroup());
        //we denormalize here so that if the parent gets deleted, there is still user readable information left
        link.setParentFeatureStore(parentFG.getFeatureGroup().getFeaturestore().getProject().getName());
        link.setParentFeatureGroupName(parentFG.getFeatureGroup().getName());
        link.setParentFeatureGroupVersion(parentFG.getFeatureGroup().getVersion());
        featureViewLinkFacade.persist(link);
        links.put(parentFG.getFeatureGroup().getId(), link);
      }
    }
    return links.values();
  }
}
