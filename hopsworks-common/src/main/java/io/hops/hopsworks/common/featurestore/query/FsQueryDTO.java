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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class FsQueryDTO extends RestDTO<FsQueryDTO> {
  private String query;
  private String queryOnline;

  private List<HudiFeatureGroupAliasDTO> hudiCachedFeatureGroups;
  private List<OnDemandFeatureGroupAliasDTO> onDemandFeatureGroups;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQueryOnline() {
    return queryOnline;
  }

  public void setQueryOnline(String queryOnline) {
    this.queryOnline = queryOnline;
  }

  public List<HudiFeatureGroupAliasDTO> getHudiCachedFeatureGroups() {
    return hudiCachedFeatureGroups;
  }

  public void setHudiCachedFeatureGroups(List<HudiFeatureGroupAliasDTO> hudiCachedFeatureGroups) {
    this.hudiCachedFeatureGroups = hudiCachedFeatureGroups;
  }

  public List<OnDemandFeatureGroupAliasDTO> getOnDemandFeatureGroups() {
    return onDemandFeatureGroups;
  }

  public void setOnDemandFeatureGroups(List<OnDemandFeatureGroupAliasDTO> onDemandFeatureGroups) {
    this.onDemandFeatureGroups = onDemandFeatureGroups;
  }
}
