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

package io.hops.hopsworks.api.opensearch.featurestore;

import com.google.gson.Gson;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.opensearch.OpenSearchFeaturestoreHit;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.exceptions.GenericException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Date;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OpenSearchFeaturestoreItemBuilder {

  @EJB
  private UserFacade userFacade;

  public OpenSearchFeaturestoreItemDTO.Base fromFeaturegroup(OpenSearchFeaturestoreHit hit,
                                                             HopsworksJAXBContext converter)
      throws GenericException {
    OpenSearchFeaturestoreItemDTO.Base item = new OpenSearchFeaturestoreItemDTO.Base();
    item.elasticId = hit.getId();
    item.name = hit.getName();
    item.version = hit.getVersion();
    item.datasetIId = hit.getDatasetIId();
    item.parentProjectId = hit.getProjectId();
    item.parentProjectName = hit.getProjectName();
    for (Map.Entry<String, Object> e : hit.getXattrs().entrySet()) {
      switch (e.getKey()) {
        case FeaturestoreXAttrsConstants.FEATURESTORE: {
          Gson gson = new Gson();
          FeaturegroupXAttr.FullDTO fg
              = converter.unmarshal(gson.toJson(e.getValue()), FeaturegroupXAttr.FullDTO.class);
          item.featurestoreId = fg.getFeaturestoreId();
          item.description = fg.getDescription();
          item.created = new Date(fg.getCreateDate());
          item.creator = new UserDTO(userFacade.findByEmail(fg.getCreator()));
        }
        break;
      }
    }
    return item;
  }

  public OpenSearchFeaturestoreItemDTO.Base fromTrainingDataset(OpenSearchFeaturestoreHit hit,
                                                                HopsworksJAXBContext converter)
      throws GenericException {
    OpenSearchFeaturestoreItemDTO.Base item = new OpenSearchFeaturestoreItemDTO.Base();
    item.elasticId = hit.getId();
    item.name = hit.getName();
    item.version = hit.getVersion();
    item.datasetIId = hit.getDatasetIId();
    item.parentProjectId = hit.getProjectId();
    item.parentProjectName = hit.getProjectName();
    for (Map.Entry<String, Object> e : hit.getXattrs().entrySet()) {
      switch (e.getKey()) {
        case FeaturestoreXAttrsConstants.FEATURESTORE: {
          Gson gson = new Gson();
          TrainingDatasetXAttrDTO td
              = converter.unmarshal(gson.toJson(e.getValue()), TrainingDatasetXAttrDTO.class);
          item.featurestoreId = td.getFeaturestoreId();
          item.description = td.getDescription();
          item.created = new Date(td.getCreateDate());
          item.creator = new UserDTO(userFacade.findByEmail(td.getCreator()));
        }
        break;
      }
    }
    return item;
  }

  public OpenSearchFeaturestoreItemDTO.Feature fromFeature(String featureName,
                                                           String featureDescription,
                                                           OpenSearchFeaturestoreItemDTO.Base parent) {
    OpenSearchFeaturestoreItemDTO.Feature item = new OpenSearchFeaturestoreItemDTO.Feature();
    item.elasticId = parent.getElasticId() + "_" + featureName;
    item.featurestoreId = parent.getFeaturestoreId();
    item.name = featureName;
    item.description = featureDescription;
    item.featuregroup = parent.getName();
    item.datasetIId = parent.getDatasetIId();
    item.version = parent.getVersion();
    item.created = parent.getCreated();
    item.creator = parent.getCreator();

    item.parentProjectId = parent.getParentProjectId();
    item.parentProjectName = parent.getParentProjectName();
    return item;
  }
}
