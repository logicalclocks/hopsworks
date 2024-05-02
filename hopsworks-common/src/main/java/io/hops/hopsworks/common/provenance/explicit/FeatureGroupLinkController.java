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

import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.provenance.FeatureGroupLink;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the feature_group_link table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupLinkController {
  @EJB
  private FeatureGroupLinkFacade featureGroupLinkFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  
  private static final Logger LOGGER = Logger.getLogger(FeatureGroupLinkController.class.getName());
  
  public Collection<FeatureGroupLink> createParentLinks(Featurestore featurestore,
                                                        StreamFeatureGroupDTO featureGroupDTO,
                                                        Featuregroup featureGroup) throws GenericException {
    if(featureGroupDTO.getParents() != null) {
      return createParentLinksInt(featurestore, featureGroup, featureGroupDTO.getParents());
    } else {
      return new ArrayList<>();
    }
  }

  public Collection<FeatureGroupLink> createParentLinks(Featurestore featurestore,
                                                        CachedFeaturegroupDTO featureGroupDTO,
                                                        Featuregroup featureGroup) throws GenericException {
    if(featureGroupDTO.getParents() != null) {
      return createParentLinksInt(featurestore, featureGroup, featureGroupDTO.getParents());
    } else {
      return new ArrayList<>();
    }
  }
  
  private Collection<FeatureGroupLink> createParentLinksInt(Featurestore featurestore,
                                                            Featuregroup featureGroup,
                                                            List<FeaturegroupDTO> parentFGs)
    throws GenericException {
    List<FeatureGroupLink> links = new ArrayList<>();
    Map<Integer, Featurestore> featurestores = new HashMap<>();
    featurestores.put(featurestore.getId(), featurestore);
    for (FeaturegroupDTO parentFG : parentFGs) {
      FeatureGroupLink link = new FeatureGroupLink();
      link.setFeatureGroup(featureGroup);
      link.setParentFeatureGroup(featuregroupFacade.find(parentFG.getId()));
      //we denormalize here so that if the parent gets deleted, there is still user readable information left
      Featurestore parentFeatureStore = featurestores.get(parentFG.getFeaturestoreId());
      if(parentFeatureStore == null) {
        parentFeatureStore = featurestoreFacade.find(parentFG.getFeaturestoreId());
        if(parentFeatureStore == null) {
          throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.WARNING,
            "cannot create explicit link to a deleted feature group");
        }
        featurestores.put(parentFeatureStore.getId(), parentFeatureStore);
      }
      link.setParentFeatureStore(parentFeatureStore.getProject().getName());
      link.setParentFeatureGroupName(parentFG.getName());
      link.setParentFeatureGroupVersion(parentFG.getVersion());
      featureGroupLinkFacade.persist(link);
      links.add(link);
    }
    return links;
  }
}
