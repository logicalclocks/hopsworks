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

package io.hops.hopsworks.common.featurestore.activity;

import io.hops.hopsworks.persistence.entity.featurestore.activity.ActivityType;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class FeaturestoreActivityController {

  @EJB
  private FeaturestoreActivityFacade activityFacade;

  public void addMetadataActivity(Users user, Featuregroup featuregroup, FeaturestoreActivityMeta activityMeta) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setUser(user);
    fsActivity.setFeatureGroup(featuregroup);
    fsActivity.setType(ActivityType.METADATA);
    fsActivity.setActivityMeta(activityMeta);

    activityFacade.save(fsActivity);
  }

  public void addMetadataActivity(Users user, TrainingDataset trainingDataset, FeaturestoreActivityMeta activityMeta) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setUser(user);
    fsActivity.setTrainingDataset(trainingDataset);
    fsActivity.setType(ActivityType.METADATA);
    fsActivity.setActivityMeta(activityMeta);

    activityFacade.save(fsActivity);
  }
}
