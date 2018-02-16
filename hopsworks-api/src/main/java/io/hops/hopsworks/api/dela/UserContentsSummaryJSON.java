/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
