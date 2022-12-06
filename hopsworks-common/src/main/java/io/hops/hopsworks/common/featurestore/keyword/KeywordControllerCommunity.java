/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.keyword;

import io.hops.hopsworks.common.integrations.CommunityStereotype;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;

@Stateless
@CommunityStereotype
public class KeywordControllerCommunity implements KeywordControllerIface {
  @Override
  public List<String> getAll(Project project, Users user, Featuregroup featureGroup, TrainingDataset trainingDataset,
    FeatureView featureView) throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public List<String> replaceKeywords(Project project, Users user, Featuregroup featureGroup,
    TrainingDataset trainingDataset, FeatureView featureView, List<String> keywords)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public List<String> deleteKeywords(Project project, Users user, Featuregroup featureGroup,
    TrainingDataset trainingDataset, FeatureView featureView, List<String> keywords)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public List<String> getUsedKeywords() throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
}
