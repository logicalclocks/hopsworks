/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserContentsSummaryJSON {

  private Integer projectId;
  private ElementSummaryJSON[] elementSummaries = new ElementSummaryJSON[0];

  public UserContentsSummaryJSON() {
  }

  public UserContentsSummaryJSON(Integer projectId,
    ElementSummaryJSON[] elementSummaries) {
    this.projectId = projectId;
    if (elementSummaries != null) {
      this.elementSummaries = elementSummaries;
    }
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public ElementSummaryJSON[] getElementSummaries() {
    return elementSummaries;
  }

  public void setElementSummaries(ElementSummaryJSON[] elementSummaries) {
    if (elementSummaries != null) {
      this.elementSummaries = elementSummaries;
    } else {
      this.elementSummaries = new ElementSummaryJSON[0];
    }
  }

  @Override
  public String toString() {
    return "UserContentsSummaryJSON{" + "projectId=" + projectId + ", elementSummaries=" + elementSummaries + '}';
  }

}
