/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.admin.alert.management;

import io.hops.hopsworks.api.alert.Entry;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PostableInhibitRulesDTO {

  private List<Entry> sourceMatch;
  private List<Entry> sourceMatchRe;
  private List<Entry> targetMatch;
  private List<Entry> targetMatchRe;
  private List<String> equal;

  public PostableInhibitRulesDTO() {
  }

  public List<Entry> getSourceMatch() {
    return sourceMatch;
  }

  public void setSourceMatch(List<Entry> sourceMatch) {
    this.sourceMatch = sourceMatch;
  }

  public List<Entry> getSourceMatchRe() {
    return sourceMatchRe;
  }

  public void setSourceMatchRe(List<Entry> sourceMatchRe) {
    this.sourceMatchRe = sourceMatchRe;
  }

  public List<Entry> getTargetMatch() {
    return targetMatch;
  }

  public void setTargetMatch(List<Entry> targetMatch) {
    this.targetMatch = targetMatch;
  }

  public List<Entry> getTargetMatchRe() {
    return targetMatchRe;
  }

  public void setTargetMatchRe(List<Entry> targetMatchRe) {
    this.targetMatchRe = targetMatchRe;
  }

  public List<String> getEqual() {
    return equal;
  }

  public void setEqual(List<String> equal) {
    this.equal = equal;
  }
}
