/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore;

import io.hops.hopsworks.common.featurestore.importjob.FeaturegroupImportJobDTO;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.exceptions.FeaturestoreException;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;


public interface ImportControllerIface {
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  default public Jobs createImportJob(Users user, Project project, FeaturegroupImportJobDTO featuregroupImportJobDTO)
      throws FeaturestoreException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
}
