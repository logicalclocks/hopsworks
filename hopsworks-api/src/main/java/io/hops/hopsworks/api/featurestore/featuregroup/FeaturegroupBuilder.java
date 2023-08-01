/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featuregroup;

import com.google.common.collect.Lists;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturegroupBuilder {

  @EJB
  private FeaturegroupController featuregroupController;

  public FeaturegroupDTO build(Featuregroup featuregroup, Project project, Users user, ResourceRequest resourceRequest)
      throws ServiceException, FeaturestoreException {
    boolean includeFeatures = false;
    boolean includeExpectationSuite = false;
    if (resourceRequest != null) {
      includeFeatures = resourceRequest.contains(ResourceRequest.Name.FEATURES);
      includeExpectationSuite = resourceRequest.contains(ResourceRequest.Name.EXPECTATIONSUITE);
    }
    return featuregroupController.convertFeaturegrouptoDTO(featuregroup, project, user, includeFeatures,
        includeExpectationSuite);
  }

  public List<FeaturegroupDTO> build(List<Featuregroup> featuregroups, Project project, Users user,
      ResourceRequest resourceRequest) throws ServiceException, FeaturestoreException {
    List<FeaturegroupDTO> featuregroupDTOS = Lists.newArrayList();
    for (Featuregroup featuregroup : featuregroups) {
      featuregroupDTOS.add(build(featuregroup, project, user, resourceRequest));
    }
    return featuregroupDTOS;
  }

}
