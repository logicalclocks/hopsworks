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

import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.logging.Level;

public interface ProvExplicitControllerIface {
  default ProvExplicitLink<Featuregroup> featureGroupLinks(Project accessProject, Featuregroup root)
    throws GenericException, FeaturestoreException, DatasetException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.FINE);
  }
  
  default ProvExplicitLink<Featuregroup> featureGroupLinks(Project accessProject,
                                                           Featuregroup root,
                                                           Integer upstreamLevels,
                                                           Integer downstreamLevels)
    throws GenericException, FeaturestoreException, DatasetException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.FINE);
  }
  
  default ProvExplicitLink<FeatureView> featureViewLinks(Project accessProject, FeatureView root)
    throws GenericException, FeaturestoreException, DatasetException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.FINE);
  }
  
  default ProvExplicitLink<FeatureView> featureViewLinks(Project accessProject,
                                                         FeatureView root,
                                                         Integer upstreamLevels,
                                                         Integer downstreamLevels)
    throws GenericException, FeaturestoreException, DatasetException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.FINE);
  }
  
  default ProvExplicitLink<TrainingDataset> trainingDatasetLinks(Project accessProject, TrainingDataset root)
    throws GenericException, FeaturestoreException, DatasetException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.FINE);
  }
  
  default ProvExplicitLink<TrainingDataset> trainingDatasetLinks(Project accessProject,
                                                                 TrainingDataset root,
                                                                 Integer upstreamLevels,
                                                                 Integer downstreamLevels)
    throws GenericException, FeaturestoreException, DatasetException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.FINE);
  }
}
