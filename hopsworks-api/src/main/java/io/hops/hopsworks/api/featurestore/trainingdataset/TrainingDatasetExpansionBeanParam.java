/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.trainingdataset;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.Set;
import java.util.stream.Collectors;

public class TrainingDatasetExpansionBeanParam {

  @QueryParam("expand")
  @ApiParam(value = "ex. expand=keywords,expand=tags",
      allowableValues = "expand=tags,expand=keywords")
  private Set<TrainingDatasetExpansions> expansions;

  public TrainingDatasetExpansionBeanParam(@QueryParam("expand") Set<TrainingDatasetExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<TrainingDatasetExpansions> getExpansions() {
    return expansions;
  }

  public void setExpansions(Set<TrainingDatasetExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ResourceRequest> getResources() {
    return expansions.stream().map(TrainingDatasetExpansions::getResourceRequest).collect(Collectors.toSet());
  }
}
