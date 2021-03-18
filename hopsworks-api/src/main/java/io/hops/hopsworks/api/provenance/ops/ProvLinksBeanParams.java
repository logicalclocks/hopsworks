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
package io.hops.hopsworks.api.provenance.ops;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.Set;

public class ProvLinksBeanParams {
  @QueryParam("filter_by")
  @ApiParam(value = "ex filter_by=APP_ID:app1, filter_by=IN_ARTIFACT:mnist_1, filter_by=OUT_ARTIFACT:mnist_1",
    allowMultiple = true)
  private Set<String> filterBy;
  @DefaultValue("false")
  @QueryParam("only_apps")
  private boolean onlyApps;
  @DefaultValue("true")
  @QueryParam("full_link")
  private boolean fullLink;
  
  public ProvLinksBeanParams(@QueryParam("filter_by") Set<String> linksFilterBy,
    @QueryParam("only_apps") boolean onlyApps,
    @QueryParam("full_link") boolean fullLink){
    this.filterBy = linksFilterBy;
    this.onlyApps = onlyApps;
    this.fullLink = fullLink;
  }
  
  public Set<String> getFilterBy() {
    return filterBy;
  }
  
  public void setFilterBy(Set<String> filterBy) {
    this.filterBy = filterBy;
  }
  
  public boolean isOnlyApps() {
    return onlyApps;
  }
  
  public void setOnlyApps(boolean onlyApps) {
    this.onlyApps = onlyApps;
  }
  
  public boolean isFullLink() {
    return fullLink;
  }
  
  public void setFullLink(boolean fullLink) {
    this.fullLink = fullLink;
  }
}
